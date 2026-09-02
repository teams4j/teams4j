# Generated sources (committed)

`src/generated/kotlin` holds the Kotlin DSL that `:codegen` derives from the Adaptive Cards JSON
Schema — from the same parse as the Java model in `teams4j-cards`, not from a second one.
**Do not edit these files by hand.**

Committed for the reasons in [`../../teams4j-cards/src/generated/README.md`](../../teams4j-cards/src/generated/README.md),
and checked for drift by the same CI step.

Only the entry point is hand-written: `src/main/kotlin/.../CardDsl.kt` holds the `@DslMarker`
annotation and `adaptiveCard { }`. Everything else — one `…Dsl` class per type, one `…Scope`
collector per list-valued property — comes out of `KotlinEmitter`.

Regenerate with `./gradlew generateModel`, which writes both trees.
