# Releasing teams4j

Releases run on a maintainer's machine. No signing key or Portal token is stored in GitHub, so a
leaked CI secret cannot publish anything. CI builds and tests; it never publishes.

## One-time setup

1. A GPG key whose public key is on a keyserver (`gpg --keyserver keys.openpgp.org --send-keys <id>`).
2. `~/.gradle/gradle.properties`:
   ```properties
   signing.gnupg.keyName=<key id>
   ```
   Signing goes through the local gpg agent; the private key never leaves it.
3. Central Portal user token, as environment variables or in `~/.jreleaser/config.properties`:
   ```properties
   JRELEASER_MAVENCENTRAL_USERNAME=<token username>
   JRELEASER_MAVENCENTRAL_TOKEN=<token>
   ```
4. A GitHub personal access token (classic) with `write:packages`, for the GitHub Packages mirror,
   as `GITHUB_TOKEN` in the environment or `gpr.key` in `~/.gradle/gradle.properties`. Like the
   others it stays on the machine.

## Version

The version is not in any file. It comes from the nearest `vX.Y.Z` tag (root `build.gradle.kts`,
`com.palantir.git-version`): on the tag with a clean tree it is `X.Y.Z`; one commit later, or with
uncommitted changes, it is `X.Y.(Z+1)-SNAPSHOT`. `./gradlew printVersion` shows it. So a release is a
tag, and there is no release commit and no "back to SNAPSHOT" commit. Untracked files count as
uncommitted changes (`git status --porcelain` must print nothing), so a stray note in the tree turns
the release into a SNAPSHOT: move it out before tagging.

## Rehearsal

Central rejects `-SNAPSHOT` versions, so a rehearsal is the release with the tag kept local.

```bash
git tag -a vX.Y.Z -m "X.Y.Z"
./gradlew clean build
./gradlew publishAllPublicationsToStagingRepository
./gradlew jreleaserDeploy            # stage UPLOAD: arrives in the Portal as VALIDATED
```

In the Portal, inspect the deployment and press **Drop**. Then `git tag -d vX.Y.Z`; whatever the
rehearsal turned up becomes a commit and the tag is placed again afterwards. On a failure, JReleaser
writes the useful part to `build/jreleaser/trace.log`.

## Release

```bash
# 0. the public ABI must not have broken against the previous release -- or, when it has, the report
#    (build/reports/japicmp/*.txt) is the breaking-changes section of the release notes
./gradlew check -PapiBaseline=<previous version>

# 1. version: a tag on a clean tree. Uncommitted changes make it a SNAPSHOT, which Central rejects.
git tag -a vX.Y.Z -m "X.Y.Z"
./gradlew printVersion               # X.Y.Z

# 2. build, sign, stage, upload
./gradlew clean build
./gradlew publishAllPublicationsToStagingRepository
./gradlew jreleaserDeploy

# 3. Portal: inspect, then Publish (cannot be undone)

# 4. the same staged, signed artifacts to the GitHub Packages mirror. After Central, so a Portal
#    rejection never leaves a version on GitHub that Central does not have. A version cannot be
#    uploaded twice: on a mistake, delete it in the repository's Packages page and run again.
./gradlew publishAllPublicationsToGitHubPackagesRepository

# 5. the tag is the release; push it
git push origin main vX.Y.Z

# 6. release notes: the commits since the previous tag, one section per conventional-commit type
#    (`feat:`, `fix:`, ...; `feat!:` marks a breaking change). Read the file: drop the sections a
#    consumer does not care about (Tasks, Build, Tests, CI), and add the ABI breaks from step 0's
#    japicmp report (build/reports/japicmp/*.txt) under a heading of their own -- a commit type
#    cannot tell a binary break from an addition.
./gradlew jreleaserChangelog         # build/jreleaser/release/CHANGELOG.md
gh release create vX.Y.Z --title X.Y.Z --notes-file build/jreleaser/release/CHANGELOG.md
```

Pushing the tag also redeploys the site's API pages from the latest tag (the `github-pages`
environment allows tags `v*`).

[Reproducible Central](https://github.com/jvm-repo-rebuild/reproducible-central) picks up new
versions on its own. If the JDK or the rebuild command changes, copy the previous buildspec to the
new version and open a PR with the `.buildspec` alone; their CI rebuilds it.

Then set `teams4jVersion` in `examples/gradle.properties` and `-PapiBaseline` in `ci.yml` to the
new version.

`jreleaserDeploy -PreleaseStage=FULL` publishes without the Portal step. Do not use it.

Gradle also writes checksums of the `.asc` signatures; `jreleaserDeploy` drops them, since Central
counts them against its [publishing limits](https://central.sonatype.org/publish/reducing-publishing-usage/).
