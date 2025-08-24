(ns practical-clj.core
  (:require
   [reitit.ring :as ring]
   [reitit.coercion.spec]
   [reitit.ring.coercion :as coercion]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]
   [ring.adapter.jetty :as jetty]
   [muuntaja.core :as m]
   [practical-clj.handlers :as h]
   [practical-clj.middleware :as mw]))

;; app 
(def app
  (ring/ring-handler 
   (ring/router
    [["/sql"
      ["/register" {:middleware [(mw/make-rate-limiter {:limit 5 :window-ms 6000})]
                    :parameters {:body {:email string?
                                        :password string?
                                        :name string?}}
                    :post h/register-handler}]
      ["/login" {:middleware [(mw/make-rate-limiter {:limit 5 :window-ms 6000})]
                 :parameters {:body {:email string? :password string?}}
                 :post h/login-handler}]
      ["/refresh" {:parameters {:body {:refresh_token string?}}
                   :post h/refresh-handler}]
      ["/profile" {:middleware [mw/wrap-auth]
                   :get h/get-profile}]
      ["/delete-account" {:middleware [mw/wrap-auth]
                          :delete h/delete-account-handler}]
      ["/logout" {:parameters {:body {:refresh_token string?}}
                  :post h/logout-handler}]]
     ["/mongo"
      ["/orders"
       {:middleware [mw/wrap-auth]}
       ["" {:post {:parameters {:body {:product string? :price int? :method string? :status string?}}
                   :handler h/create-order-mongo}
            :get h/list-orders-mongo}]
       ["/:id/status" {:parameters {:path {:id string?} :body {:status string?}}
                       :patch h/update-order-status-mongo}]
       ["/:id" {:parameters {:path {:id string?}}
                :delete h/delete-order-mongo}]]
      ["/analytics"
       ["/total-spent" {:get h/total-spent-per-user-mongo}]
       ["/top-orders" {:get h/top-orders-mongo}]]]]
    {:data {:coercion reitit.coercion.spec/coercion
            :muuntaja m/instance
            :middleware [parameters/parameters-middleware
                         muuntaja/format-middleware
                         coercion/coerce-request-middleware
                         coercion/coerce-response-middleware
                         mw/custom-exception-middleware]}})
   (ring/routes 
    (ring/create-resource-handler {:path "/" :root "public"
                                   :index-files ["index.html"]})
    (ring/create-default-handler))))

;; server ala-ala
(defonce server (atom nil))
(defn start-server []
  (reset! server (jetty/run-jetty app {:port 3000 :join? false})))
(defn stop-server []
  (when @server (.stop @server) (reset! server nil)))