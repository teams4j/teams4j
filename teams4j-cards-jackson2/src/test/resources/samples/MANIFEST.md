# Vendored sample cards

Official Adaptive Cards samples, used by the round-trip tests. They are committed so
the test suite never touches the network.

- Source: https://github.com/microsoft/AdaptiveCards `samples/v1.*/{Elements,Scenarios}`
- Commit: `7fff4461f0fd45f8809fd051fcd97cc387a80bb2`
- Retrieved: 2026-08-26
- Count: 184 files
- Licence: MIT (see the upstream repository)

File names are flattened as `<version>.<original name>`, so `samples/v1.0/Elements/TextBlock.json`
becomes `v1.0.TextBlock.json`. The `Tests/` directories are excluded: they hold deliberately
malformed cards for renderer error handling, not valid schema examples.

## Refreshing

Re-download the same paths, update the commit above, and run the round-trip tests. Any sample that
starts passing must be removed from `UNSUPPORTED.txt`; the test enforces this.

## Samples that cannot round-trip

`UNSUPPORTED.txt` lists them with a reason. Two groups will never shrink:

**Samples ahead of the published schema.** `Carousel`, `Table.firstRowAsHeaders`,
`AdaptiveCard.style`, `Column.horizontalAlignment`, `Action.OpenUrl.role`,
`Action.Submit.disabledUnlessAssociatedInputsChange`, `Image.forceLoad`, `ImageSet.style` and
`TableRow.spacing` appear in samples but in **no published 1.6.0 schema**. Verified 2026-08-26
against both sources, which are equivalent at 73 definitions each:

- `https://adaptivecards.io/schemas/1.6.0/adaptive-card.json`
- `microsoft/AdaptiveCards` `schemas/1.6.0/adaptive-card.json`

They exist only in the upstream `schemas/src` tree, the modular source for a future release. Since
Teams supports Adaptive Cards up to 1.6, these are outside the profile teams4j
targets; rejecting them under the strict mapper is correct rather than a gap. They will resolve on
their own when a 1.7 schema is published and vendored.

**Deliberately invalid samples.** Some samples carry an unknown element type (`NoExist`, `Graph`)
or an invalid enum value (an action style of `"other"`) to exercise renderer fallback. The strict
mapper is right to reject them; `CardJson.mapper()` reads them, mapping the unknown enum to null.
