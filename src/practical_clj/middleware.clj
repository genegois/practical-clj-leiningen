(ns practical-clj.middleware
  (:require
   [practical-clj.auth :as auth]
   [reitit.ring.middleware.exception :as exception]
   [practical-clj.db.postgre :as db]
   [practical-clj.handlers :refer [error]]
   [clojure.core.cache :as cache]
   [ring.util.http-response :as response]))

;; custom middleware
(defn wrap-auth [handler & {:keys [allow-refresh]}]
  (fn [request]
    (let [auth-header (get-in request [:headers "authorization"])
          token (auth/bearer->token auth-header)
          claims (and token (auth/verify-token token))]
      (cond
        (nil? claims) (error 401 "unauthorized" "Invalid atau token hangus")
        (and (= "access" (:type claims)) (not allow-refresh)) (let [email (:sub claims)
                                                                    user (db/find-user email)]
                                                                (if user
                                                                  (handler (assoc request :identity {:email (:email user)
                                                                                                     :name (:name user)
                                                                                                     :jwt token}))
                                                                  (error 401 "unauthorized" "User tidak ditemukan")))
        (and allow-refresh (= "refresh" (:type claims))) (handler (assoc request :identity {:email (:sub claims) :jwt token}))
        :else (error 401 "unauthorized" "Token salah type")))))

(def custom-exception-middleware
  (exception/create-exception-middleware
   (merge exception/default-handlers
          {::exception/wrap (fn [_ _ _] {:status 400 :body {:status "error" :message "Input tidak valid"}})})))

;; anti brute-force
(defn inc-and-allowed? [store k limit]
  (let [new-count (atom 0)]
    (swap! store (fn [cache0]
                   (let [current (cache/lookup cache0 k 0)
                         updated (inc current)]
                     (reset! new-count updated)
                     (cache/miss cache0 k updated))))
    (<= @new-count limit)))

(defn make-rate-limiter [{:keys [limit window-ms] :or {limit 5, window-ms 6000}}]
  (let [store (atom (cache/ttl-cache-factory {} :ttl window-ms))]
    (fn [handler]
      (fn [req]
        (let [ip (or (get-in req [:headers "x-forwarded-for"]) (:remote-addr req) "unknown")
              k (str ip ":" (:uri req) ":" (name (:request-method req)))]
          (if (inc-and-allowed? store k limit)
            (handler req)
            (response/too-many-requests (error 429 "error" (str "Terlalu banyak request, coba " (quot window-ms 1000) " detik lagi boss")))))))))