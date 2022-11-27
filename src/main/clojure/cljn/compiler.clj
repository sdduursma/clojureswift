(ns cljn.compiler
  (:require [clojure.tools.analyzer :refer [empty-env]]
            [clojure.tools.analyzer.env :as env]
            [clojure-n.tools.analyzer.swift :as ana]
            [cljn.emitter :refer [emit]]))

(defn compile
  ([form]
   (compile form (empty-env)))
  ([form env]
   (emit (ana/analyze form env))))

#_(defn compile-form-seq
    "Compile a sequence of forms to a Swift source string."
    ([forms]
     (compile-form-seq forms
                       (when env/*env*
                         ;; TODO: Does @env/*env* have an :options key?
                         (:options @env/*env*))))
    ([forms opts]
     (with-core-cljs opts
                     (fn []
                       (with-out-str
                         (binding [ana/*cljs-ns* 'cljs.user]
                           (doseq [form forms]
                             (comp/emit (ana/analyze (ana/empty-env) form)))))))))
