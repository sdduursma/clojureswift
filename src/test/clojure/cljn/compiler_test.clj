(ns cljn.compiler-test
  (:require [clojure.test :refer :all]
            [cljn.compiler :as c]))

(deftest test-compiler
  (let [uuid #uuid "1369709c-2bdc-4e35-9ae1-1cde9068f672"]
    (is
      (= (with-out-str
           (c/compile '#uuid "1369709c-2bdc-4e35-9ae1-1cde9068f672" {:context :ctx/return}))
         (str "return Foundation.UUID(uuidString: \"" (.toString uuid) "\")!;\n")))
    #_(is
      (= (with-out-str
           (c/compile '(let [a (.uppercased "abc")]
                         a)
                      {:context :ctx/return}))
         (str "return ({ () -> Any? in do { var a = \"abc\".uppercased(); return a; } }()); \n")))))
