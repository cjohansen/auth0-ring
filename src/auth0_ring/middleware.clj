(ns auth0-ring.middleware
  (:require [auth0-ring.core :refer [delete-cookie]]
            [auth0-ring.jwt :refer [get-jwt-verifier verify-token]]
            [clojure.walk :refer [keywordize-keys]]))

(defn wrap-token-verification [handler config]
  (let [jwt-verifier (get-jwt-verifier config)]
    (fn [req]
      (let [id-token (get-in req [:cookies "id-token" :value])
            access-token (get-in req [:cookies "access-token" :value])]
        (if (and id-token access-token)
          (if-let [user (verify-token jwt-verifier id-token)]
            (handler (assoc req :user (keywordize-keys (into {} user))))
            (update-in (handler req) [:cookies] #(merge {"id-token" (delete-cookie req)
                                                         "access-token" (delete-cookie req)} %)))
          (handler req))))))
