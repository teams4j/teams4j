# Contributing to teams4j

Thanks for taking a look. This file covers what you need to build the project, what the
build checks and why, and the few places where the obvious edit is the wrong one.

## Prerequisites

A **Java 21** toolchain. Published bytecode targets **Java 17**, which is the floor teams4j
claims; the toolchain is newer so the tests can use newer language features. Gradle will not
download a JDK for you, so either have 21 on `PATH` or use [mise](https://mise.jdx.dev) —
`mise.toml` in the repository root selects it.

Nothing else. The Gradle wrapper brings its own Gradle.

## Build and test

```bash
./gradlew build          # compile, test, Spotless, NullAway — the whole gate
./gradlew test           # tests only (575 of them at the time of writing)
./gradlew spotlessApply  # fix formatting rather than being told about it
```

`build` is what CI runs, so if it passes locally the build job will too.

### The starter, against both Spring Boot lines

One starter artifact supports Boot 3.5.x and 4.1.x. CI runs the starter's tests against
both; locally you pick one:

```bash
./gradlew :teams4j-webhook-spring-boot-starter:test -PbootTestVersion=4.1.1
```

If a change makes that matrix fail, the answer might be to split the artifact per Boot line
rather than to patch the test.

### The examples

`examples/` is **a separate Gradle build** that resolves teams4j from a repository instead of
from the source tree. That is deliberate: it is the only place that exercises the published
POMs, the dependency scopes, the BOM's module list, and the `ServiceLoader` registration that
picks a JSON binding — none of which a project dependency would prove. So it needs the
artifacts installed first:

```bash
./gradlew publishToMavenLocal
cd examples && ./gradlew build
```

`examples/README.md` covers running them, including how to point the Spring example at a
loopback stub when you have no Teams channel to hand.

## Do not hand-edit generated sources

The Adaptive Cards model is **generated from the official schema**, not written by hand:

| Tree | Contents |
|---|---|
| `teams4j-cards/src/generated/java` | the Java model — `AdaptiveCard`, `TextBlock`, `Table`, … |
| `teams4j-cards-kotlin/src/generated/kotlin` | the Kotlin type-safe DSL |
| `teams4j-cards-kotlinx/src/generated/kotlin` | the kotlinx.serialization binding |

All three are committed, and CI regenerates them and fails on any diff. An edit made directly
in one of those trees will be reverted by the next generator run, and CI will catch it first.

The source of truth is `codegen/schemas/adaptive-card-1.6.0.json` plus
`codegen/overrides.json`. To change the model, change the generator or the overrides and
regenerate:

```bash
./gradlew :codegen:generateModel
```

Before reaching for an override, run the diagnostic — it says what the schema actually yields,
which properties could not be narrowed, and which overrides no longer match anything:

```bash
./gradlew :codegen:report
```

Filling in `overrides.json` from that output beats guessing, which is what it is for.

### Schema coverage is measured, not asserted

`teams4j-cards-jackson/src/test/resources/samples/` holds 184 official sample cards, and
`SampleRoundTripTest` parses and re-serialises every one of them with a strict mapper.
`samples/UNSUPPORTED.txt` lists the ones the model cannot round-trip, with a reason each.

That file is an **expected-failure list, not a skip list**: a sample on it that starts passing
fails the build and the entry has to be deleted. Its length is the coverage metric. If your
change makes an entry pass, deleting it is part of the change.

### Golden files, and the docs that quote them

The DSL has golden-file snapshots: `DocumentedCardsTest` in `teams4j-cards-jackson` pins the JSON each
Java DSL shape produces, and its namesake in `teams4j-cards-kotlin` builds the same cases with the
Kotlin DSL and compares them against the same files, read-only. After an intended change to what the
DSL emits:

```bash
./gradlew :teams4j-cards-jackson:test -PgoldenUpdate=true
```

Read the resulting diff before committing it. Without the flag a difference is a test failure,
which is the point; if the Kotlin test then disagrees, the Kotlin DSL has drifted, and that is the
finding.

Those two tests are also the source of every DSL snippet on the docs site: each case is fenced with
`// #region <name>` markers and included by name, next to its golden file, so the site quotes code CI
compiles rather than a copy. Renaming a case, a region or a golden file breaks the docs build, which
the `docs` CI job runs on every pull request.

## The docs site

`docs/` is a [VitePress](https://vitepress.dev) site, published to
<https://teams4j.github.io/teams4j/> from `main`. Node is pinned in `mise.toml` alongside the JDK.

```bash
cd docs
npm ci
npm run dev      # live preview
npm run build    # what CI runs; fails on a dead link or a missing snippet
```

Code samples on the site are `<<<` includes of files CI compiles (the documented-cards tests, the
examples), never pasted code. If a page needs a snippet that does not exist yet, add it to one of
those sources first.

## Code style and static analysis

Formatting is applied, not reviewed: `./gradlew spotlessApply` runs **palantir-java-format**
(4-space, 120 columns) on Java and **ktlint** on Kotlin, plus the import order
`java`, `javax`, everything else, `io.github.teams4j`. Generated sources are not targets —
the emitter owns their layout. `.editorconfig` carries the same 120-column limit for editors.

`removeUnusedImports()` is deliberately not enabled; see the comment in
`buildSrc/src/main/kotlin/teams4j.java-conventions.gradle.kts` for the reason.

What the build enforces beyond formatting:

- **Warnings are errors**, in both languages (`-Werror`, `allWarningsAsErrors`). The count is
  zero and staying there is the point; if a JDK or Kotlin upgrade introduces new warnings, they
  are dealt with on the upgrade commit.
- **Error Prone runs with its default checks**, plus NullAway as an error. Every module is
  JSpecify `@NullMarked`, and `@Nullable` is a type-use annotation. Generated sources and
  `codegen` are excluded, because a finding there is a codegen bug. One check is disabled
  project-wide (`CanonicalDuration`); the reason sits next to the `disable(...)` call.
- **detekt covers Kotlin** with deltas in `config/detekt.yml`, each carrying its reason.
- **The public ABI is guarded twice.** Kotlin modules dump theirs to committed `api/*.api`
  files — after an intended API change, run `./gradlew apiDump` and read the diff before
  committing it. Java modules are compared against the previous release with japicmp:
  `./gradlew check -PapiBaseline=<last release>`, dormant until a first release exists.
- **Comments are in English.** This is a public project; the working language of the code is
  English regardless of the language a discussion happens in.

Suppressing a finding is fine when the tool is wrong about the specific case, but the
suppression carries the argument: every `@Suppress` in the tree says why the rule does not
apply there.

### On comments

The existing code explains *why*, not *what*, and cites evidence where a claim could be
doubted — a measurement date, a documentation link, a sample that proves it. Constants that
came from a live tenant say so. Please match that; a comment restating the line below it is
worse than none.

## Tests

Prefer a test that would have caught the bug over one that describes the fix. The suite leans
on a few patterns worth knowing before adding to it:

- The webhook tests run against a **WireMock loopback stub**, which speaks plain http. The
  client requires https, so `WorkflowsWebhookClient.Builder.allowPlainHttp()` lifts that for
  the stub. The https check itself is covered by its own test, so lifting it in the fixtures
  does not lose coverage.
- Time is injected — `Clock`, `Sleeper`, `Delayer`, a nanosecond source. Retry and rate-limit
  tests assert on recorded waits rather than sleeping.
- Anything claiming Teams platform behaviour needs a source. Several validator rules were
  deleted once a live tenant showed them to be false, and [the measurements page](https://teams4j.github.io/teams4j/reference/measurements)
  records those runs.

## Pull requests

The CI workflow runs four jobs, and all four have to be green:

1. **build** — `./gradlew build`, then a regeneration check across the three generated trees
2. **starter-boot-matrix** — the starter's tests on Boot 3.5.16 and 4.1.1
3. **examples** — `publishToMavenLocal`, then the separate `examples/` build, on both Boot lines
4. **docs** — the docs site build, which fails on a dead link or a missing snippet

Alongside it, a few security checks run on every pull request: Trivy and Semgrep (`security.yml`),
zizmor on the workflow files themselves, and a dependency review that compares the Gradle
dependency graph with `main` and rejects a newly introduced vulnerable package. Actions are pinned
to commit SHAs; `pinact run` updates them.

Beyond that:

- One concern per pull request. A formatting sweep mixed into a behaviour change hides the
  behaviour change.
- If you change something a reader of the README or the cookbook would then find wrong, update
  it in the same pull request. Documentation that does not run is the worst failure mode here,
  which is why the examples are wired into CI at all.
- New public API needs Javadoc that says what the caller has to know, including the failure
  mode.

## Reporting bugs

A card that does not render, or renders differently than teams4j predicted, is the most useful
report this project can get — include the JSON teams4j produced (`CardWriter.write(card)`) and
what Teams did with it. Redact the webhook URL: **it carries its own signature, so the URL is
write access to the channel.**

For anything that looks like a security issue, use GitHub's **Report a vulnerability** button
under the repository's Security tab rather than opening a public issue.

## Licence

teams4j is Apache-2.0. By contributing you agree that your contribution is licensed under it.
There is no CLA.
