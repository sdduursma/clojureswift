(ns cljn.emitter)

(defmulti -emit (fn [{:keys [op]} _] op))

(defn emit [ast frame]
  (-emit ast frame))

(def emitln println)

(def emits print)

(defmacro emit-contextually
  "Macro that wraps its body with, for example, 'return' and ';', depending on the context."
  [env & body]
  `(let [env# ~env]
     (when (isa? (:context env#) :ctx/return) (emits "return "))
     ~@body
     (when-not (= (:context env#) :ctx/expr) (emitln ";"))))

;; Why class instead of type?
(defmulti emit-constant* class)

(defmethod emit-constant* nil [_]
  (emits "nil"))

(defmethod emit-constant* Long [x]
  (emits x))

;; TODO: Integers?

(defmethod emit-constant* Double [x]
  (let [x (double x)]
    (cond
      ;; TODO: Double.nan or Double.signalingNaN?
      (Double/isNaN x) (emits "Double.nan")
      (Double/isInfinite x) (emits (if (pos? x)
                                     "Double.infinity"
                                     "(-Double.infinity)"))
      :else (emits x))))

(defmethod emit-constant* Boolean [x]
  (emits (if x "true" "false")))

(defmethod emit-constant* java.util.UUID [^java.util.UUID uuid]
  ;; Force unwrap is OK because we know uuid is actually a UUID.
  ;; Use different UUID implementation because Foundation.UUID returns uppercase strings?
  (emits (str "Foundation.UUID(uuidString: \"" (.toString uuid) "\")!")))

(defn emit-constant-no-meta [v]
  ;; TODO: Emit lists, vectors, maps, etc.
  (emit-constant* v))

(defn emit-constant [v]
  ;; TODO: Elide metadata?
  (emit-constant-no-meta v))

(defmethod -emit :const
  [{:keys [env form]} frame]
  (let [context (:context env)]
    (when-not (isa? context :ctx/statement)
      (emit-contextually env (emit-constant form)))))

(defmethod -emit :do
  [{:keys [statements ret env]} frame]
  (let [context (:context env)]
    (when (and (seq statements) (isa? context :ctx/expr)) (emitln "({ () -> Any? in"))
    (doseq [s statements] (emit s frame))
    (emit ret frame)
    (when (and (seq statements) (= :expr context)) (emitln "})()"))))
