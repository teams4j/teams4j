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
`com.palantir.git-version`): on the tag with a clean tree it is `0.1.0`; one commit later, or with
uncommitted changes, it is `0.1.1-SNAPSHOT`. `./gradlew printVersion` shows it. So a release is a
tag, and there is no release commit and no "back to SNAPSHOT" commit. Untracked files count as
uncommitted changes (`git status --porcelain` must print nothing), so a stray note in the tree turns
the release into a SNAPSHOT: move it out before tagging.

## Rehearsal

Central rejects `-SNAPSHOT` versions, so a rehearsal is the release with the tag kept local.

```bash
git tag -a v0.1.0 -m "0.1.0"
./gradlew clean build
./gradlew publishAllPublicationsToStagingRepository
./gradlew jreleaserDeploy            # stage UPLOAD: arrives in the Portal as VALIDATED
```

In the Portal, inspect the deployment and press **Drop**. Then `git tag -d v0.1.0`; whatever the
rehearsal turned up becomes a commit and the tag is placed again afterwards. On a failure, JReleaser
writes the useful part to `build/jreleaser/trace.log`.

## Release

```bash
# 0. the public ABI must not have broken against the previous release (0.1.0 is the first baseline)
./gradlew check -PapiBaseline=<previous version>

# 1. version: a tag on a clean tree. Uncommitted changes make it 0.1.1-SNAPSHOT, which Central rejects.
git tag -a v0.1.0 -m "0.1.0"
./gradlew printVersion               # 0.1.0

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
git push origin main v0.1.0
```

Pushing the tag also runs the Docs workflow, which rebuilds the API pages of the site from the
latest tag (`docs/scripts/api-docs.sh`), so the published Javadoc and Dokka follow the release
rather than main.

Then point `examples/gradle.properties` (`teams4jVersion`) and the docs at the released coordinates,
and check the previous release against the new one is no longer the baseline: `-PapiBaseline` in
CONTRIBUTING and the CI job, if one guards it, move to the version just released.

For a version that is already on Central but not on GitHub Packages (0.1.0 was published this way on
2026-09-10): check out its tag with a clean tree, so the version derives correctly, and run step 4.
The repository block is part of the build only from 0.2.0; on an older tag, supply it with an init
script (`-I`) that declares the same `GitHubPackages` repository rather than editing the tree, which
would make the version a SNAPSHOT.

`jreleaserDeploy -PreleaseStage=FULL` publishes without the Portal step. Do not use it.

## Why the key decisions are what they are

- **Version from the tag, not a file.** The alternative is editing `gradle.properties` twice per release
  and committing both edits, with the tag and the file free to disagree. The plugin only reads git; it
  never tags or pushes, so the human steps stay human. It needs the tags to be present: CI checks out
  with `fetch-depth: 0`.
- **Local only.** A signing key or Portal token in GitHub is a standing credential that a compromised
  action or workflow could use. Two commands on one machine are a smaller surface than a workflow with
  secrets, and a release is rare enough that automation buys little.
- **gpg agent, not an exported key.** `useGpgCmd()` means the armored private key is never written
  to an environment variable or a file. `SIGNING_KEY` remains as a fallback for a machine without gpg.
- **Upload stage, human publish.** Publishing cannot be undone, so the last step stays a click in the
  Portal after looking at what arrived.
- **ed25519 key, personal-email UID.** Chosen after checking eight comparable libraries. Whether Central
  accepts it is what the first rehearsal checks.
- **GitHub Packages is a mirror, not a second source of truth.** Downloading from it needs a token
  even for a public repository, so the docs name Central and nothing else. It gets the same signed
  files, after Central has accepted them, through plain `maven-publish` rather than a second JReleaser
  deployer: one more repository block, no more moving parts.
