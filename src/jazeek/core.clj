(ns jazeek.core
  (:use compojure.core)
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]
            [ring.middleware.session :as session]
            [jazeek.db :as db]))

(letfn [(redirect [status]
                  (fn [uri] {:status status
                            :headers {"Location" uri}}))]
  (def ^{:doc "moved permanently" :private true}
    moved-to  (redirect 301)))

(defn create-block! [text]
  (let [id (db/create-block! text)]
    (moved-to (str "/blocks/" id))))

(defn- clob-to-string [clob]
  "Turn a JdbcClob into a String"
  (with-open [rdr (java.io.BufferedReader. (.getCharacterStream clob))]
    (apply str (line-seq rdr))))

(defn get-block [id]
  (let [clob (:text (first @(db/get-block id)))
        text (clob-to-string clob)]
    text))

(defn update-block! [params]
  (println ">>> " params)
  ;[{id :id, :as block}]
;  (db/update-block! block)
 ; (get-block id)
  )

(defn delete-block! [id]
  (db/delete-block! id)
  (moved-to "/"))

(defn list-blocks []
  "TODO")

(defn show-session
  [req]
  (str (type req)))

(defn wrap-auth-check [handler]
  (fn [{:keys [request-method uri] :as req}]
    (let [resp (handler req)]
      (println uri)
      resp)))

(defroutes main-routes
  (GET    "/"           []                 (response/resource "index.html"))
  (GET    "/blocks/"    []                 (list-blocks))
  (POST   "/blocks/"    [& {:keys [text]}] (create-block! text))
  (GET    "/blocks/:id" [id]               (get-block id))
  (PUT    "/blocks/:id" [& params]         (update-block! params))
  (DELETE "/blocks/:id" [id]               (db/delete-block! id))
  (GET    "/session/"   [& req]                 (show-session req))
  (route/not-found "Page not found"))

(def app
  (-> (handler/site main-routes)
      wrap-auth-check))
