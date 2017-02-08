(ns auth0-ring.dev
  (:require [auth0-ring.app :as app]
            [ring.adapter.jetty :refer [run-jetty]]))

(def server nil)

(defn start []
  (when-not server
    (def server (run-jetty #'app/handler {:port 3666 :join? false}))))

(defn stop []
  (when server
    (.stop server)
    (def server nil)))

(defn restart []
  (start)
  (stop))
