;; Copyright (c) 2014 Engagor
;;
;; The use and distribution terms for this software are covered by the
;; BSD License (http://opensource.org/licenses/BSD-2-Clause)
;; which can be found in the file LICENSE at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clj-vw.core-test
  (:require [clojure.test :refer :all]
            [clj-vw.core :refer :all]))

(deftest examples-test

  (testing "Formating of examples"

    (is (= (format-example {:labels [{:value -1.231}]
                            :features [{:name "f1" :value -1.2}
                                       {:name "f2" :value 3.5}
                                       {:name "f3" :namespace {:name "ns3" :weight 1.0}}]})
           "-1.231 | f1:-1.2 f2:3.5 |ns3:1.0 f3"))
    
    (is (= (format-example {:labels [{:value 1, :cost 1.0}]
                            :tag "a1_expect_1"
                            :features [{:name "a"}]})
           "1:1.0 a1_expect_1| a"))

    (is (= (format-example {:labels [{:value 1, :cost 2.0}
                                     {:value 2, :cost 1.0}]
                            :tag "ab1_expect_2"
                            :features [{:name "a"} 
                                       {:name "b"}]})
           "1:2.0 2:1.0 ab1_expect_2| a b")))

  (testing "Formatting of examples with light syntax"
    
    (is (= (format-example {:label {:value -1.231}
                   :features [{:name "f1"}
                              {:name "f2" :value 3.5}
                              {:name "f3" :namespace {:name "ns3"}}]})
           "-1.231 | f1 f2:3.5 |ns3 f3"))
    
    (is (= (format-example {:label -1.231
                   :features ["f1"
                              {:name "f2" :value 3.5}
                              {:name "f3" :namespace "ns3"}]})
           "-1.231 | f1 f2:3.5 |ns3 f3"))))

(deftest options-test

  (testing "set-option"
    (is (= (set-option :data "foo/bar.dat")
           {:options [[:data "foo/bar.dat"]]}))
    (is (= (set-option {} :data "foo/bar.dat")
           {:options [[:data "foo/bar.dat"]]}))
    (is (= (-> (set-option :data "foo/bar.dat")
               (set-option :save-resume true)
               (set-option :ngram 3)
               (set-option :quadratic "ab")
               (set-option :quadratic "ac")
               (set-option :learning-rate 0.3))
           {:options
            [[:data "foo/bar.dat"]
             [:save-resume true]
             [:ngram 3]
             [:quadratic "ab"]
             [:quadratic "ac"]
             [:learning-rate 0.3]]}))

    (testing "get-option"
      (let [settings (-> (set-option :data "foo/bar.dat")
                         (set-option :save-resume true)
                         (set-option :ngram 3)
                         (set-option :quadratic "ab")
                         (set-option :quadratic "ac")
                         (set-option :learning-rate 0.3))]
        (is (= (get-option settings :data)
               "foo/bar.dat"))
        (is (nil? (get-option (remove-option settings :data) :data)))
        (is (= (vw-command settings)
               "vw --data foo/bar.dat --save_resume --ngram 3 --quadratic ab --quadratic ac --learning_rate 0.3"))))))

(deftest example-test
  
  (testing "add-example"
    
    (is (= (add-example {}
                        {:labels [{:value 1, :cost 1.0}]
                         :tag "a1_expect_1"
                         :features ["a"]}
                        {:labels [{:value 2, :cost 1.0}]
                         :tag "b1_expect_2"
                         :features ["b"]})
           {:data
            [{:features ["a"],
              :labels [{:value 1, :cost 1.0}],
              :tag "a1_expect_1"}
             {:features ["b"],
              :labels [{:value 2, :cost 1.0}],
              :tag "b1_expect_2"}]}))))
