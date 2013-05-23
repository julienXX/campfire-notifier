(ns campfire.core
  (:require
   [http.async.client :as c]
   [cheshire.core :refer :all :as j]
   [clj-growl.core :as g]
   [clojure.java.io :as io])
  (:import [java.io PushbackReader])
  (:use [clojure.java.shell :only [sh]]))

(def conf (with-open [r (io/reader "config.clj")]
            (read (PushbackReader. r))))

(def organisation (get conf :organisation))
(def token (get conf :token))
(def pass "X")
(def room_ids (get conf :room_ids))
(def notifier (get conf :notifier))
(def stream_url "https://streaming.campfirenow.com")
(def users_url (str "https://" organisation ".campfirenow.com"))
(def regex (re-pattern (apply str (interpose "|" (get conf :matches)))))

(def growl
  (g/make-growler "" "Campfire Notifier" ["Mention" true "New" true]))

(defn terminal-notifier [title message]
  (sh "terminal-notifier" "-message" message "-title" title))

(defn notify [type title message]
  (if (= notifier "growl")
    (growl type title message))
  (terminal-notifier title message))

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
  (println "Hello World!")
  (println "Starting Notifier with config:" conf)
  (connect-rooms room_ids))
