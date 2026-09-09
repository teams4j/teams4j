# Vendored Activity Protocol specs

The TypeSpec sources of the Activity Protocol, the Bot Connector (Channel) API and the Token Service,
as published in the Microsoft 365 Agents SDK repository. They are the language-neutral definition of
the wire format `teams4j-bot` implements, and the only one: the Agents SDK ships C#, JavaScript and
Python, not Java. Committed so tests can compare the `Activity` model and the `ConnectorClient`
surface against the spec without touching the network.

- Source: https://github.com/microsoft/Agents `specs/typespec/`
- Commit: `5d1f7669b3c2acfd2889981043a4a1e1300ccc63`
- Retrieved: 2026-09-09
- Licence: MIT (see the upstream repository; each file also carries its licence in the header)

| File | Spec | Version |
|---|---|---|
| `activity.tsp` | ActivityProtocol: `Activity`, `ActivityTypes`, cards, invoke models | 3.2.0 |
| `channelapi.tsp` | ChannelAPI: the `/v3/conversations` and `/v3/attachments` operations | 3.1.12 |
| `tokenservice.tsp` | TokenService: `/api/usertoken` and `/api/botsignin` | 3.1.12 |

Files are byte-identical to upstream. Not vendored: the Teams-specific extensions (roster, meetings,
batch messaging, invoke names and their response models) have no TypeSpec; the per-language
`Extensions.Teams` packages are the closest thing to a definition.

## Refreshing

The `spec-drift` workflow compares these files with upstream `main` weekly and opens an issue when
they differ. To refresh: re-download the three paths at the new commit, update the commit and date
above, and run `./gradlew :teams4j-bot:test`. `ActivityProtocolSpecTest` lists every spec field and
operation teams4j does not model, so a new one fails there until it is either modelled or added to
the list.
