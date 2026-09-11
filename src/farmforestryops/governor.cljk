(ns farmforestryops.governor
  "FarmForestryOpsGovernor — the independent safety/traceability layer
  named in this repository's README/business-model.md, gating every
  scheduling/logistics proposal an advisor may make for mobile farm
  and forestry plant operators (tractors, harvesters and other
  mobile plant — real physical-safety stakes: rollover, entanglement).
  The governor never dispatches hardware itself, never operates
  equipment, and never overrides an operator's on-site safety
  judgment; it only gates what the advisor may schedule, log or
  coordinate. Modeled on cloud-itonami-isco-3313's
  accountingsupport.governor.

  Task twist: this actor coordinates SCHEDULING and LOGISTICS ONLY —
  it never operates the equipment. The closed op-allowlist below
  contains no op that directly finalizes an equipment-operation/
  movement decision or overrides an operator's on-site safety
  judgment; any such op, however phrased, is a hard, permanent block
  (never overridable by human approval — the graph never even reaches
  the human-approval interrupt for a hard violation).

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. operator provenance     — the equipment operator must be
                                registered.
    2. no-actuation            — proposal :effect must be :propose
                                (the governor never dispatches
                                hardware and never operates equipment;
                                it only gates what the advisor may
                                schedule, log or coordinate).
    3. closed op-allowlist     — :op must be one of the four
                                registered scheduling/logistics ops
                                (:log-service-record,
                                :schedule-crew-operation,
                                :flag-safety-concern,
                                :coordinate-maintenance-order). No op
                                that finalizes an equipment-operation/
                                movement decision is ever on this
                                allowlist.
    4. equipment basis         — a proposal must cite a REGISTERED
                                piece of equipment currently assigned
                                to this request's operator.
    5. scope exclusion         — any proposal that would directly
                                finalize an equipment-operation/
                                movement decision, or override an
                                operator's on-site safety judgment
                                (however phrased in its rationale), is
                                a hard, permanent block. This is
                                checked as full finalization/execution
                                ACTION phrases (e.g. \"initiate the
                                equipment movement\", \"override the
                                operator's on-site safety judgment\"),
                                never bare nouns like \"equipment\" or
                                \"movement\" — a bare-noun exclusion
                                list would false-trip on the mock
                                advisor's own default rationale text
                                (e.g. \"...on equipment E-1\"), which
                                legitimately mentions equipment by
                                name. See
                                farmforestryops.governor-test's
                                default-mock-advisor-proposals-never-
                                self-trip-scope-violation test.
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off per
  business-model.md's Trust Controls — these are :high/
  :safety-critical regardless of confidence):
    6. :op :flag-safety-concern (an equipment-defect, terrain-hazard
                                or operator-fatigue concern always
                                requires human review — the governor
                                never resolves a safety concern
                                itself).
    7. :op :coordinate-maintenance-order with :estimated-cost above
                                `maintenance-cost-escalation-threshold`
                                (a maintenance procurement order above
                                the cost threshold always requires
                                human sign-off).
    8. low confidence (< `confidence-floor`)."
  (:require [kotoba.lang.text :as str]
            [farmforestryops.store :as store]))

(def confidence-floor 0.6)
(def maintenance-cost-escalation-threshold 2000)

(def ^:private allowed-ops
  #{:log-service-record :schedule-crew-operation :flag-safety-concern
    :coordinate-maintenance-order})

(def ^:private always-escalate-ops #{:flag-safety-concern})

;; Scope-exclusion terms are phrased as finalization/execution ACTIONS,
;; never as bare nouns — a bare noun like "equipment" or "movement"
;; would match inside the mock advisor's own default rationale text
;; ("propose ... on equipment E-1"), causing a false self-block. See
;; docstring point 5 above and the dedicated self-trip regression test.
(def ^:private scope-exclusion-phrases
  ["finalize the equipment movement"
   "finalize the equipment-operation decision"
   "finalize equipment movement"
   "initiate the equipment movement"
   "directly operate the equipment"
   "directly dispatch the equipment"
   "override the operator's on-site safety judgment"
   "override the operator's safety judgment"
   "override the operator's on-site judgment"])

(defn- scope-violation? [proposal]
  (let [text (str/lower (str (:rationale proposal)))]
    (boolean (some #(str/includes? text %) scope-exclusion-phrases))))

(defn- hard-violations [{:keys [request proposal]} operator-record e]
  (let [{:keys [op]} proposal]
    (cond-> []
      (nil? operator-record)
      (conj {:rule :no-operator :detail "未登録 operator"})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation
             :detail "effect は :propose のみ許可（governor は機材操作・移動判断を直接実行しない）"})

      (not (contains? allowed-ops op))
      (conj {:rule :op-not-allowlisted
             :detail "許可されていない op（closed allowlist 外。機材操作/移動を確定する op は allowlist に存在しない）"})

      (nil? e)
      (conj {:rule :unknown-equipment :detail "未登録 equipment への提案は不可"})

      (and e (not= (:operator-id e) (:operator-id request)))
      (conj {:rule :equipment-wrong-operator :detail "equipment が別 operator に割当済み"})

      (scope-violation? proposal)
      (conj {:rule :scope-violation
             :detail "機材操作/移動判断の直接確定、またはオペレーターの現場安全判断の上書きは常時・恒久的にブロック（このアクターはスケジューリング/ロジスティクス調整のみを行い、機材を操作しない）"}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `farmforestryops.store/Store`. Pure — never
  mutates the store, never operates equipment, never overrides an
  operator's on-site safety judgment."
  [request context proposal store]
  (let [operator-record (store/operator store (:operator-id request))
        e (some->> (:equipment-id proposal) (store/equipment store))
        hard (hard-violations {:request request :proposal proposal} operator-record e)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        always-risky? (contains? always-escalate-ops (:op proposal))
        cost-over? (and (= :coordinate-maintenance-order (:op proposal))
                         (number? (:estimated-cost proposal))
                         (> (:estimated-cost proposal) maintenance-cost-escalation-threshold))]
    {:ok? (and (not hard?) (not low?) (not always-risky?) (not cost-over?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky? cost-over?))}))
