(ns jazeek.core
  (:use compojure.core
        net.cgrand.enlive-html
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

(defn- clob->str [block]
  (let [clob (:text block)]
    (.getSubString clob 1 (.length clob)))) ; TODO: replace w/reader

(defn create-block! [text]
  (let [id (db/create-block! text)]
    (moved-to (str "/blocks/" id))))

(deftemplate editor "index.html" [id text]
  [:form]     (do-> (set-attr :action (str "/blocks/" id))
                    (prepend {:tag :input :attrs {:type "hidden"
                                                  :name "_method"
                                                  :value "PUT"}}))
  [:textarea] (content text))

(defn get-block [id]
  (editor id (clob->str (first @(db/get-block id)))))

(defn update-block! [{id :id, :as block}]
  (db/update-block! block)
  (get-block id))

(defn delete-block! [id]
  (db/delete-block! id)
  (moved-to "/"))

(defn list-blocks []
  (apply str (map clob->str @(db/list-blocks))))

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

(defonce *server* (future (run-jetty (var app) {:port 3000})))

