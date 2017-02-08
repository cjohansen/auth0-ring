# auth0-ring

Integrate with Auth0 from Clojure. This repo contains a middleware and some Ring
handlers that mostly mirror the functionality in
the [official Auth0 Servlet](https://github.com/auth0/auth0-servlet), with the
notable exception that this library implements Auth0 in a completely statelesss
manner (e.g. no sessions).

## Install

Add [auth0-ring "0.1.0"] to :dependencies in your project.clj.

## Prerequisites

The middleware and handlers assumes the existence of `:cookies` and
`:query-params` in the Ring request map, meaning you should make sure to enable
the corresponding middlewares:

```clj
(ns your.app
  (:require [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.params :refer [wrap-params]]))

(def handler (-> your-web-handler
                 wrap-params
                 wrap-cookies))
```

## Usage

The official
Auth0 [Java introduction](https://auth0.com/docs/quickstart/webapp/java)
provides much more details and background on how to use this code, the flow,
related specs and so on.

If you just want the distilled version of how to use this library in Clojure,
read on.

The library includes a middleware that will verify the user's JSON Web Token
(JWT), if any, and deserialize it into `(:user req)`. It does not provide a
middleware for requiring logged in users. It also provides handlers that
implement the authentication callback, add a CSRF token/nonce to the login form,
and implements logout from your app and Auth0.

### Configuration

In order to run the sample code in this repo (and Readme), you will need some
configuration:

```clj
{:domain "yourapp.datacenter.auth0.com"
 :issuer "https://yourapp.datacenter.auth0.com/"
 :client-id "client id"
 :signing-algorithm :hs256
 :client-secret "client secret"
 :scope "openid user_id name nickname email picture"
 :callback-path "/auth/callback"
 :error-redirect "/login"
 :success-redirect "/"
 :logout-handler "/auth/logout"
 :logout-redirect "/"}
```

To run the provided sample code, put this in `resources/config.edn`.

### The login page

We'll be using [Auth0's Lock](https://auth0.com/docs/libraries/lock) for logins.
Follow the link to learn how to customize it.

The login page pulls a few values from your configuration, as well as the
`:nonce` from the request. In order to make this available, wrap the handler in
`auth0-ring.handlers/wrap-login-handler`:

```clj
(def login
  (auth0-handlers/wrap-login-handler
   (fn [req]
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
</html>")})))
```

Then create your web handler:

```clj
(ns auth0-clj.app
  (:require [auth0-ring.handlers :as auth0]
            [auth0-ring.middleware :refer [wrap-token-verification]]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.params :refer [wrap-params]]))

;; config ...
;; login handler ...

(defn web-handler [req]
  (let [callback-handler (auth0/create-callback-handler config)
        logout-callback-handler (auth0/create-logout-callback-handler config)
        logout-handler (auth0/create-logout-handler config)]
    (case (:uri req)
      "/login" (login-handler req)
      "/auth/callback" (callback-handler req)
      "/auth/logout" (logout-callback-handler req)
      "/logout" (logout-handler req)
      "/favicon.ico" {:status 404})))

(def handler (-> #'web-handler
                 (wrap-token-verification config)
                 wrap-params
                 wrap-cookies))
```

After going through the login, you should now see `(:user req)` in your web
handlers.

## License

Copyright © 2017 Christian Johansen

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.


## License: BSD

Copyright © 2017 Christian Johansen. All rights reserved.

1. Redistribution and use in source and binary forms, with or without
   modification, are permitted provided that the following conditions are met:
2. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.
3. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

Neither the name of the copyright holder nor the names of its contributors may
be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
