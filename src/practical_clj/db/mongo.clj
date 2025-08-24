(ns practical-clj.db.mongo
  (:import [com.mongodb.client MongoClients]
           [org.bson Document]
           [org.bson.types ObjectId]))

;; --------------------- connection ---------------------
(def uri "mongodb://localhost:27017")
(defonce client (MongoClients/create uri))
(defonce db (.getDatabase client "practical_clj"))
(defn coll ^com.mongodb.client.MongoCollection [name]
  (.getCollection db name))

;; --------------------- maintenance helpers ---------------------
(defn drop-collection! [name]
  (.drop (coll name)))

(defn reset-all! []
  (doseq [c ["orders"]]
    (try (drop-collection! c) (catch Exception _ nil))))

;; --------------------- indexes ---------------------
(defn ensure-orders-index! []
  (.createIndex (coll "orders") (Document. {"user_email" 1}))
  (.createIndex (coll "orders") (Document. {"price" 1})))

;; --------------------- doc <-> clj helpers ---------------------
(defn ->doc ^Document [m]
  (letfn [(coerce [v]
                  (cond
                    (map? v) (->doc v)
                    (sequential? v) (mapv coerce v)
                    :else v))]
    (Document. (into {} (for [[k v] m] [(name k) (coerce v)])))))

(defn doc->clj [^Document d]
  (letfn [(coerce [v]
                  (cond
                    (instance? Document v) (doc->clj v)
                    (instance? java.util.List v) (mapv coerce v)
                    (instance? ObjectId v) (str v)
                    :else v))]
    (into {} (for [[k v] d] [(keyword k) (coerce v)]))))

;; --------------------- orders ---------------------
(defn create-order! [order]
  (.insertOne (coll "orders") (->doc order)))

(defn find-order-by-email [email]
  (map doc->clj
       (iterator-seq
        (.iterator 
         (.find (coll "orders") (->doc {:user_email email}))))))

(defn update-order-status! [id email new-status]
  (.updateOne (coll "orders")
              (->doc {"_id" (ObjectId. id) "user_email" email})
              (->doc {"$set" {:status new-status}})))

(defn delete-order! [id email]
  (.deleteOne (coll "orders") (->doc {"_id" (ObjectId. id) "user_email" email})))

(defn delete-orders-by-email! [email]
  (.deleteMany (coll "orders") (->doc {:user_email email})))

(defn total-spent-per-user []
  (vec
   (.aggregate (coll "orders") [(->doc {"$group" {"_id" "$user_email" "total" {"$sum" "$price"}}})])))

(defn top-orders []
  (vec
   (.aggregate (coll "orders") [(->doc {"$sort" {"price" -1}})
                                (->doc {"$limit" 5})])))

