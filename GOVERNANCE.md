# Governance

`cloud-itonami-isco-8341` is an OSS open-occupation blueprint. Governance covers
both code and the operator model.

## Maintainers

Maintainers may merge changes that preserve these invariants:

- the Advisor cannot directly dispatch robot actions, operate equipment, or disclose records.
- Farm/Forestry Ops Governor remains independent of the advisor.
- hard policy violations (including any attempt to finalize an equipment-operation/movement decision or override an operator's on-site safety judgment) cannot be overridden by human approval.
- every commit, hold and approval path is auditable.
- real operator/equipment data stays outside Git.

## Decision Records

Architecture decisions live in `docs/adr/`. Changes to the trust model,
storage contract, public business model, operator certification or license
should add or update an ADR.

## Operator Governance

Anyone may fork and operate independently. itonami.cloud certification is a
separate trust mark and should require security, audit, support and data-flow
review.

Certified operators can lose certification for:

- bypassing policy checks
- mishandling operator/equipment data
- misrepresenting certification status
- failing to respond to security incidents
- hiding material changes to customer-facing operation
