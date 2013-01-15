(ns campfire.core
  (:require
   [http.async.client :as c]
   [cheshire.core :refer :all :as j]
   [clj-growl.core :as g]
   [clojure.java.io :as io])
  (:import [java.io PushbackReader]))

(def conf (with-open [r (io/reader "config.clj")]
            (read (PushbackReader. r))))

(def token (get conf :token))
(def pass "X")
(def room_ids (get conf :room_ids))
(def stream_url "https://streaming.campfirenow.com")
(def regex (re-pattern (apply str (interpose "|" (get conf :matches)))))

(def growl
  (g/make-growler "" "Campfire Notifier" ["Mention" true "New" true]))

(def client (c/create-client))

(defn match-text [text]
  (if (re-seq regex text)
    (growl "Mention" "Campfire Mention" text)
  (growl "New" "Campfire" text)))

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
