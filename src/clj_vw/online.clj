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
           [java.net Socket InetSocketAddress]
           [java.util UUID]))

(defn- locking-send-and-recieve [client message]
  (read-string 
   (locking client
     (doto ^java.io.PrintWriter (:out client)
           (.println message)
           (.flush))     
     (.readLine ^java.io.BufferedReader (:in client)))))

;;; Public API
;;; ==========

(defn daemon
  "Start a vw daemon. Port (and any other options) can be set via vw options,
  e.g. (daemon (set-option :port 8003))."
  ([] (daemon {}))
  ([settings]
     (let [settings (maybe-set-option settings 
                                      :daemon true
                                      :pid-file (str "/tmp/.pid.vw." (java.util.UUID/randomUUID)))
           t (future (vw settings))]
       (Thread/sleep 500)
       (loop [i 0]
         (if-let [pid (try (clojure.string/trim (slurp (get-option settings :pid-file)))
                           (catch Exception e false))]
           (assoc settings :daemon {:thread t
                                    :host "localhost"
                                    :pid pid})
           (if (< i 5) 
             (do (Thread/sleep 500)
                 (recur (inc i)))
             (do (future-cancel t)
                 (throw (Exception. "Unable to launch vw daemon.")))))))))

(defn set-connection-timeout
  "Set connection timeout in milliseconds"
  ([settings timeout]
   (assoc-in settings [:client :timeout] timeout)))

(defn connect 
  "Connect to a vw daemon. 

   Host is determined as the value of either (get-in settings [:client :host]), (get-in
  settings [:daemon :host]) or \"localhost\", in this order.

  Port is determined as the value of either (get-in settings [:client :port]), (get-in
  settings [:daemon :port]), (get-option settings :port) or 26542 in this order.

  Example, to start a local daemon on port 8003 and connect to it, do:

      (-> (set-option :port 8003) 
          (daemon) 
          (connect)).
"
  [settings]
  (let [host (or (get-in settings [:client :host])
                 (get-in settings [:daemon :host]) 
                 "localhost")
        port (or (get-in settings [:client :port])
                 (get-in settings [:daemon :port])
                 (get-option settings :port) 
                 26542)
        socket-address (InetSocketAddress. (str host) (long port))
        socket (java.net.Socket.)
        timeout (or (get-in settings [:client :timeout]) 1000)]
    (.connect socket socket-address timeout)
    (-> settings
        (assoc-in [:client :in] (java.io.BufferedReader. (java.io.InputStreamReader. (.getInputStream socket))))
        (assoc-in [:client :out] (java.io.PrintWriter. (.getOutputStream socket) true))
        (assoc-in [:client :socket] socket)
        (assoc-in [:client :host] host)
        (assoc-in [:client :port] port))))

(defn train
  "Send examples to a vw daemon connection (as returned by connect) for training. Examples in the
  return settings are extended with a :prediction slot corresponding to vowpal wabbit's prediction
  before training."
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
  "Send examples to a vw daemon connection for prediction (without training). Predictions are put
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
                                            :label 1 :importance 0 :labels [])))})
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


