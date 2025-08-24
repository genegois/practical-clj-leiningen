(ns practical-clj.frontend.core
  (:require [reagent.core :as r]
            [reagent.dom.client :as rdom]
            [ajax.core :as ajax]
            [practical-clj.frontend.views :as views]))

;; ---------------- state ----------------
(defonce app-state (r/atom {:token nil 
                            :refresh-token nil 
                            :profile nil
                            :error nil
                            :orders nil
                            :total-spent 0}))

(defonce root (rdom/create-root (js/document.getElementById "app")))
(def api-base "http://localhost:3000")
(defn auth-header []
  (when-let [t (:token @app-state)]
    {"Authorization" (str "Bearer " t)}))
(defn relog []
  (swap! app-state assoc :error nil))

;; ---------------- helpers / API calls ----------------
(defn register! [email password name]
  (relog)
  (ajax/POST (str api-base "/sql/register")
    {:params {:email email :password password :name name}
     :format :json
     :response-format :json
     :keywords? true
     :handler (fn [_] (js/alert "Register sukses, sok login!"))
     :error-handler #(swap! app-state assoc :error (str "Register gagal: " (or (get-in % [:response :message]) (get-in % [:response :body :message]))))}))

(defn login! [email password]
  (relog)
  (ajax/POST (str api-base "/sql/login")
    {:params {:email email :password password}
     :format :json
     :response-format :json
     :keywords? true
     :handler (fn [{:keys [access_token refresh_token]}]
                (swap! app-state assoc :token access_token :refresh-token refresh_token :error nil))
     :error-handler #(swap! app-state assoc :error (str "Login gagal: " (or (get-in % [:response :message]) (get-in % [:response :body :message]))))}))

(defn fetch-profile! []
  (relog)
  (ajax/GET (str api-base "/sql/profile")
    {:headers (auth-header)
     :response-format :json
     :keywords? true
     :handler (fn [resp]
                (swap! app-state assoc :profile resp))
     :error-handler #(swap! app-state assoc :error (str "Gagal ambil profile: " (or (get-in % [:response :message]) (get-in % [:response :body :message]))))}))

(defn logout! []
  (relog)
  (ajax/POST (str api-base "/sql/logout")
    {:params {:refresh_token (:refresh-token @app-state)}
     :format :json
     :response-format :json
     :keywords? true
     :handler (fn [_]
                (reset! app-state {:token nil :refresh-token nil :profile nil :orders nil :total-spent 0}))
     :error-handler #(swap! app-state assoc :error (str "Gagal logout: " (or (get-in % [:response :message]) (get-in % [:response :body :message]))))}))

(defn delete-account! []
  (relog)
  (ajax/DELETE (str api-base "/sql/delete-account")
    {:headers (auth-header)
     :params {:access_token {:refresh-token @app-state}}
     :format :json
     :response-format :json
     :keywords? true
     :handler (fn [_]
                (reset! app-state {:token nil :refresh-token nil :profile nil :orders nil :total-spent 0})
                (js/alert "Akun lo udah kehapus boyy"))
     :error-handler #(swap! app-state assoc :error (str "Gagal hapus akun: " (or (get-in % [:response :message]) (get-in % [:response :body :message]))))}))

;; ---------------- ORDERS MONGO ----------------
(defn load-order! []
  (relog)
  (ajax/GET (str api-base "/mongo/orders")
    {:headers (auth-header)
     :response-format :json
     :keywords? true
     :handler (fn [{:keys [orders total-spent]}]
                (let [email (get-in @app-state [:profile :email])
                      total (some #(when (= (:_id %) email) (:total %)) total-spent)]
                  (swap! app-state assoc :orders orders :total-spent (or total 0))
                  (fetch-profile!)))
     :error-handler #(js/alert (str "Gagal load orders: " (get-in % [:response :message])))}))

(defn create-order! [product price method]
  (relog)
  (ajax/POST (str api-base "/mongo/orders")
    {:headers (auth-header)
     :params {:product product :price (js/parseInt price) :method method :status "Belom bayar"}
     :format :json
     :response-format :json
     :keywords? true
     :handler (fn [_] 
                (js/alert "Order berhasil dibuat!")
                (load-order!))
     :error-handler #(js/alert (str "Gagal bikin order: " (get-in % [:response :message])))}))

(defn update-order-status! [id new-status]
  (relog)
  (ajax/PATCH (str api-base "/mongo/orders/" id "/status")
    {:headers (auth-header)
     :params {:status new-status}
     :format :json
     :response-format :json
     :keywords? true
     :handler (fn [_]
                (js/alert "Status pembayaran telah di-update!")
                (load-order!))
     :error-handler #(js/alert (str "Gagal update status order: " (or (get-in % [:response :message]) (get-in % [:response :body :message]))))}))

(defn delete-order! [id]
  (relog)
  (ajax/DELETE (str api-base "/mongo/orders/" id)
    {:headers (auth-header)
     :response-format :json
     :keywords? true
     :handler (fn [_] (load-order!))
     :error-handler #(js/alert (str "Gagal hapus order: "(or (get-in % [:response :message]) (get-in % [:response :body :message]))))}))

;; ---------------- App root ----------------
(defn app []
  (let [state app-state]
    [:div
     [:h1 "Practical Clojure (super mini demo)"]
     (when-let [e (:error @state)]
       [:div {:style {:color "crimson" :margin-bottom "8px"}} e])
     (if (:refresh-token @state)
       (if (some? (:orders @state))
         [:div {:style {:display "flex" :gap "24px"}}
          (let [p (:profile @state)]
            [:div
             [:h3 "Profile"]
             [:pre "Email lo: " (:email p)]
             [:pre "Nama lo: " (:name p)]
             [:p [views/create-order-form create-order!]
              [:button {:on-click load-order! :style {:margin-top "8px"}} "Refresh order"]]
             [views/orders-list (:orders @state) (:total-spent @state) update-order-status! delete-order!]
             [:p [:button {:on-click logout! :style {:margin-top "8px"}} "logout"]]])]
         [views/profile-view fetch-profile! load-order! logout! delete-account! state])
       [:div 
        [views/register-form register!]
        [views/login-form login!]])]))

(defn ^:export init []
  (rdom/render root [app]))

