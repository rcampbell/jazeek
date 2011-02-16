(ns jazeek.core
  (:use compojure.core)
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]
            [jazeek.db :as db]))

(defn create-block! [text]
  (let [id (db/create-block! text)]
    {:status 301
     :headers {"Location" (str "/blocks/" id)}}))

(defroutes main-routes
  (GET  "/"           []                 (response/resource "index.html"))
  (GET  "/blocks/:id" [id]               (db/get-block id))
  (POST "/blocks"     [& {:keys [text]}] (create-block! text))
  (route/not-found "Page not found"))

(def app
  (handler/site main-routes))
