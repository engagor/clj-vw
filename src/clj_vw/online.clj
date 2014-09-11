;; Copyright (c) 2014 Engagor
;;
;; The use and distribution terms for this software are covered by the
;; BSD License (http://opensource.org/licenses/BSD-2-Clause)
;; which can be found in the file LICENSE at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Higher level helper functions for launching and connecting to a (local or remote)
vowpal wabbit running in daemon mode."}
  clj-vw.online
  (:require [clj-vw.core :refer :all]
            [clojure.java 
             [shell :refer [sh]] 
             [io :refer :all]])
  (:import [java.io Writer BufferedReader PrintWriter InputStreamReader]
           [java.net Socket]
           [java.util UUID]))

(defn- locking-send-and-recieve [client message]
  (read-string 
   (locking client
     (.println ^java.io.PrintWriter (:out client) message)
     (.readLine ^java.io.BufferedReader (:in client)))))

;;; Public API
;;; ==========

(defn vw-daemon
  "Start a vw daemon."
  ([] (vw-daemon {}))
  ([settings]
     (let [settings (maybe-set-option settings 
                                      :daemon true
                                      :pid-file (str "/tmp/.pid.vw." (java.util.UUID/randomUUID)))
           t (future (vw settings))]
       (Thread/sleep 500)
       (loop [try 0]
         (if-let [pid (try (clojure.string/trim (slurp (get-option settings :pid-file)))
                           (catch Exception e false))]
           (assoc settings :daemon {:thread t
                                    :host "localhost"
                                    :pid pid})
           (if (< try 5) 
             (do (Thread/sleep 500)
                 (recur (inc try)))
             (do (future-cancel t)
                 (throw (Exception. "Unable to launch vw daemon.")))))))))

(defn connect 
  "Connect to a vw daemon."
  [settings]
  (let [host (or (get-in settings [:client :host])
                 (get-in settings [:daemon :host]) 
                 "localhost")
        port (or (get-in settings [:client :port])
                 (get-in settings [:daemon :port])
                 (get-option settings :port) 
                 26542)
        socket (java.net.Socket. (str host) (long port))
        in (java.io.BufferedReader. (java.io.InputStreamReader. (.getInputStream socket)))
        out (java.io.PrintWriter. (.getOutputStream socket) true)]
    (-> settings
        (assoc-in [:client :in] in)
        (assoc-in [:client :out] out)
        (assoc-in [:client :socket] socket)
        (assoc-in [:client :host] host)
        (assoc-in [:client :port] port))))

(defn train
  "Send examples to a vw daemon for training. Examples are extended with a :prediction slot."
  [settings]  
  (assoc settings :data
         (doall 
          (map (fn [example]
                 (assoc example :prediction 
                        (locking-send-and-recieve 
                         (:client settings) 
                         (format-example example))))
               (:data settings)))))

(defn predict
  "Send examples to a vw daemon for prediction (without training). Predictions are put
  under :predictions in settings."
  [settings]  
  (assoc settings :predictions
         (doall 
          (map (fn [example]
                 {:tag (:tag example)
                  :label (locking-send-and-recieve 
                          (:client settings) 
                          (format-example (assoc example 
                                            ;; :tag (or (:tag example) "dummy")
                                            :importance 0 :labels [])))})
               (:data settings)))))

(defn save 
  "Save daemon's model to (get-opt settings :final-regressor)."
  [settings]
  (assert-options settings :final-regressor)
  (.println ^java.io.PrintWriter (:out (:client settings))
            (str "save_" (get-option settings :final-regressor)))
  settings)

(defn close
  "Close daemon and/or client and cleanup."
  [settings]
  (when-let [client (:client settings)]
    (.close ^java.io.BufferedReader (:in client))
    (.close ^java.io.PrintWriter (:out client))
    (.close ^java.net.Socket (:socket client)))
  (when-let [daemon (:daemon settings)]
    (sh "kill" (:pid daemon))
    (when (and (get-option settings :pid-file)
               (.exists (as-file (get-option settings :pid-file))))
      (.delete (as-file (get-option settings :pid-file)))))
  (-> (dissoc settings :client :daemon)
      (remove-option :daemon :pid-file)))


