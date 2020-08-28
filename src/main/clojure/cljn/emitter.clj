(ns cljn.emitter)

(defmulti -emit (fn [{:keys [op]} _] op))

(defn emit [ast frame]
  (-emit ast frame))

(defmethod -emit :const
  [{:keys [val env :as ast]} frame]
  (let [context (:context env)]
    (when-not (isa? context :ctx/statement)
      (case val
        nil (do (when (isa? context :ctx/return)
                  (print "return "))
                (print "nil"))
        ;; TODO: Handle other constants as well.
        ))))

(defmethod -emit :do
  [{:keys [statements ret env]} frame]
  (let [context (:context env)]
    (when (and (seq statements) (isa? context :ctx/expr)) (println "({ () -> Any? in"))
    (doseq [s statements] (emit s frame))
    (emit ret frame)
    (when (and (seq statements) (= :expr context)) (println "})()"))))
