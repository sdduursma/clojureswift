(ns cljn.emitter
  (:import (java.util.concurrent.atomic AtomicLong)
           (java.io Writer)))

(def ^:dynamic *source-map-data* nil)
(def ^:dynamic *source-map-data-gen-col* nil)

(defmulti -emit (fn [{:keys [op]}] op))

(defn emit [ast]
  (-emit ast))

(def emitln println)

(defn emits
  ([])
  ([^Object a]
   (cond
     (nil? a) nil
     (map? a) (emit a)
     (seq? a) (apply emits a)
     (fn? a) (a)
     :else (let [^String s (cond-> a (not (string? a)) .toString)]
             (when-some [^AtomicLong gen-col *source-map-data-gen-col*]
               (.addAndGet gen-col (.length s)))
             (.write ^Writer *out* s)))
   nil)
  ([a b]
   (emits a) (emits b))
  ([a b c]
   (emits a) (emits b) (emits c))
  ([a b c d]
   (emits a) (emits b) (emits c) (emits d))
  ([a b c d e]
   (emits a) (emits b) (emits c) (emits d) (emits e))
  ([a b c d e & xs]
   (emits a) (emits b) (emits c) (emits d) (emits e)
   (doseq [x xs] (emits x))))

(defn ^:private _emitln []
  (newline)
  (when *source-map-data*
    (.set ^AtomicLong *source-map-data-gen-col* 0)
    (swap! *source-map-data*
           (fn [{:keys [gen-line] :as m}]
             (assoc m
               :gen-line (inc gen-line)))))
  nil)

(defn emitln
  ([] (_emitln))
  ([a]
   (emits a) (_emitln))
  ([a b]
   (emits a) (emits b) (_emitln))
  ([a b c]
   (emits a) (emits b) (emits c) (_emitln))
  ([a b c d]
   (emits a) (emits b) (emits c) (emits d) (_emitln))
  ([a b c d e]
   (emits a) (emits b) (emits c) (emits d) (emits e) (_emitln))
  ([a b c d e & xs]
   (emits a) (emits b) (emits c) (emits d) (emits e)
   (doseq [x xs] (emits x))
   (_emitln)))

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
  (emits "Foundation.UUID(uuidString: \"" (.toString uuid) "\")!"))

(defn emit-constant-no-meta [v]
  ;; TODO: Emit lists, vectors, maps, etc.
  (emit-constant* v))

(defn emit-constant [v]
  ;; TODO: Elide metadata?
  (emit-constant-no-meta v))

(defmethod -emit :const
  [{:keys [env form]}]
  (let [context (:context env)]
    (when-not (isa? context :ctx/statement)
      (emit-contextually env (emit-constant form)))))

(defmethod -emit :do
  [{:keys [statements ret env]}]
  (let [context (:context env)]
    (when (and (seq statements) (isa? context :ctx/expr)) (emitln "({ () -> Any? in"))
    (doseq [s statements] (emitln s))
    (emit ret)
    (when (and (seq statements) (= :expr context)) (emitln "})()"))))
