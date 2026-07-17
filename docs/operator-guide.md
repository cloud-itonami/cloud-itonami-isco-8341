# Operator Guide

## First Deployment

1. Define the operator's service area and field-work intake process.
2. Register every equipment operator and piece of mobile farm/forestry plant before accepting any coordination request for it.
3. Run synthetic operating cases, including scope-violation attempts (proposals that try to finalize an equipment-operation/movement decision or override an operator's on-site safety judgment) to confirm they hard-hold.
4. Enable human-reviewed sign-off for `:high`/`:safety-critical` actions (safety-concern flags and above-threshold maintenance orders).
5. Measure operating outcomes and audit coverage.

## Minimum Production Controls

- consent and disclosure log
- safety-critical escalation path (equipment-defect, terrain-hazard and operator-fatigue concerns always reach a human)
- provenance for all operating records (registered operator + registered equipment)
- human review for high-risk cases
- audit export for all gated actions

## Scope Boundary

This actor coordinates SCHEDULING and LOGISTICS ONLY. It never operates a
tractor, harvester or other mobile plant, and it never overrides an
operator's on-site safety judgment — those decisions stay with the human
operator on site, always.

## Certification

Certified operators must prove that the governor gates every safety-critical
robot action, that safety-critical risks escalate to humans, and that no
op on the closed allowlist ever finalizes an equipment-operation/movement
decision.
