# Vendored schemas

The Adaptive Cards schema is committed and pinned by sha256, so neither the build nor CI touches
the network.

| File | Source | Version | Size | sha256 |
|------|--------|---------|------|--------|
| `adaptive-card-1.6.0.json` | https://adaptivecards.io/schemas/1.6.0/adaptive-card.json | 1.6.0 | 103990 B | `d0df0a2e5020c50becfea0b18b6fa1dbce1b4d99108b3752899b5ddb9ae6e20a` |

Do not use the unversioned `https://adaptivecards.io/schemas/adaptive-card.json`. When checked on
2026-08-26 it held 68 definitions, which is *older* than 1.6.0's 73. Always pin a version URL.

## Updating

```bash
# download the new schema and print its sha256, then update the table above
./gradlew generateModel          # regenerate
git diff -- teams4j-cards/src/generated   # review the generated diff in a PR
```

Fetching is a manual step on purpose; it is never wired into CI or into `build`.

## Notes on schema shape (why the reader looks the way it does)

- **Unions**: `ImplementationsOf.X` is an `anyOf` of `{required:[type], allOf:[{$ref: Concrete}]}`
  branches, and becomes a sealed interface with a `permits` clause.
- **Base types**: `Extendable.X` holds shared properties and chains to its own parent through
  `allOf`. These are never emitted as types; their properties are flattened into concrete types.
- **Discriminator**: a concrete type's `properties.type.enum[0]`, for example `"TextBlock"`.
- **Enums**: `anyOf: [{enum:[...]}, {pattern: <case-insensitive regex>}]`. Only the `enum` branch
  is used; the `pattern` branch is discarded and case-insensitivity is handled by the mapper.
- **Nullable**: `anyOf: [{$ref: X}, {type: "null"}]` folds to X, since every record component is
  already nullable.

### Warts the reader has to absorb

| Wart | Example | Handling |
|------|---------|----------|
| Property name embeds a `?` | `Container."rtl?"` | Name normalised to `rtl` |
| Inherited properties re-listed as empty `{}` | `TextBlock.fallback: {}` | Ignored, so the ancestor's real definition survives |
| Genuinely alternative shapes | `Container.backgroundImage` is object or string | Emitted as `JsonNode`, which round-trips losslessly |
| Definitions that are not a single type | `TargetElement` is string or object | Not generated; references to it degrade to `JsonNode` with a warning |
| Documentation-only keys | `features`, `version` | `version` feeds Javadoc; `features` is discarded |
