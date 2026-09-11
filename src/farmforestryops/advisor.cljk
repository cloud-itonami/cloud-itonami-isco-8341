(ns farmforestryops.advisor
  "Farm/Forestry Ops Advisor — the advisor named in this repository's
  README, proposing a mobile farm/forestry plant scheduling or
  logistics-coordination action (log a service record, schedule a
  crew operation, flag a safety concern, coordinate a maintenance
  order) from an operator roster, equipment registry and field-work
  request. Swappable mock/llm; the advisor ONLY proposes — it never
  finalizes an equipment-operation/movement decision and never
  overrides an operator's on-site safety judgment. This actor
  coordinates SCHEDULING/LOGISTICS ONLY; it never operates the
  equipment. `farmforestryops.governor` independently checks operator
  and equipment provenance, the closed op-allowlist, and always
  escalates safety-concern flags and above-threshold maintenance
  orders. Modeled on cloud-itonami-isco-3313's advisor.

  A proposal: {:op :log-service-record|:schedule-crew-operation|
                    :flag-safety-concern|:coordinate-maintenance-order
               :effect :propose :equipment-id str
               :estimated-cost number? :concern-type kw?
               :stake kw :confidence n :rationale str}")

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(defn- infer [_store {:keys [op stake operator-id equipment-id estimated-cost
                              concern-type] :as request}]
  (cond-> {:op op
           :effect :propose
           :equipment-id equipment-id
           :stake (or stake :low)
           :confidence (case (or stake :low) :high 0.7 :medium 0.85 :low 0.95)
           :rationale (str "propose " (name op) " for operator " operator-id
                            " on equipment " equipment-id)}
    (some? estimated-cost) (assoc :estimated-cost estimated-cost)
    (some? concern-type) (assoc :concern-type concern-type)))

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are a farm/forestry operations scheduling and logistics
   coordinator advisor. Given a field-work request, propose an :op
   (log-service-record, schedule-crew-operation, flag-safety-concern
   or coordinate-maintenance-order), the :equipment-id, an honest
   :confidence and a :stake. You coordinate SCHEDULING and LOGISTICS
   ONLY — never propose finalizing an equipment-operation/movement
   decision, and never propose overriding an operator's on-site
   safety judgment; those are always out of scope. The governor
   checks operator and equipment provenance against the registry and
   always requires human sign-off for safety-concern flags and
   maintenance orders above the cost threshold, regardless of
   confidence.")

(defn- parse-proposal [content]
  (try
    (let [p (read-string content)]
      (if (map? p)
        (assoc p :effect :propose)
        {:op :unknown :effect :propose :confidence 0.0 :stake :high
         :rationale "unparseable LLM response"}))
    (catch #?(:clj Exception :cljs js/Error) _
      {:op :unknown :effect :propose :confidence 0.0 :stake :high
       :rationale "LLM response parse failure"})))

(defn llm-advisor
  [chat-model model-generate-fn gen-opts]
  (reify Advisor
    (-advise [_ _store request]
      (let [msgs [{:role :system :content system-prompt}
                  {:role :user :content (str "field-work request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))
