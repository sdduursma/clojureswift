(ns cljn.emitter-test
  (:require [clojure.test :refer :all]
            [cljn.emitter :refer :all]))

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
