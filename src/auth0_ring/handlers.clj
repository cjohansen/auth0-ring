(ns auth0-ring.handlers
  (:require [auth0-ring.core :refer [qualify-url get-logout-url http-only-cookie delete-cookie]]
            [clojure.string :as s])
  (:import [com.auth0 NonceFactory QueryParamUtils Auth0ClientImpl]))

(defn query-param [req p]
  (get (:query-params req) (name p)))

(defn parse-query-param [str param]
  (QueryParamUtils/parseFromQueryParams (or str "") (name param)))

(defn create-client [{:keys [client-id client-secret domain]}]
  (Auth0ClientImpl. client-id client-secret domain))

(defn matches-nonce [req]
  (let [state (query-param req :state)
        nonce (get-in req [:cookies "nonce" :value])]
    (if nonce
      (and state (= (parse-query-param state :nonce) nonce))
      true)))

(defn is-valid [req]
  (and (not (query-param req :error))
       (matches-nonce req)))

(defn redirect-uri [req redirect-path]
  (if (re-find #"^https?://" redirect-path)
    redirect-path
    (qualify-url req redirect-path)))

(defn get-url-path [url-str]
  (second (re-find #"(?:.+://[^/]+)?(.*)" url-str)))

(defn get-success-redirect [req config]
  (if-let [return-url (parse-query-param (query-param req :state) :returnUrl)]
    (qualify-url req (get-url-path return-url))
    (:success-redirect config)))

(defn create-callback-handler [config & [{:keys [on-authenticated]}]]
  (let [auth0-client (create-client config)
        callback-uri (or (:callback-uri config) "/callback")]
    (fn [req]
      (try
        (if (is-valid req)
          (let [tokens (.getTokens auth0-client
                                   (query-param req :code)
                                   (redirect-uri req (:success-redirect config)))
                user-profile (.getUserProfile auth0-client tokens)]
            (when (fn? on-authenticated)
              (on-authenticated user-profile tokens))
            {:status 302
             :headers {"Location" (get-success-redirect req config)}
             :cookies {"nonce" (delete-cookie req)
                       "id-token" (http-only-cookie req {:value (.getIdToken tokens)})
                       "access-token" (http-only-cookie req {:value (.getAccessToken tokens)})}})
          {:status 302 :headers {"Location" (:error-redirect config)}})
        (catch RuntimeException e
          (.printStackTrace e)
          {:status 302 :headers {"Location" (:error-redirect config)}})))))


(defn create-logout-callback-handler [config]
  (fn [req]
    {:status 302
     :cookies {"id-token" (delete-cookie req)
               "access-token" (delete-cookie req)}
     :headers {"Location" (:logout-redirect config)}}))

(defn get-nonce [req]
  (let [cookie (:value (get (:cookies req) "nonce"))]
    (if (s/blank? cookie)
      (NonceFactory/create)
      cookie)))

(defn wrap-login-handler [handler]
  (fn [req]
    (let [nonce (get-nonce req)]
      (assoc-in (handler (assoc req :nonce nonce))
                [:cookies "nonce"]
                (http-only-cookie req {:value nonce :max-age 600})))))

(defn create-logout-handler [config]
  (fn [req] {:status 302 :headers {"Location" (get-logout-url req config)}}))
