(ns auth0-ring.app
  (:require [auth0-ring.handlers :as auth0]
            [auth0-ring.middleware :refer [wrap-token-verification]]
            [clojure.java.io :as io]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :refer [redirect]]))

(def config (read-string (slurp (io/resource "config.edn"))))

(defn login [req]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (str "<!DOCTYPE html>
<html>
  <head>
    <title>Login</title>
  </head>
  <body>
    <script src='https://cdn.auth0.com/js/lock/10.9.1/lock.min.js'></script>
    <script>var lock = new Auth0Lock(
'" (:client-id config) "',
'" (:domain config) "', {
  auth: {
    params: {
      scope: '" (:scope config) "',
      state: 'nonce=" (:nonce req) "&returnUrl=" (get-in req [:query-params "returnUrl"]) "'
    },
    responseType: 'code',
    redirectUrl: window.location.origin + '" (:callback-path config) "'
  }
});

lock.show();</script>
  </body>
</html>")})

(defn some-page [req]
  (if-let [user (:user req)]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (str "<!DOCTYPE html>
<html>
  <head>
    <title>This is some page</title>
  </head>
  <body>
    <h1>You need to be logged in to see this</h1>
    <p><a href=\"/\">Frontpage please</a></p>
  </body>
</html>")}
    (redirect (str "/login?returnUrl=" (:uri req)))))

(defn index [req]
  (if-let [user (:user req)]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (str "<!DOCTYPE html>
<html>
  <head>
    <title>Hello</title>
  </head>
  <body>
    <h1>Welcome dear user!</h1>
    <p>Nice to see you, " (:nickname user) "</p>
    <p><a href=\"/logout\">Log out</a></p>
  </body>
</html>")}
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (str "<!DOCTYPE html>
<html>
  <head>
    <title>Hello</title>
  </head>
  <body>
    <h1>You ain't logged in</h1>
    <p><a href=\"/login\">Log in</a></p>
  </body>
</html>")}))

(defn web-handler [req]
  (let [callback-handler (auth0/create-callback-handler config)
        logout-callback-handler (auth0/create-logout-callback-handler config)
        logout-handler (auth0/create-logout-handler config)
        login-handler (auth0/wrap-login-handler login)]
    (case (:uri req)
      "/" (index req)
      "/login" (login-handler req)
      "/auth/callback" (callback-handler req)
      "/auth/logout" (logout-callback-handler req)
      "/logout" (logout-handler req)
      "/some/page" (some-page req)
      "/favicon.ico" {:status 404})))

(def handler (-> #'web-handler
                 (wrap-resource "public")
                 wrap-content-type
                 wrap-not-modified
                 (wrap-token-verification config)
                 wrap-params
                 wrap-cookies))
