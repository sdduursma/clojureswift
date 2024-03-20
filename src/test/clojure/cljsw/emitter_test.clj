(ns cljsw.emitter-test
  (:require [clojure.test :refer :all]
            [cljsw.emitter :refer :all]))

(deftest test-emit-nil
  (is (= (with-out-str
           (emit-constant* nil))
         "nil")))

(deftest test-emit-long
  (is (= (with-out-str
           (emit-constant* 42)))
      "42"))

(deftest test-emit-double
  (is (= (with-out-str
           (emit-constant* 3.14))
         "3.14")))

(deftest test-emit-double-nan
  (is (= (with-out-str
           (emit-constant* Double/NaN))
         "Double.nan")))

(deftest test-emit-double-positive-infinity
  (is (= (with-out-str
           (emit-constant* ##Inf))
         "Double.infinity")))

(deftest test-emit-double-positive-infinity
  (is (= (with-out-str
           (emit-constant* ##-Inf))
         "(-Double.infinity)")))

(deftest test-emit-bool
  (is (= (with-out-str
           (emit-constant* true))
         "true"))
  (is (= (with-out-str
           (emit-constant* false))
         "false")))

(deftest test-emit-char
  (is (= (with-out-str
           (emit-constant* \C))
         "Character(\"C\")")))

(deftest test-emit-string
  (is (= (with-out-str
           (emit-constant* "foo"))
         "\"foo\"")))

(deftest test-emit-uuid
  (is (= (with-out-str
           (emit-constant* #uuid "1369709c-2bdc-4e35-9ae1-1cde9068f672"))
         "Foundation.UUID(uuidString: \"1369709c-2bdc-4e35-9ae1-1cde9068f672\")!")))

(deftest test-emit-simple-type
  (is (= (with-out-str
           (-emit {:op :deftype
                   :form '(deftype A [x])
                   :name 'A
                   :fields [{:op :binding
                             :name 'x
                             :local :field}]}))
         "class A {
let x: Any?;
init(_ x: Any?) {
self.x = x;
}
}
")))

(deftest test-emit-dot-method
  (is (= (with-out-str
           (-emit {:args [],
                   :children [:target :args],
                   :method 'uppercased,
                   :op :host-call,
                   :top-level true,
                   :form '(. "foo" uppercased),
                   :target {:op :const, :env {:context :ctx/expr}, :type :string, :literal? true, :val "foo", :form "foo"},
                   :raw-forms '((.uppercased "foo"))}))
         "\"foo\".uppercased()")))

(deftest test-emit-dot-field
  (is (= (with-out-str
           (-emit {:children [:target],
                   :field 'count,
                   :op :host-field,
                   :top-level true,
                   :form '(. "foo" -count),
                   :target {:op :const, :env {:context :ctx/expr}, :type :string, :literal? true, :val "foo", :form "foo"},
                   :assignable? true,
                   :raw-forms '((.-count "foo"))}))
         "\"foo\".count")))
