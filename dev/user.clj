(ns user
  (:require [practical-clj.core :as core]
            [clj-http.client :as http]
            [cheshire.core :as json]
            [practical-clj.db.postgre :as db]
            [practical-clj.db.mongo :as mdb]
            [next.jdbc :as jdbc]
            [clojure.tools.namespace.repl :refer [refresh clear]]))

;; --------------------- setup base ---------------------
(def base-url-sql "http://localhost:3000/sql")
(def base-url-mongo "http://localhost:3000/mongo")
(def default-opts
  {:content-type :json
   :accept :json
   :as :json
   :throw-exceptions false})
(defn merge-opts [opts]
  (merge default-opts opts))

;; (require 'user :reload-all) 

;; --------------------- Server control ---------------------
(defn start []
  (core/start-server)
  (println "Server started ..."))
(defn stop [] (core/stop-server)
  (println "Server stopped!!"))
(defn reset-database []
  (jdbc/execute! db/datasource ["DELETE FROM users"])
  (jdbc/execute! db/datasource ["DELETE FROM refresh_token"])
  (jdbc/execute! db/datasource ["ALTER SEQUENCE users_id_seq RESTART WITH 1"])
  (mdb/reset-all!)
  (mdb/ensure-orders-index!)
  (println "Database telah direset"))

(defn reset
  "ini tuh buat hotrepl reset semua kode lo misal kalo lo ada perubahan di kode di file ini dan pengen langsung ngetes di repl"
  []
  (stop)
  (clear)
  (refresh :after 'user/start))

(defn reset-double []
  (reset-database)
  (reset)
  (println "reset double"))

(defn reset-double-mongo []
  (mdb/reset-all!)
  (reset)
  (mdb/ensure-orders-index!))

;; --------------------- SQL API Helpers ---------------------
(defn register [email password name]
  (http/post (str base-url-sql "/register")
             (merge-opts
              {:body (json/generate-string {:email email
                                            :password password
                                            :name name})})))

(defn login [email password]
  (http/post (str base-url-sql "/login")
             (merge-opts
              {:body (json/generate-string {:email email
                                            :password password})})))

(defn profile [token]
  (http/get (str base-url-sql "/profile")
            (merge-opts
             {:headers {"Authorization" (str "Bearer " token)}})))

(defn delete-account [token]
  (http/delete (str base-url-sql "/delete-account")
               (merge-opts
                {:headers {"Authorization" (str "Bearer " token)}})))

(defn logout [refresh-token]
  (http/post (str base-url-sql "/logout")
             (merge-opts
              {:body (json/generate-string {:refresh_token refresh-token})})))

(defn refresh-token [refresh-token]
  (http/post (str base-url-sql "/refresh")
             (merge-opts
              {:body (json/generate-string {:refresh_token refresh-token})})))

;; --------------------- MONGO API HELPERS --------------------- 
(defn orders-mongo [token product price method status]
  (http/post (str base-url-mongo "/orders")
             (merge-opts
              {:headers {"Authorization" (str "Bearer " token)}
               :body (json/generate-string {:product product
                                            :price price
                                            :method method
                                            :status status})})))

(defn list-orders-mongo [token]
  (http/get (str base-url-mongo "/orders")
            (merge-opts
             {:headers {"Authorization" (str "Bearer " token)}})))

(defn update-order-status [token status]
  (let [order-id (get-in (list-orders-mongo token) [:body :orders 0 :_id])]
    (http/patch (str base-url-mongo "/orders/" order-id "/status")
                (merge-opts
                 {:headers {"Authorization" (str "Bearer " token)}
                  :body (json/generate-string {:status status})}))))

(defn total-spent-user [token]
  (http/get (str base-url-mongo "/analytics/total-spent")
            (merge-opts
             {:headers {"Authorization" (str "Bearer " token)}})))

(defn delete-order [token]
  (let [order-id (get-in (list-orders-mongo token) [:body :orders 0 :_id])]
    (http/delete (str base-url-mongo "/orders/" order-id)
                 (merge-opts {:headers {"Authorization" (str "Bearer " token)}}))))

(comment
  (start)
  (stop)
  (reset)
  ;; (require 'user :reload-all) 
  (reset-database)
  (reset-double)
  (reset-double-mongo)
  ;; =================== once-and-for-all SQL ============================
  (do
    (register "sql@test.com" "pass123" "SQL test")
    (def login-res (login "sql@test.com" "pass123"))
    (def access (get-in login-res [:body :access_token]))
    (def refresh-jwt (get-in login-res [:body :refresh_token]))
    (profile access)
    (logout access)
    (delete-account access)
    (profile access))
  ;; =================== once-and-for-all MONGO ===================  

  (do
    (register "jwt@test.io" "pass123" "JWT USER")
    (def login-res (login "jwt@test.io" "pass123"))
    (def access (get-in login-res [:body :access_token]))
    (def refresh-jwt (get-in login-res [:body :refresh_token]))
    (orders-mongo access "Keyborad" 200000 "cash" "unpaid")
    (orders-mongo access "Mouse" 100000 "cash" "unpaid")
    (list-orders-mongo access)
    (total-spent-user access)
    (update-order-status access "paid")
    (delete-order access)
    (delete-account access))

  ;; tambahan ajh buat ngetes refresh token
  (register "manual@test.com" "rahasia123" "Manual user")
  (def login-res (login "manual@test.com" "rahasia123"))
  (def access (get-in login-res [:body :access_token]))
  (def refresh-jwt (get-in login-res [:body :refresh_token]))
  (profile access)
  (def new-access (-> (refresh-token refresh-jwt) :body :access_token))
  (profile new-access)
  
  (mdb/top-orders)
  (mdb/total-spent-per-user))