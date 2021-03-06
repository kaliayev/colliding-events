(defproject colliding-events "0.1.0-SNAPSHOT"
  :description "FInd colliding events in a sequence"
  :url "https://www.github.com/kaliayev/colliding-events"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [compojure "1.6.1"]
                 [ring/ring-json "0.4.0"]
                 [ring/ring-defaults "0.3.2"]
                 [cheshire "5.8.1"]
                 [clj-time "0.14.4"]]
  :plugins [[lein-ring "0.12.4"]]
  :ring {:handler colliding-events.handler/app}
  :profiles {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                                  [ring/ring-mock "0.3.2"]]}})
