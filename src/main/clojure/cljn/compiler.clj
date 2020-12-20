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
                  user/PersistentList
                  user.PersistentList
                  [first rest count]
                  :implements
                  [NSObject]
                  (isEqual [this o] false))
               {})

  (compile '(deftype*
                  Banana
                  Banana
                  [first rest count]
                  :implements
                  [NSObject Codable]
                  (isEqual [this o] false))
           {:context :ctx/statement}))
