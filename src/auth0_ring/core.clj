(ns auth0-ring.core
  (:import [java.net URLEncoder]
           (java.text SimpleDateFormat)
           (java.util Calendar TimeZone)))

(def rfc822-formatter
  (doto
    (SimpleDateFormat. "EEE, dd MMM yyyy HH:mm:ss Z")
    (.setTimeZone (TimeZone/getTimeZone "GMT"))))

(defn max-age->expires
  "Return a valid 'expires' value (RFC822 string)"
  [max-age]
  (let [cal (doto
              (Calendar/getInstance)
              (.add Calendar/SECOND max-age))]
    (.format rfc822-formatter (.getTime cal))))

(defn urlencode [str]
  (URLEncoder/encode str "utf-8"))

(defn qualify-url [req path]
  (if (re-find #"^https?://" path)
    path
    (str (name (:scheme req)) "://" (get (:headers req) "host") path)))

(defn get-logout-url [req config]
  (str "https://" (:domain config) "/v2/logout?"
       "client_id=" (:client-id config)
       (when-let [url (:logout-handler config)]
         (str "&returnTo=" (urlencode (qualify-url req url))))))

(defn http-only-cookie [req cookie]
  (cond-> (merge {:http-only true
                  :secure (= (:scheme req) :https)
                  :path "/"} cookie)
          (number? (:max-age cookie))
          (assoc :expires (max-age->expires (:max-age cookie)))))

(defn delete-cookie [req]
  (http-only-cookie req {:value "" :max-age 1}))
