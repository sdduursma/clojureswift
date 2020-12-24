(ns cljn.compiler
  (:require [clojure-n.tools.analyzer.swift :as ana]
            [cljn.emitter :refer [emit]]))

(defn compile
  ([form]
   (compile form (ana/global-env)))
  ([form env]
   (emit (ana/analyze form env))))

(comment
  (ana/analyze '(deftype*
                  Banana
                  Banana
                  [first rest count]
                  :implements
                  [NSObject Codable]
                  (isEqual [this o] false)
                  (peel [this x y z] nil))
               {:context :ctx/statement})

  (compile '(deftype*
                  Banana
                  Banana
                  [first rest count]
                  :implements
                  [NSObject Codable]
                  (isEqual [this o] false)
                  (peel [this x y z] nil))
           {:context :ctx/statement})
  (compile '(let [a 42]
              a)
           {:context :ctx/expr})
  (compile '(in-ns 'user))
  (compile '(deftype*
              Fraction
              Fraction
              [numerator denominator]
              :implements
              [NSObject]
              (description [this]
                (.appending numerator "/" denominator)))
           {:context    :ctx/statement
            :locals     {}
            :ns         'user}))
