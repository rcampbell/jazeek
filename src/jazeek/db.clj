(ns jazeek.db
  (:require [clojure.contrib.sql :as sql]
            [clojureql.core :as cql]))

;; TODO:
;; - separate the DDL (c.c.sql) from the DML (cql)
;; - catch PK constraint violations, looping with new id

(def db
  {:classname   "org.h2.Driver"
   :subprotocol "h2"
   :user        "sa"
   :password    ""
   :subname     "~/jazeek"})

(cql/open-global db)

(defn init!
  "creates the schema"
  []
  (sql/with-connection db
    (sql/create-table :blocks
                      [:id "varchar(6)" "PRIMARY KEY"]
                      [:text :text])))

(let [blocks (cql/table :blocks)]
  (letfn [(create-id!
           []
           (let [alphanumeric (map char (concat (range 48 58)    ; 0-9
                                                (range 65 91)    ; A-Z
                                                (range 97 123))) ; a-z
                 confusing #{\0 \O \o \1 \L \l \5 \S \s}
                 pool (remove confusing alphanumeric)]
             (->> pool shuffle (take 6) (apply str))))]
    (defn create-block! [text]
      (let [id (create-id!)]
        (cql/conj! blocks {:id id :text text})
        id)))
  (defn get-block [id]
    (cql/project (cql/select blocks (cql/where (= :id id))) [:text]))
  (defn update-block! [{:keys [id text]}]
    (cql/update-in! blocks (cql/where (= :id id)) {:text text}))
  (defn delete-block! [id]
    (cql/disj! blocks (cql/where (= :id id)))))

;;TODO: make this defn- ? Test will not be able to use it than... 
 
(defn clob-to-string [clob]
  "Turn a JdbcClob into a String"
  (with-open [rdr (java.io.BufferedReader. (.getCharacterStream clob))]
    (apply str (line-seq rdr))))

