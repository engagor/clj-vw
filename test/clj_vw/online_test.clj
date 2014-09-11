;; Copyright (c) 2014 Engagor
;;
;; The use and distribution terms for this software are covered by the
;; BSD License (http://opensource.org/licenses/BSD-2-Clause)
;; which can be found in the file LICENSE at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clj-vw.online-test
  (:require [clojure.test :refer :all]
            [clj-vw.online :refer :all]
            [clj-vw.core :refer :all]))

(deftest online-test

  (let [settings (connect (vw-daemon))]

    (testing "train"
      (is (= {:prediction 0.0, :features ["a"], :label 1, :tag "ex1"}
             (first (:data (train (add-example settings {:label 1 :tag "ex1" :features ["a"]}))))))
      (is (= {:prediction 0.506846, :features ["a"], :label 1, :tag "ex1"}
             (first (:data (train (add-example settings {:label 1 :tag "ex1" :features ["a"]}))))))
      (is (= {:prediction 0.73842, :features ["a"], :label 1, :importance 0, :tag "ex1"}
             (first (:data (train (add-example settings {:label 1 :tag "ex1" :features ["a"], :importance 0})))))))

    (testing "predict"

      (is (= {:label 0.73842, :tag "ex1"}
             (first (:predictions (predict (add-example settings {:label 1 :tag "ex1" :features ["a"]})))))))
  
    ;; (save (set-option settings :final-regressor "/tmp/test.model"))
  
    (close settings)

    ))

