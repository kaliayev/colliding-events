(ns colliding-events.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [clojure.edn :refer [read-string]]
            [clojure.spec.alpha :as spec]
            [clj-time
             [core :as time]
             [coerce :as coerce-time]
             [types :as time-type]]
            [cheshire.core :as json])
  (:import (org.joda.time DateTime)))

(extend-protocol cheshire.generate/JSONable
  org.joda.time.DateTime
  (to-json [dt gen]
    (cheshire.generate/write-string gen (str dt))))

(defn time-formatter
  [calendar]
  (letfn [(time-format-event [event] (-> event
                                         (update :start-time coerce-time/from-string)
                                         (update :end-time coerce-time/from-string)))]
    (mapv time-format-event calendar)))

;; Event has a start-time, end-time, time-zone,  description, event-name, location

(spec/def ::event-name string?)
(spec/def ::description string?)
(spec/def ::start-time time-type/date-time?)
(spec/def ::end-time time-type/date-time?)
(spec/def ::time-zone time-type/time-zone?)
(spec/def ::longitude number?)
(spec/def ::latitude number?)
(spec/def ::location (spec/map-of #{:latitude :longitude} number?))
(spec/def ::event (spec/keys :req-un [::start-time ::end-time]
                             :opt-un [::event-name ::description ::location ::time-zone]))

(spec/def ::calendar (spec/coll-of #(spec/valid? ::event %)))
(spec/def ::collisions (spec/coll-of #(spec/valid? ::calendar %)))

(defn collision?
  [event1 event2 & {:keys [allow-equality?] :or {allow-equality? true}}]
  (let [comparison (compare (:end-time event1) (:start-time event2))
        collision-fn (if allow-equality? pos? (complement neg?))]
    (collision-fn comparison)))

(defn find-collisions
  "Check that we have a valid calendar, find colliding events, return sequence of pairs of events"
  [calendar & {:keys [allow-equality?] :or {allow-equality? true}}]
  {:pre [(spec/valid? ::calendar calendar)]
   :post [(spec/valid? ::collisions %)]}
  (let [calendar (sort-by :start-time calendar)]
    (loop [event (first calendar)
           remaining-calendar (rest calendar)
           acc []]
      (if (seq remaining-calendar)
        (let [collisions (reduce (fn [event-collisions test-event]
                                   (if (collision? event test-event :allow-equality? allow-equality?)
                                     (conj event-collisions [event test-event])
                                     event-collisions))
                                 []
                                 remaining-calendar)]
          (recur (first remaining-calendar) (rest remaining-calendar) (concat acc collisions)))
        (into [] acc)))))

(defn handle-local-calendar
  "Use edn read-string for safer reading"
  [{:keys [file-name allow-equality] :as params}]
  (let [file-path (str "resources/" file-name ".edn")]
    (try
      (let [calendar (-> file-path
                         slurp
                         read-string
                         time-formatter)]
        {:status 200
         :body (json/generate-string (find-collisions calendar :allow-equality? (Boolean/valueOf allow-equality)))})
      (catch Exception e
        {:status 400
         :exception (ex-data e)}))))

(defn handle-request-calendar
  [{:keys [body params]}]
  (println "\n" body "\n")
  (try {:status 200
        :body (-> body
                  :calendar
                  time-formatter
                  (find-collisions :allow-equality? (Boolean/valueOf (:allow-equality params)))
                  json/generate-string)}
       (catch Exception e
         {:status 400
          :exception (ex-data e)})))

(defroutes app-routes
  (GET "/:file-name" {:keys [params]} (handle-local-calendar params))
  (POST "/sequence" req (handle-request-calendar req))
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      (wrap-json-body {:keywords? true})
      (wrap-json-response)
      (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))))
