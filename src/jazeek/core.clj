(ns jazeek.core
  (:use compojure.core
        ring.adapter.jetty)
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]
            [jazeek.db :as db]))

(letfn [(redirect [status]
                  (fn [uri] {:status status
                            :headers {"Location" uri}}))]
  (def ^{:doc "moved permanently" :private true}
    moved-to  (redirect 301)))

(defn create-block! [text]
  (let [id (db/create-block! text)]
    (moved-to (str "/blocks/" id))))



(defn get-block [id]
  (let [clob (:text (first @(db/get-block id)))
        text (.getSubString clob 1 (.length clob))] ; TODO: replace w/reader
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

(defroutes main-routes
  (GET    "/"           []                 (response/resource "index.html"))
  (GET    "/blocks/"    []                 (list-blocks))
  (POST   "/blocks/"    [& {:keys [text]}] (create-block! text))
  (GET    "/blocks/:id" [id]               (get-block id))
  (PUT    "/blocks/:id" [& params]         (update-block! params))
  (DELETE "/blocks/:id" [id]               (db/delete-block! id))
  (route/not-found "Page not found"))

(def app
  (handler/site main-routes))

(def run-app []
  )

(defonce jetty (future (run-jetty (var app) {:port 3000})))

