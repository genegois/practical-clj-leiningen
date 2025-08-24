(ns practical-clj.frontend.views
  (:require [reagent.core :as r]))

(defn login-form [login!]
  (let [email (r/atom "")
        pass (r/atom "")]
    (fn []
      [:div
       [:h3 "Login"]
       [:div
        [:input {:type "text"
                 :placeholder "Email lo"
                 :value @email
                 :on-change #(reset! email (.. % -target -value))}]]
       [:div
        [:input {:type "password"
                 :placeholder "Password lo"
                 :value @pass
                 :on-change #(reset! pass (.. % -target -value))}]]
       [:div {:style {:margin-top "8px"}}
        [:button {:on-click #(login! @email @pass)} "Login"]]])))

(defn register-form [register!]
  (let [email (r/atom "")
        pass (r/atom "")
        name (r/atom "")]
    (fn []
      [:div
       [:h3 "Register"]
       [:div
        [:input {:type "text"
                 :placeholder "Email lo"
                 :value @email
                 :on-change #(reset! email (.. % -target -value))}]]
       [:div
        [:input {:type "password"
                 :placeholder "Password lo"
                 :value @pass
                 :on-change #(reset! pass (.. % -target -value))}]]
       [:div 
        [:input {:type "text"
                 :placeholder "Nama lo"
                 :value @name
                 :on-change #(reset! name (.. % -target -value))}]]
       [:div {:style {:margin-top "8px"}}
        [:button {:on-click #(register! @email @pass @name)} "Daftar"]]])))

(defn profile-view [fetch-profile! load-orders! logout! delete-account! state]
  (let [loading (r/atom false)]
    (fn []
      [:div
       [:div {:style {:display "flex" :gap "8px" :margin-bottom "8px"}}
        [:button {:on-click (fn []
                              (reset! loading true)
                              (fetch-profile!)
                              (js/setTimeout (fn [] (reset! loading false)) 700))}
         (if @loading "Bentar, sabar boyy" "Muat profile lo")]
        [:button {:on-click load-orders!} "Tampilkan order lo"]
        [:button {:on-click logout!} "Logout"]
        [:button {:on-click delete-account!} "Apus akun lo"]]
       (when-let [p (:profile @state)]
         [:div 
          [:h3 "Profile"]
          [:pre "Email lo: " (:email p)]
          [:pre "Nama lo: " (:name p)]])])))

;; MONGO
(defn create-order-form [create-order!]
  (let [product (r/atom "")
        price (r/atom "")
        method (r/atom "cash")]
    (fn []
      [:div
       [:h3 "Buat Order"]
       [:input {:type "text" 
                :placeholder "Produk" 
                :value @product
                :on-change #(reset! product (.. % -target -value))}]
       [:input {:type "number"
                :placeholder "Harga"
                :value @price
                :on-change #(reset! price (.. % -target -value))}]
       [:select {:value @method
                 :on-change #(reset! method (.. % -target -value))}
        [:option {:value "Cash"} "Cash"]
        [:option {:value "Transfer"} "Transfer"]]
       [:button {:on-click #(create-order! @product @price @method)} "Buat Order!"]])))

(defn orders-list [orders total-spent update-order-status! delete-order!]
  [:div
   [:h3 "Orders"]
   (if (empty? orders)
     [:p "Lo belom ada order"]
     [:ul
      (for [order orders]
        ^{:key (:id order)}
        [:li
         [:div "Produk: " (:product order)]
         [:div "Harga: " (:price order)]
         [:div "Metode pembayaran: " (:method order)]
         [:div "Status: " (:status order)]
         [:button {:on-click #(update-order-status! (:_id order) "Lo udah bayar")} "Bayar!"]
         [:button {:on-click #(delete-order! (:_id order))} "Hapus order"]])])
   [:div 
    [:h4 "Harga total: " total-spent]]]) 