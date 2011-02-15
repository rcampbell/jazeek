(ns jazeek.core
  (:use compojure.core)
  (:require [compojure.route :as route]
            [compojure.handler :as handler]))

(defroutes main-routes
  (GET "/" [] "It works")
  (route/resources "/")
  (route/not-found "Page not found"))

(def app
  (handler/site main-routes))
