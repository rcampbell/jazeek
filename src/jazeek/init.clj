;;; Run with (load "jazeek/init") to reset the database

(require '[clojure.contrib.sql :as sql])

(defn drop-schema
  "Drops schema..."
  []
  (do
    (-> (sql/drop-table :blocks) (try (catch Exception e)) with-out-str)
    (-> (sql/drop-table :auth_info) (try (catch Exception e)) with-out-str)
    (-> (sql/drop-table :account) (try (catch Exception e)) with-out-str)))

(defn create-schema
  "Creates schema..."
  []
  (do
    (sql/create-table :account
                      [:id "varchar(6)" "PRIMARY KEY"]
                      [:name :text]
                      [:email :text]
                      [:photo :text])

    (sql/create-table :blocks
                      [:id "varchar(6)" "PRIMARY KEY"]
                      [:text :text]
                      [:account_id "varchar(6)" "references account(id)" ])
    
    (sql/create-table :auth_info
                      [:identity :text "PRIMARY KEY"]
                      [:provider :text]
                      [:name :text]
                      [:nickname :text]
                      [:email :text]
                      [:gender "varchar(1)"]
                      [:dob :text]
                      [:account_id "varchar(6)" "references account(id)" ])
       ))

(sql/with-connection jazeek.db/db
  (drop-schema)
  (create-schema))

