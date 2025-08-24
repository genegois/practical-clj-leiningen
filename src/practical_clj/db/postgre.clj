(ns practical-clj.db.postgre
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [next.jdbc.result-set :as rs])
  (:import [java.time Instant Duration]
           [java.sql Timestamp]))

(def db-spec
  {:dbtype "postgresql"
   :host "localhost"
   :port 5432
   :dbname "practical_clj"
   :user "postgres" ;; default gini, tapi kalo lo beda tinggal diganti
   :password "kotak"}) ;; ganti kotak sesuai settingan postgresql lo
(def datasource 
  (jdbc/with-options 
    (jdbc/get-datasource db-spec)
    {:builder-fn rs/as-unqualified-lower-maps}))

;; --------------------- CRUD USERS ---------------------
(defn create-user! [email password name]
  (jdbc/execute! datasource ["INSERT INTO users (email, password, name) VALUES (?, ?, ?)"
                             email password name]))

(defn find-user [email]
  (first (jdbc/execute! datasource ["SELECT * FROM users WHERE email = ?" email])))

(defn delete-user! [email]
  (jdbc/execute! datasource ["DELETE FROM users WHERE email = ?" email]))

;; --------------------- TOKEN ---------------------
(defn create-refresh-token! [token email days-valid]
  (let [expires (-> (Instant/now) (.plus (Duration/ofDays days-valid)) (Timestamp/from))]
    (sql/insert! datasource :refresh_token {:token token :email email :expires_at expires})))

(defn find-refresh-token [token]
  (sql/query datasource ["SELECT * FROM refresh_token WHERE token = ? AND expires_at > now()" token]
             {:row-fn first}))

(defn delete-refresh-token! [token]
  (sql/delete! datasource :refresh_token {:token token}))







