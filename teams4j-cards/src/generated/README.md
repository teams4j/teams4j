# Generated sources (committed)

`src/generated/java` holds the model that `:codegen` derives from the Adaptive Cards JSON Schema.
**Do not edit these files by hand.**

Why they are committed:

- a schema change shows up as a reviewable diff in a pull request, which is decisive when moving to
  Adaptive Cards 1.7
- consumer builds and IDEs work without running the generator
- CI only has to check for drift: regenerate, then `git diff --exit-code`

To change the output, edit the generator in `:codegen` or `codegen/overrides.json` and regenerate
with `./gradlew generateModel`.
