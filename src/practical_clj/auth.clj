(ns practical-clj.auth
  (:require [buddy.sign.jwt :as jwt]
            [clojure.string :as str]))

(def ^:private jwt-secret (or (System/getenv "JWT_SECRET") "dev-secret-please-change"))

(def access-ttl-seconds (* 15 60))
(def refresh-ttl-seconds (* 7 24 3600))
(defn now [] (quot (System/currentTimeMillis) 1000))

(defn gen-access-token [{:keys [email name]}]
  (jwt/sign {:sub email
             :name name
             :type "access"
             :iat (now)
             :exp (+ (now) access-ttl-seconds)}
            jwt-secret))

(defn gen-refresh-token [{:keys [email]}]
  (jwt/sign {:sub email
             :type "refresh"
             :iat (now)
             :exp (+ (now) refresh-ttl-seconds)} 
            jwt-secret))

(defn verify-token [token]
  (try 
    (jwt/unsign token jwt-secret)
    (catch Exception _e nil)))

(defn bearer->token [auth-header]
  (some-> auth-header
          (str/replace #"^[Bb]earer " "")
          str/trim))