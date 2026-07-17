(ns farmforestryops.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [farmforestryops.actor :as actor]
            [farmforestryops.advisor :as advisor]
            [farmforestryops.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-operator! st {:operator-id "operator-1" :name "K. Tanaka"})
    (store/register-equipment! st {:equipment-id "EQ-1" :operator-id "operator-1"
                                    :name "tractor-042"})
    st))

(deftest commits-a-registered-log-service-record
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:operator-id "operator-1" :op :log-service-record :stake :low
                  :equipment-id "EQ-1"}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "operator-1"))))))

(deftest commits-a-registered-schedule-crew-operation
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:operator-id "operator-1" :op :schedule-crew-operation :stake :low
                  :equipment-id "EQ-1"}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :done (:status result)))
    (is (= 1 (count (store/records-of st "operator-1"))))))

(deftest holds-on-unregistered-equipment
  (testing "an equipment-operation/movement decision can never proceed against an unregistered equipment record"
    (let [st (fresh-store)
          graph (actor/build-graph {:store st})
          request {:operator-id "operator-1" :op :log-service-record :stake :low
                    :equipment-id "EQ-ghost"}
          result (actor/run-request! graph request {} "thread-3")]
      (is (= :hold (:disposition (:state result))))
      (is (empty? (store/records-of st "operator-1"))))))

(deftest interrupts-then-approves-flag-safety-concern-on-human-approval
  (testing ":flag-safety-concern always escalates — the governor never resolves a safety concern itself"
    (let [st (fresh-store)
          graph (actor/build-graph {:store st})
          request {:operator-id "operator-1" :op :flag-safety-concern :stake :low
                    :equipment-id "EQ-1" :concern-type :equipment-defect}
          interrupted (actor/run-request! graph request {} "thread-4")]
      (is (= :interrupted (:status interrupted)))
      (is (empty? (store/records-of st "operator-1")))
      (let [resumed (actor/approve! graph "thread-4")]
        (is (= :done (:status resumed)))
        (is (= 1 (count (store/records-of st "operator-1"))))))))

(deftest interrupts-then-approves-maintenance-order-above-cost-threshold-on-human-approval
  (testing "a maintenance procurement order above the cost threshold always requires human sign-off"
    (let [st (fresh-store)
          graph (actor/build-graph {:store st})
          request {:operator-id "operator-1" :op :coordinate-maintenance-order :stake :low
                    :equipment-id "EQ-1" :estimated-cost 5000}
          interrupted (actor/run-request! graph request {} "thread-5")]
      (is (= :interrupted (:status interrupted)))
      (is (empty? (store/records-of st "operator-1")))
      (let [resumed (actor/approve! graph "thread-5")]
        (is (= :done (:status resumed)))
        (is (= 1 (count (store/records-of st "operator-1"))))))))

(def ^:private rogue-advisor
  "A stub advisor whose proposal always attempts to finalize an
  equipment-operation/movement decision — used to prove the governor's
  scope-violation hard-block is never overridable, regardless of what
  an advisor (mock or LLM) proposes."
  (reify advisor/Advisor
    (-advise [_ _store _request]
      {:op :schedule-crew-operation :effect :propose
       :equipment-id "EQ-1" :confidence 0.99 :stake :low
       :rationale "initiate the equipment movement now"})))

(deftest holds-on-scope-violation-and-is-never-overridable
  (testing "a proposal that would finalize an equipment-operation/movement decision or override the operator's on-site safety judgment never even reaches the human-approval interrupt"
    (let [st (fresh-store)
          graph (actor/build-graph {:store st :advisor rogue-advisor})
          request {:operator-id "operator-1" :op :schedule-crew-operation :stake :low
                    :equipment-id "EQ-1"}
          result (actor/run-request! graph request {} "thread-6")]
      (is (= :hold (:disposition (:state result))))
      (is (empty? (store/records-of st "operator-1"))))))
