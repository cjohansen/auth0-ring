(ns auth0-ring.jwt
  (:require [clojure.java.io :as io])
  (:import [com.auth0.jwt JWTVerifier]
           [com.auth0.jwt.pem PemReader]))

(defmulti get-jwt-verifier :signing-algorithm)

(defmethod get-jwt-verifier :hs256 [{:keys [client-secret client-id issuer]}]
  (JWTVerifier. client-secret client-id issuer))

(defmethod get-jwt-verifier :rs256 [{:keys [public-key-path client-id issuer]}]
  (-> public-key-path
      io/resource
      io/file
      PemReader/readPublicKey
      (JWTVerifier. client-id issuer)))

(defn verify-token [jwt-verifier token]
  (try
    (.verify jwt-verifier token)
    (catch Exception e nil)))
