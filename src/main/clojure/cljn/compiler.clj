(ns cljn.compiler
  (:require [clojure-n.tools.analyzer.swift :as ana]
            [cljn.emitter :refer [emit]]))

(defn compile [form]
  (emit (ana/analyze form {})))
