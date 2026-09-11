# Contributing

`cloud-itonami-isco-8341` accepts contributions to the OSS actor, policy tests,
documentation, examples and open occupation blueprint.

## Development

```bash
kbb -M:dev:test
kbb -M:lint
```

Keep changes small and include tests for policy, audit, store or disclosure
behavior.

## Rules

- Do not commit real operator/equipment data, credentials or operating documents.
- Keep production writes and disclosures behind Farm/Forestry Ops Governor.
- Never add an op to the allowlist that finalizes an equipment-operation/movement decision or overrides an operator's on-site safety judgment.
- Treat this occupation's workflows as high-risk: add tests for permission,
  purpose, safety and audit logging.
- Document any new business-model or operator assumption in `docs/`.

## Pull Requests

PRs should describe:

- what behavior changed
- which policy invariant is affected
- how it was tested
- whether operator or certification docs need updates
