(ns jazeek.db
  (:require [clojure.contrib.sql :as sql]
            [clojureql.core :as cql]))

(def db
  {:classname   "org.h2.Driver"
   :subprotocol "h2"
   :user        "sa"
   :password    ""
   :subname     "~/jazeek"})

(cql/open-global db)

(defn init! []
  (sql/with-connection db
    (sql/create-table :blocks [:text :text])))

