(ns cljn.emitter
  (:require [clojure.string :as str])
  (:import (java.util.concurrent.atomic AtomicLong)
           (java.io Writer)))

(def ^:dynamic *source-map-data* nil)
(def ^:dynamic *source-map-data-gen-col* nil)

(defmulti -emit (fn [{:keys [op]}] op))

(declare emits)
(declare emitln)

(defmacro emit-contextually
  "Macro that wraps its body with, for example, 'return' and ';', depending on the env's context."
  [env & body]
  `(let [env# ~env]
     (when (isa? (:context env#) :ctx/return) (emits "return "))
     ~@body
     (when-not (= (:context env#) :ctx/expr) (emitln ";"))))

(defn emit [ast]
  (emit-contextually (:env ast) (-emit ast)))

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

;; TODO: Implement
(defn munge
  ([s]
   s)
  ([s reserved]
   s))

(defmethod -emit :binding
  [{:keys [local name mutable]}]
  (case local
    :arg (emits "_ " (munge name) ": Any?")
    ;; TODO: Support mutable
    ;; TODO: Support type (hints)
    :field (emits "let " name ": Any?")))

(defn emit-let
  [{:keys [bindings body env]} is-loop]
  (let [context (:context env)]
    (when (isa? context :ctx/expr) (emitln "({ () -> Any? in"))
    (doseq [{:keys [init] :as binding} bindings]
      (emitln "do {")
      ;; TODO: Can it be a let?
      (emits "var ")
      (emits (:name binding))
      (emitln " = " init ";"))
    (when is-loop (emitln "while(true) {"))
    (emits body)
    (when is-loop
      (emitln "break;")
      (emitln "}"))
    (doseq [_ bindings]
      (emitln "}"))
    (when (isa? context :ctx/expr) (emits "})()"))))

(defmethod -emit :let [ast]
  (emit-let ast false))

(defmethod -emit :if
  [{:keys [test then else env]}]
  (let [context (:context env)
        test-sym (gensym "test_")]
    (if (isa? context :ctx/expr)
      (do (emitln "{ () -> Any? in")
          (emitln "let " test-sym " = " test)
          (emitln "return (" test-sym " != nil && (" test-sym " as? Bool ?? true)) ? " then " : " else)
          (emitln "}()"))
      (do (emitln "let " test-sym " = " test ";")
          (emitln "if (" test-sym " != nil && (" test-sym " as? Bool ?? true)) {")
          (emit then)
          (emitln "} else {")
          (emit else)
          (emitln "}")))))

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

(defn- escape-char [^Character c]
  (let [cp (.hashCode c)]
    ;; TODO: Check if espcape rules are appropriate for Swift.
    (case cp
      ; Handle printable escapes before ASCII
      34 "\\\""
      92 "\\\\"
      ; Handle non-printable escapes
      8 "\\b"
      12 "\\f"
      10 "\\n"
      13 "\\r"
      9 "\\t"
      (if (< 31 cp 127)
        c ; Print simple ASCII characters
        (format "\\u%04X" cp) ; Any other character is Unicode
        ))))

(defn- escape-string [^CharSequence s]
  (let [sb (StringBuilder. (count s))]
    (doseq [c s]
      (.append sb (escape-char c)))
    (.toString sb)))

(defn- wrap-in-double-quotes [x]
  (str \" x \"))

(defmethod emit-constant* Character [x]
  ;; TODO: Probably better to emit a literal, however this requires explicitly specifying the type in the binding.
  (emits "Character(" (wrap-in-double-quotes (escape-char x)) ")"))

(defmethod emit-constant* String [x]
  (emits (wrap-in-double-quotes (escape-string x))))

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
      (emit-constant form))))

(defmethod -emit :do
  [{:keys [statements ret env]}]
  (let [context (:context env)]
    (when (isa? context :ctx/expr) (emitln "({ () -> Any? in"))
    (doseq [s statements] (emit s))
    (emit ret)
    (when (isa? context :ctx/expr) (emitln "})()"))))

(defn emit-local [name]
  (emits name))

(defmethod -emit :local
  [{:keys [name env]}]
  (let [context (:context env)]
    (when-not (isa? context :ctx/statement)
      (emit-local name))))

(defmethod -emit :maybe-class
  [{:keys [class]}]
  (emits class))

(defn- comma-sep [xs]
  (interpose "," xs))

(defn emit-dot
  [{:keys [target field method args env]}]
  (if field
    (emits target "." (munge field #{}))
    (let [[method-name & arg-labels] (str/split (name method) #"\:")
          colons (repeat ": ")
          commas (repeat ", ")]
      (emits target "." (munge method-name #{}) "("
           (drop-last (interleave arg-labels colons args commas))
           ")"))))

(defmethod -emit :host-field [ast] (emit-dot ast))
(defmethod -emit :host-call [ast] (emit-dot ast))

(defmethod -emit :method
  [{:keys [form name params body]}]
  ;; TODO: Infer throwing from protocol.
  ;; TODO: Access control
  ;; TODO: Infer return type from protocol.
  (emitln "func " (munge name) "(" (comma-sep params) ") -> Any? {")
  (emits body)
  (emitln "}"))

(defmethod -emit :deftype
  [{:keys [form name class-name nsobject swift-protocols fields methods env]}]
  ;; TODO: Access control
  (emits "class " name)
  ;; Superclass needs to be first
  (let [nsobject-swift-protocols (into (if nsobject [nsobject] [])
                                       swift-protocols)]
    (when (not (empty? nsobject-swift-protocols))
      (emits ": " (comma-sep nsobject-swift-protocols)))
    (emitln " {")
    (when (not (empty? fields))
      (doseq [f fields]
        (emits f))
      ;; TODO: Here we pretend that there are AST nodes for the init params.
      ;; Does this really make sense? What if the fieds' AST contains data that doesn't apply to the
      ;; init params?
      (let [init-params (mapv (fn [f] (-> f
                                          (assoc :local :arg)
                                          (assoc-in [:env :context] :ctx/expr)))
                              fields)]
        ;; TODO: Access control
        (emitln "init(" (comma-sep init-params) ") {")
        (doseq [f fields]
          (emitln "self." (munge (:name f)) " = " (munge (:name f)) ";"))
        (emitln "}")))
    (doseq [m methods]
      (emitln m))
    (emitln "}")))
