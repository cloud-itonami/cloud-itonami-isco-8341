(ns farmforestryops.store
  "SSoT for the ISCO-08 8341 mobile farm and forestry plant operators
  scheduling/logistics coordination actor (itonami actor pattern,
  ADR-2607121000 / CLAUDE.md Actors section; README's 'Robotics
  premise' — a scheduling and logistics coordination robot performs
  crew-roster planning, equipment service-record filing and
  maintenance-order coordination under this advisor/governor pair,
  which never dispatches hardware itself, never operates a tractor,
  harvester or other mobile plant, and never overrides an operator's
  on-site safety judgment). Modeled on cloud-itonami-isco-3313's
  accountingsupport.store.

  Domain:

    operator  — a registered equipment operator (:operator-id :name).
    equipment — a registered piece of mobile farm/forestry plant
                {:equipment-id :operator-id :name}. `:operator-id` is
                the registered operator this equipment is currently
                assigned to — a proposal citing equipment assigned to
                a different operator is out of scope for that request.
    record    — a committed coordination record (a logged service
                record, an approved crew-operation schedule, a
                coordinated maintenance order, or an escalated safety
                flag once signed off) — written ONLY via
                commit-record!. This actor never operates equipment;
                a record is scheduling/logistics data, never an
                equipment-movement command.
    ledger    — append-only audit trail, commit / hold / escalate.")

(defprotocol Store
  (operator [s operator-id])
  (equipment [s equipment-id])
  (records-of [s operator-id])
  (ledger [s])
  (register-operator! [s operator])
  (register-equipment! [s e])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (operator [_ operator-id] (get-in @a [:operators operator-id]))
  (equipment [_ equipment-id] (get-in @a [:equipment equipment-id]))
  (records-of [_ operator-id] (filter #(= operator-id (:operator-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-operator! [s op]
    (swap! a assoc-in [:operators (:operator-id op)] op) s)
  (register-equipment! [s e]
    (swap! a assoc-in [:equipment (:equipment-id e)] e) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:operators {} :equipment {} :records [] :ledger []}
                                    seed)))))
