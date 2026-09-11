# cloud-itonami-isco-8341

Open Occupation Blueprint for **ISCO-08 8341**: Mobile Farm and Forestry Plant Operators.

This repository designs a forkable OSS business for mobile farm and forestry plant scheduling/logistics coordination: a scheduling and logistics coordination robot manages operator rosters, equipment service records and maintenance-order coordination under a governor-gated actor, so a farm or forestry contracting crew keeps its own coordination records instead of renting a closed fleet-dispatch SaaS.

**Maturity: `:implemented`.** `src/farmforestryops/` implements the
`FarmForestryOpsActor` as a `langgraph.graph/state-graph`
(`farmforestryops.actor`) wired to a `Farm/Forestry Ops Advisor`
(`farmforestryops.advisor`) and an independent `FarmForestryOpsGovernor`
(`farmforestryops.governor`), following the itonami actor pattern
(ADR-2607121000): `:intake -> :advise -> :govern -> :decide -+-> :commit
(:ok?) +-> :request-approval (:escalate?, human-in-the-loop interrupt)
+-> :hold (:hard?)`. 19 tests / 44 assertions green (`kbb -M:test`).
HARD invariants (always hold, never overridable): operator provenance,
no-actuation (`:effect` must be `:propose`), a closed op-allowlist
(no op that finalizes an equipment-operation/movement decision is ever
on it), a registered-equipment basis for any proposal, and a hard,
permanent block on any proposal that would directly finalize an
equipment-operation/movement decision or override an operator's
on-site safety judgment, however phrased. Always-escalate paths (human
sign-off regardless of confidence, mapping this repo's Trust Controls
in [`docs/business-model.md`](docs/business-model.md)):
`:flag-safety-concern` (always) and `:coordinate-maintenance-order`
above the cost-escalation threshold.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a scheduling and logistics coordination robot performs operator-roster planning, equipment service-record filing and maintenance-order coordination under an actor that proposes actions and an independent **Farm/Forestry Ops Governor** that gates them. The governor never
dispatches hardware itself and never operates equipment (tractors, harvesters and other mobile plant — real physical-safety stakes: rollover, entanglement); `:high`/`:safety-critical` actions (such as safety-concern flags and above-threshold maintenance orders) require human sign-off. This actor coordinates SCHEDULING and LOGISTICS ONLY — it never operates the equipment and never overrides an operator's on-site safety judgment.

## Core Contract

```text
operator roster + equipment registry + field-work request
        |
        v
Farm/Forestry Ops Advisor -> Farm/Forestry Ops Governor -> log/schedule/coordinate, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, finalize
an equipment-operation/movement decision, override an operator's on-site
safety judgment, or suppress an operating record without governor approval
and audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `8341`). Required capabilities:

- :robotics
- :identity
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
