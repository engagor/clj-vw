;; Copyright (c) 2014 Engagor
;;
;; The use and distribution terms for this software are covered by the
;; BSD License (http://opensource.org/licenses/BSD-2-Clause)
;; which can be found in the file LICENSE at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clj-vw.offline-test
  (:require [clojure.test :refer :all]
            [clj-vw.offline :refer :all]
            [clj-vw.core :refer :all]))

(deftest offline-test

  ;; TODO: use fixtures
  (when (.exists (clojure.java.io/as-file "/tmp/test.dat"))
    (.delete (clojure.java.io/as-file "/tmp/test.dat")))
  (when (.exists (clojure.java.io/as-file "/tmp/test.model"))
    (.delete (clojure.java.io/as-file "/tmp/test.model")))

  (testing "train"
    
    (is (thrown-with-msg? Exception #"Unset :data file and empty examples." 
                          (train {})))

    (is (thrown-with-msg? Exception #"Non-existing :data-file and empty examples." 
                          (train (set-option {} :data "/tmp/test.dat"))))

    (is (= {:options nil,
            :data
            [{:features ["b"],
              :labels [{:value 2, :cost 1.0}],
              :tag "b1_expect_2"}
             {:features ["c"],
              :labels [{:value 3, :cost 1.0}],
              :tag "c1_expect_3"}
             {:features ["a" "b"],
              :labels [{:value 1, :cost 2.0} {:value 2, :cost 1.0}],
              :tag "ab1_expect_2"}
             {:features ["b" "c"],
              :labels [{:value 2, :cost 1.0} {:value 3, :cost 3.0}],
              :tag "bc1_expect_2"}
             {:features ["a" "c"],
              :labels [{:value 1, :cost 3.0} {:value 3, :cost 1.0}],
              :tag "ac1_expect_3"}
             {:features ["d"],
              :labels [{:value 2, :cost 3.0}],
              :tag "d1_expect_2"}],
            :features ["a"],
            :labels [{:value 1, :cost 1.0}],
            :tag "a1_expect_1"}
           (-> (add-example {:labels [{:value 1, :cost 1.0}]
                             :tag "a1_expect_1"
                             :features ["a"]}
                            {:labels [{:value 2, :cost 1.0}]
                             :tag "b1_expect_2"
                             :features ["b"]}
                            {:labels [{:value 3, :cost 1.0}]
                             :tag "c1_expect_3"
                             :features ["c"]}
                            {:labels [{:value 1, :cost 2.0}
                                      {:value 2, :cost 1.0}]
                             :tag "ab1_expect_2"
                             :features ["a" "b"]}
                            {:labels [{:value 2, :cost 1.0}
                                      {:value 3, :cost 3.0}]
                             :tag "bc1_expect_2"
                             :features ["b" "c"]}
                            {:labels [{:value 1, :cost 3.0}
                                      {:value 3, :cost 1.0}]
                             :tag "ac1_expect_3"
                             :features ["a" "c"]}
                            {:labels [{:value 2, :cost 3.0}]
                             :tag "d1_expect_2"
                             :features ["d"]})
               (train)))))

  (testing "predict (see https://github.com/JohnLangford/vowpal_wabbit/wiki/Weighted-All-Pairs-%28wap%29-multi-class-example)"

    (is (= (-> (set-option :data "/tmp/test.dat")
               (set-option :final-regressor "/tmp/test.model")
               (set-option :wap 3)
               (add-example {:labels [{:value 1, :cost 1.0}]
                             :tag "a1_expect_1"
                             :features ["a"]}
                            {:labels [{:value 2, :cost 1.0}]
                             :tag "b1_expect_2"
                             :features ["b"]}
                            {:labels [{:value 3, :cost 1.0}]
                             :tag "c1_expect_3"
                             :features ["c"]}
                            {:labels [{:value 1, :cost 2.0}
                                      {:value 2, :cost 1.0}]
                             :tag "ab1_expect_2"
                             :features ["a" "b"]}
                            {:labels [{:value 2, :cost 1.0}
                                      {:value 3, :cost 3.0}]
                             :tag "bc1_expect_2"
                             :features ["b" "c"]}
                            {:labels [{:value 1, :cost 3.0}
                                      {:value 3, :cost 1.0}]
                             :tag "ac1_expect_3"
                             :features ["a" "c"]}
                            {:labels [{:value 2, :cost 3.0}]
                             :tag "d1_expect_2"
                             :features ["d"]})
               (train)
               (predict))
           {:predictions
            [{:label 1.0, :tag "a1_expect_1"}
             {:label 2.0, :tag "b1_expect_2"}
             {:label 3.0, :tag "c1_expect_3"}
             {:label 1.0, :tag "ab1_expect_2"}
             {:label 2.0, :tag "bc1_expect_2"}
             {:label 3.0, :tag "ac1_expect_3"}
             {:label 2.0, :tag "d1_expect_2"}],
            :data
            [{:features ["a"],
              :labels [{:value 1, :cost 1.0}],
              :tag "a1_expect_1"}
             {:features ["b"],
              :labels [{:value 2, :cost 1.0}],
              :tag "b1_expect_2"}
             {:features ["c"],
              :labels [{:value 3, :cost 1.0}],
              :tag "c1_expect_3"}
             {:features ["a" "b"],
              :labels [{:value 1, :cost 2.0} {:value 2, :cost 1.0}],
              :tag "ab1_expect_2"}
             {:features ["b" "c"],
              :labels [{:value 2, :cost 1.0} {:value 3, :cost 3.0}],
              :tag "bc1_expect_2"}
             {:features ["a" "c"],
              :labels [{:value 1, :cost 3.0} {:value 3, :cost 1.0}],
              :tag "ac1_expect_3"}
             {:features ["d"],
              :labels [{:value 2, :cost 3.0}],
              :tag "d1_expect_2"}],
            :options
            [[:data "/tmp/test.dat"]
             [:final-regressor "/tmp/test.model"]
             [:wap 3]]})))
  
  )

