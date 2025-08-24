(ns practical-clj.handlers
  (:require [practical-clj.db.postgre :as db]
            [practical-clj.db.mongo :as mdb]
            [practical-clj.auth :as auth]
            [buddy.hashers :as hashers]
            [clojure.string :as str]))

;; utils
(defn success [status status-detail msg & [data]]
  {:status status
   :body (merge {:status status-detail :message msg} (or data {}))})

(defn error [status status-detail msg]
  {:status status
   :body {:status status-detail :message msg}})

;; --------------------- POSTGRESQL Handlers ---------------------
(defn register-handler [{{{:keys [email password name]} :body} :parameters}]
  (if (db/find-user email)
    (error 400 "error" "Email lo udah kedaftar boss")
    (let [hashed (hashers/derive password)]
      (db/create-user! email hashed name)
      (success 201 "ok" (str "User " name " berhasil daftar!")))))

(defn login-handler [{{{:keys [email password]} :body} :parameters}]
  (let [user (db/find-user email)]
    (cond
      (nil? user) (error 404 "error" "Email lo nggak ada boss")
      (not (hashers/check password  (:password user))) (error 401 "unauthorized" "Password lo salah")
      :else
      (let [access (auth/gen-access-token {:email email :name (:name user)})
            refresh (auth/gen-refresh-token {:email email})]
        (db/create-refresh-token! refresh email 7)
        (success 200 "ok" "login sukses!" {:access_token access :refresh_token refresh})))))

(defn refresh-handler [{{{:keys [refresh_token]} :body} :parameters}]
  (let [claims (auth/verify-token refresh_token)
        token-row (db/find-refresh-token refresh_token)]
    (cond
      (nil? claims) (error 401 "unauthorized" "Refresh token invalid")
      (nil? token-row) (error 401 "unauthorized" "Refresh token tidak ditemukan")
      :else
      (let [email (:sub claims)
            user (db/find-user email)
            access (auth/gen-access-token {:email email :name (:name user)})]
        (success 200 "ok" "Access token baru" {:access_token access})))))

(defn get-profile [{:keys [identity]}]
  {:status 200 :body {:email (:email identity) :name (:name identity)}})

(defn delete-account-handler [{{:keys [email]} :identity}]
  (if (nil? (db/find-user email))
   (error 400 "error" "Email nggak ketemu cuyy")
   (do
     (db/delete-user! email)
     (mdb/delete-orders-by-email! email)
     (success 200 "success" "Akun lo udah terhapus bro"))))

(defn logout-handler [{{{:keys [refresh_token]} :body} :parameters}]
  (db/delete-refresh-token! refresh_token)
  (success 200 "ok" "logout sukses!"))

;; --------------------- MONGO HANDLER ---------------------
(defn create-order-mongo
  [{{{:keys [product price method status]} :body} :parameters
    {:keys [email]} :identity}]
  (cond
    (str/blank? product) (error 400 "error" "Product lo masih kosong")
    (nil? price) (error 400 "error" "Harga lo masih kosong")
    :else
    (do
      (mdb/create-order! {:user_email email
                          :product product
                          :price price
                          :method method
                          :status status})
      (success 200 "ok" "Order berhasil dibuat!"))))

(defn list-orders-mongo [{:keys [identity]}]
  (let [orders (mdb/find-order-by-email (:email identity))
        total-spent (mdb/total-spent-per-user)]
    (success 200 "ok" "Orders diambil" {:orders orders :total-spent total-spent})))

(defn update-order-status-mongo [{{{:keys [status]} :body} :parameters
                                  {:keys [email]} :identity
                                  {:keys [id]} :path-params}]
  (if (str/blank? status)
    (error 400 "error" "Status kosong")
    (let [result (mdb/update-order-status! id email status)]
      (if (pos? (.getModifiedCount result))
        (success 200 "ok" "Status order berhasil di-update!")
        (error 404 "error" "Order tidak ditemukan atau bukan milikmu")))))

(defn total-spent-per-user-mongo [_]
  (success 200 "ok" "Total spent per user" {:data (mdb/total-spent-per-user)}))

(defn top-orders-mongo [_]
  (success 200 "ok" "Top orders" {:data (mdb/top-orders)}))

(defn delete-order-mongo [{{:keys [id]} :path-params
                           {:keys [email]} :identity}]
  (let [result (mdb/delete-order! id email)]
    (if (pos? (.getDeletedCount result))
      (success 200 "ok" "Order berhasil dihapus")
      (error 404 "error" "Order tidak ditemukan atau bukan milikmu"))))