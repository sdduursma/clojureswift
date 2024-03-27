(ns clojureswift.compiler-test
  (:require [clojure.test :refer :all]
            [clojureswift.compiler :as c]
            [clojureswift.emitter :as e]
            [clojure.tools.analyzer.env :as env]
            [clojure.tools.analyzer :refer [empty-env]]
            [clojureswift.tools.analyzer.swift :refer [global-env analyze]]))

(defn emit [ast]
      (env/ensure (e/emit ast)))

(def aenv (assoc-in (empty-env) [:ns] 'cljs.user))
(def cenv (global-env))

(deftest test-compiler
  (let [uuid #uuid "1369709c-2bdc-4e35-9ae1-1cde9068f672"]
    (is
      (= (with-out-str
           (c/compile '#uuid "1369709c-2bdc-4e35-9ae1-1cde9068f672" {:context :ctx/return}))
         (str "return Foundation.UUID(uuidString: \"" (.toString uuid) "\")!;\n")))))

(comment
  (analyze '(def ans 42)
           aenv)
  (c/compile '(def ans 42) aenv)
  (e/emit (analyze '(def ans 42) aenv))
  (env/with-env cenv
                (e/emit (analyze '(def ans 42)
                                 aenv)))
  (env/with-env cenv
    (with-out-str (e/emit (analyze '(def ans 42)
                                   aenv))))

  (env/with-env cenv
    (with-out-str (e/emit (analyze '(def id #uuid "1369709c-2bdc-4e35-9ae1-1cde9068f672")
                                   aenv))))
  ,)
