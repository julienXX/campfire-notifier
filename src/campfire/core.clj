(ns campfire.core
  (:require
   [http.async.client :as c]
   [cheshire.core :refer :all :as j]
   [clj-growl.core :as g]
   [clojure.java.io :as io]
   [clojure.edn]
   [clojure.string :as str])
  (:use
   [campfire.core]
   [clojure.java.shell :only [sh]])
  (:gen-class))


(def conf (clojure.edn/read-string (slurp "config.clj")))

(def organisation (:organisation conf))
(def token (:token conf))
(def pass "X")
(def room_ids (:room_ids conf))
(def notifier (:notifier conf))
(def stream_url "https://streaming.campfirenow.com")
(def users_url (str "https://" organisation ".campfirenow.com"))
(def regex (re-pattern (apply str (interpose "|" (:matches conf)))))

(def growl
  (g/make-growler "" "Campfire Notifier" ["Mention" true "New" true]))

(defn escape [message]
  (str/replace message "[" "\\["))

(defn terminal-notifier [title message]
  (sh "terminal-notifier" "-message" (escape message) "-title" title))

(defn notify [type title message]
  (cond
   (= notifier "growl") (growl type title message)
   (= notifier "terminal-notifier") (terminal-notifier title message)))

(def client (c/create-client))

(defn match-text [text]
  (if (re-seq regex text)
    (notify "Mention" "Campfire Mention" text)
  (notify "New" "Campfire" text)))

(defn process-text [text]
  (if (not (nil? text))
    (match-text text)))

(defn parse-message [s]
  (let [message (j/parse-string s true)
        room (:room_id message)
        text (:body message)]
    (process-text text)))

(defn listen-stream [room_id]
  (let [uri (str stream_url "/room/" room_id "/live.json")]
    (doseq [campfire-str (c/string
                          (c/stream-seq client :get uri
                                        :auth {:user token :password pass :preemptive true}
                                        :timeout -1))]
      (parse-message campfire-str))))

(defn connect-rooms [room_ids]
  (doseq [room_id room_ids]
    (println "Connecting room" room_id)
    (future (listen-stream room_id))))

(defn -main []
  (println "Starting Notifier with config:" conf)
  (connect-rooms room_ids))
