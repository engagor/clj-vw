;; Copyright (c) 2014 Engagor
;;
;; The use and distribution terms for this software are covered by the
;; BSD License (http://opensource.org/licenses/BSD-2-Clause)
;; which can be found in the file LICENSE at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Higher level helper functions for interfacing to a local vowpal wabbit installation."}
  clj-vw.offline
  (:require [clj-vw.core :refer :all]
            [clojure.java.io :refer (as-file)])
  (:import [java.util UUID]))

(defn- maybe-write-data-file 
  "Like write-data-file but only executes if (get-option settings :data) doesn't already exist and if (:data settings) is non-empty. In that case, the settings data is written to a new file, which is pushed onto ::tmp-files."
  ([settings]
     (maybe-write-data-file settings {}))
  ([settings writer-settings]
     (if-not (get-option settings :data)
       (if (empty? (:data settings))
         (throw (Exception. "Unset :data file and empty examples."))
         (let [tmp-file (str "/tmp/.vw-temp-data." (java.util.UUID/randomUUID))]
           (println "temporarily writing data to" tmp-file)
           (-> settings
               (set-option :data tmp-file)
               (write-data-file)
               (update-in [::tmp-files] conj tmp-file))))
       (if-not (.exists (clojure.java.io/as-file (get-option settings :data)))
         (do (when (empty? (:data settings))
               (throw (Exception. "Non-existing :data-file and empty examples.")))
             (println "writing data to" (get-option settings :data))
             (write-data-file settings))
         settings))))

(defn- maybe-set-predictions-file [settings]
  (if (get-option settings :predictions)
    settings
    (let [tmp-file (str "/tmp/.vw-temp-predictions." (java.util.UUID/randomUUID))]
      (println "temporarily writing predictions to" tmp-file)
      (-> settings
          (set-option :predictions tmp-file)
          (update-in [::tmp-files] conj tmp-file)))))

(defn- cleanup-tmp-files [settings]
  (doseq [f (::tmp-files settings)]
    (when (.exists (as-file f))
      (println "removing temporary file" f)
      (.delete (as-file f))))
  (dissoc settings ::tmp-files))

;;; Public API
;;; ==========

(defn train 
  "Train a vowpal wabbit model from a data file (as specified by (get-option settings :data)) or from a
  collection of in memory examples (as specified by (:data settings))."
  [settings]
  (-> settings
      (maybe-write-data-file)
      (vw)
      (cleanup-tmp-files)
      (assoc :options (:options settings))))

(defn predict 
  "Use an existing vowpal wabbit model (as specified by (get-option
  settings :initial-regressor))or (get-option settings :final-regressor), in that order) to compute
  predictions for examples in a data file (as specified by (get-option settings :data)) or in
  memory (as spefified by (:data settings))."
  [settings]
  (-> settings
      (maybe-write-data-file)
      (maybe-set-predictions-file)
      (maybe-set-option :test-only true)
      (maybe-set-option :initial-regressor (get-option settings :final-regressor))
      (vw)
      (read-predictions)
      (cleanup-tmp-files)
      (assoc :options (:options settings))))
