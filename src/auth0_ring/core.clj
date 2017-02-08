(ns auth0-ring.core
  (:import [java.net URLEncoder]))

(defn urlencode [str]
  (URLEncoder/encode str "utf-8"))

(defn qualify-url [req path]
  (str (name (:scheme req)) "://" (get (:headers req) "host") path))

(defn get-logout-url [req config]
  (str "https://" (:domain config) "/v2/logout?"
       "client_id=" (:client-id config)
       (when-let [url (:logout-handler config)]
         (str "&returnTo=" (urlencode (qualify-url req url))))))

(defn http-only-cookie [req cookie]
  (merge {:http-only true
          :secure (= (:scheme req) :https)
          :path "/"} cookie))

(defn delete-cookie [req]
  (http-only-cookie req {:value "" :max-age 1}))
