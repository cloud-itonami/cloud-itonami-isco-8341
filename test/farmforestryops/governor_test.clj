(ns farmforestryops.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [farmforestryops.store :as store]
            [farmforestryops.advisor :as advisor]
            [farmforestryops.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-operator! st {:operator-id "operator-1" :name "K. Tanaka"})
    (store/register-equipment! st {:equipment-id "EQ-1" :operator-id "operator-1"
                                    :name "tractor-042"})
    st))

(defn- log-op [equipment-id]
  {:op :log-service-record :effect :propose :equipment-id equipment-id
   :confidence 0.9 :stake :low})

(def ^:private req {:operator-id "operator-1"})

(deftest ok-registered-operator-and-equipment
  (let [st (fresh-store)
        v (governor/check req {} (log-op "EQ-1") st)]
    (is (:ok? v))))

(deftest hard-on-unregistered-operator
  (let [st (fresh-store)
        v (governor/check {:operator-id "nobody"} {} (log-op "EQ-1") st)]
    (is (:hard? v))
    (is (some #(= :no-operator (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        v (governor/check req {} (assoc (log-op "EQ-1") :effect :direct-write) st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest hard-on-op-not-allowlisted
  (testing "no op that finalizes an equipment-operation/movement decision is ever on the allowlist"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (log-op "EQ-1") :op :finalize-equipment-movement) st)]
      (is (:hard? v))
      (is (some #(= :op-not-allowlisted (:rule %)) (:violations v))))))

(deftest hard-on-unknown-equipment
  (let [st (fresh-store)
        v (governor/check req {} (log-op "EQ-ghost") st)]
    (is (:hard? v))
    (is (some #(= :unknown-equipment (:rule %)) (:violations v)))))

(deftest hard-on-foreign-equipment
  (let [st (fresh-store)]
    (store/register-operator! st {:operator-id "operator-2" :name "Other"})
    (let [v (governor/check {:operator-id "operator-2"} {} (log-op "EQ-1") st)]
      (is (:hard? v))
      (is (some #(= :equipment-wrong-operator (:rule %)) (:violations v))))))

(deftest hard-on-scope-violation-finalizing-equipment-movement
  (testing "a proposal that would finalize an equipment-operation/movement decision is a hard, permanent block, however phrased in its rationale"
    (let [st (fresh-store)
          v (governor/check req {}
                             (assoc (log-op "EQ-1")
                                    :op :schedule-crew-operation
                                    :rationale "recommend the crew initiate the equipment movement immediately without further review")
                             st)]
      (is (:hard? v))
      (is (some #(= :scope-violation (:rule %)) (:violations v))))))

(deftest hard-on-scope-violation-overriding-operator-safety-judgment
  (testing "a proposal that would override the operator's on-site safety judgment is a hard, permanent block"
    (let [st (fresh-store)
          v (governor/check req {}
                             (assoc (log-op "EQ-1")
                                    :op :coordinate-maintenance-order
                                    :estimated-cost 100
                                    :rationale "override the operator's on-site safety judgment and proceed with the field pass")
                             st)]
      (is (:hard? v))
      (is (some #(= :scope-violation (:rule %)) (:violations v))))))

(deftest always-escalates-flag-safety-concern-even-at-high-confidence
  (testing "the governor never resolves a safety concern itself"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (log-op "EQ-1") :op :flag-safety-concern
                                          :concern-type :equipment-defect :confidence 0.99) st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest escalates-maintenance-order-above-cost-threshold
  (testing "a maintenance procurement order above the cost threshold always requires human sign-off"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (log-op "EQ-1") :op :coordinate-maintenance-order
                                          :estimated-cost 5000 :confidence 0.99) st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest ok-at-exact-cost-threshold-boundary
  (testing "the cost-escalation threshold is inclusive (at-threshold does not escalate)"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (log-op "EQ-1") :op :coordinate-maintenance-order
                                          :estimated-cost 2000 :confidence 0.9) st)]
      (is (:ok? v))
      (is (not (:escalate? v))))))

(deftest escalates-low-confidence
  (let [st (fresh-store)
        v (governor/check req {} (assoc (log-op "EQ-1") :confidence 0.3) st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))

(deftest default-mock-advisor-proposals-never-self-trip-scope-violation
  (testing "the governor's own scope-exclusion phrases must never match the mock advisor's own default rationale text (regression: bare-noun exclusion terms like \"equipment\"/\"movement\" would false-trip here)"
    (let [st (fresh-store)
          adv (advisor/mock-advisor)
          ops [:log-service-record :schedule-crew-operation
               :flag-safety-concern :coordinate-maintenance-order]]
      (doseq [op ops]
        (let [request {:operator-id "operator-1" :op op :equipment-id "EQ-1"
                        :stake :low :estimated-cost 100 :concern-type :equipment-defect}
              proposal (advisor/-advise adv st request)
              v (governor/check request {} proposal st)]
          (is (not (some #(= :scope-violation (:rule %)) (:violations v)))
              (str "op " op " unexpectedly self-tripped :scope-violation; rationale=" (:rationale proposal))))))))
