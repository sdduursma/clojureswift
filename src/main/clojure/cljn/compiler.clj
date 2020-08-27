(ns cljn.compiler
  (:require [clojure-n.tools.analyzer.swift :as ana]))

(defn compile [form]
  (ana/analyze form {}))
