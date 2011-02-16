(ns jazeek.db
  (:require [clojure.contrib.sql :as sql]
            [clojureql.core :as cql]))

;; TODO
;; - separate the "migration" logic (c.c.sql) from the "runtime" (cql)
;; - catch PK constraint violations, looping with new id

(def db
  {:classname   "org.h2.Driver"
   :subprotocol "h2"
   :user        "sa"
   :password    ""
   :subname     "~/jazeek"})

(cql/open-global db)

(defn init! []
  (sql/with-connection db
    (sql/create-table :blocks
                      [:id "varchar(6)" "PRIMARY KEY"]
                      [:text :text])))

(defn- create-id! []
  (let [alphanumeric (map char (concat (range 48 58)    ; 0-9
                                       (range 65 91)    ; A-Z
                                       (range 97 123))) ; a-z
        confusing #{\0 \O \o \1 \L \l \5 \S \s}
        pool (remove confusing alphanumeric)]
    (comment "# permutations" (Math/pow (count pool) 6))
    (->> pool shuffle (take 6) (apply str))))

(let [blocks (cql/table :blocks)]
  (defn get-block [id]
    (cql/project (cql/select blocks (cql/where (= :id id))) [:text]))
  (defn create-block! [text]
    (let [id (create-id!)]
      (cql/conj! blocks {:id id :text text})
      id)))

 

