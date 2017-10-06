(defproject auth0-ring "0.4.4-SNAPSHOT"
  :description "Auth0 integration from Clojure"
  :url "http://github.com/cjohansen/auth0-ring"
  :license {:name "BSD-3-Clause"
            :url "http://opensource.org/licenses/BSD-3-Clause"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.auth0/mvc-auth-commons "0.1.2"]]
  :profiles {:dev {:dependencies [[ring "1.5.0"]]
                   :resource-paths ["resources-dev"]}})
