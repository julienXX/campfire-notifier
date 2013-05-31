(ns campfire.core
  (:require
   [http.async.client :as c]
   [cheshire.core :refer :all :as j]
   [clj-growl.core :as g]
   [clojure.java.io :as io]
   [clojure.edn])
  (:use [clojure.java.shell :only [sh]]))


(def conf (clojure.edn/read-string (slurp "config.clj")))

(def organisation (:organisation conf))
(def token (:token conf))
(def pass "X")
(def room-ids (:room-ids conf))
(def notifier (:notifier conf))
(def stream-url "https://streaming.campfirenow.com")
(def campfire-api (str "https://" organisation ".campfirenow.com"))
(def regex (re-pattern (apply str (interpose "|" (:matches conf)))))
(def rooms-names (atom (hash-map)))

(def growl
  (g/make-growler "" "Campfire Notifier" ["Mention" true "New" true]))

(defn terminal-notifier [title message]
  (sh "terminal-notifier" "-message" message "-title" title))

(defn notify [type title message]
  (if (= notifier "growl")
    (growl type title message))
  (terminal-notifier title message))

(def client (c/create-client))

(defn get-room-name [room-id]
  (get @rooms-names room-id))

(defn match-text [text]
  (if (re-seq regex text)
    (notify "Mention" "Campfire Mention" text)
  (notify "New" "Campfire" text)))

(defn process-text [text]
  (if (not (nil? text))
    (match-text text)))

(defn parse-message [s]
  (let [message (j/parse-string s true)
        room (:room-id message)
        text (:body message)]
    (process-text text)))

(defn parse-room [s]
  (let [room-json (j/parse-string s true)]
    (get-in room-json [:room :name])))

(defn listen-stream [room-id]
  (let [uri (str stream-url "/room/" room-id "/live.json")]
    (doseq [campfire-str (c/string
                          (c/stream-seq client :get uri
                                        :auth {:user token :password pass :preemptive true}
                                        :timeout -1))]
      (parse-message campfire-str))))

(defn get-room-name-from-api [room-id]
  (let [response (c/GET client (str campfire-api "/room/" room-id ".json")
                        :auth {:user token :password pass :preemptive true})]
    (-> response
        c/await
        c/string)))

(defn store-room-name [room-id]
  (let [room-name (parse-room (get-room-name-from-api room-id))]
    (swap! rooms-names assoc room-id room-name)))

(defn connect-rooms [room-ids]
  (doseq [room-id room-ids]
    (println "Connecting room" room-id)
    (store-room-name room-id)
    (future (listen-stream room-id))))

(defn -main []
  (println "Starting Notifier with config:" conf)
  (connect-rooms room-ids))
