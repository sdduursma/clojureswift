(ns cljn.compiler
  (:require [clojure-n.tools.analyzer.swift :as ana]
            [cljn.emitter :refer [emit]]))

(defn compile
  ([form]
   (compile form (ana/global-env)))
  ([form env]
   (emit (ana/analyze form env))))
