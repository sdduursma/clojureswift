(ns cljn.compiler
  (:require [clojure.tools.analyzer :refer [empty-env]]
            [clojure.tools.analyzer.env :as env]
            [clojure.tools.analyzer.swift :as ana]
            [cljn.emitter :refer [emit]]
            [cljs.analyzer.api :as cljs-ana]))

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

(comment
  (ana/analyze '(deftype*
                  Banana
                  Banana
                  [first rest count]
                  :implements
                  (NSObject
                    (^Bool isEqual [this o] false))
                  #_(IBanana
                    (peel [this x y z] nil)))
               #_{:context :ctx/statement})
  (cljs-ana/analyze
    (cljs-ana/empty-env)
    '(let [x 42] x))
  (-> (macroexpand '(deftype Banana [foo bar] NSObject (^Bool isEqual [this o] false)))
      (nth 2)
      (nth 6)
      first
      meta)
  (compile '(deftype*
                  Banana
                  Banana
                  [first rest count]
                  :implements
                  (NSObject
                    (^Bool isEqual [this o] false))
                  (IBanana
                   (peel [this x y z] nil)))
           #_{:context :ctx/statement})
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
            :ns         'user})
  (compile '(def masa "Nelson"))
  (compile '(.capitalized masa))
  (compile '(.capitalized banaan)))
