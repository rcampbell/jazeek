;;; Run with (load "jazeek/init") to reset the database

(require
  '[clojure.contrib.sql :as sql]
  '[jazeek.db :as db])

(defn drop-schema
  "Drops schema..."
  []
  (do
    (-> (sql/drop-table :blocks) (try (catch Exception e)) with-out-str)
    (-> (sql/drop-table :auth_info) (try (catch Exception e)) with-out-str)
    (-> (sql/drop-table :account) (try (catch Exception e)) with-out-str)))

(def key-type "varchar(8)")

(defn create-schema
  "Creates schema..."
  []
  (do
    (sql/create-table :account
                      [:id key-type  "PRIMARY KEY"]
                      [:name :text]
                      [:email :text]
                      [:photo :text])

    (sql/create-table :blocks
                      [:id key-type "PRIMARY KEY"]
                      [:title :text]
                      [:text :text]
                      [:account_id key-type "references account(id)" ]
                      [:created :timestamp "NOT NULL" :default "now()"])
    
    (sql/create-table :auth_info
                      [:identity :text "PRIMARY KEY"]
                      [:provider :text]
                      [:name :text]
                      [:nickname :text]
                      [:email :text]
                      [:gender "varchar(1)"]
                      [:dob :text]
                      [:account_id key-type "references account(id)" ])
       ))

(sql/with-connection db/db
  (drop-schema)
  (create-schema))

