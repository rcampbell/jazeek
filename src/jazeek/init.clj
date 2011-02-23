;;; Run with (load "jazeek/init") to reset the database

(require '[clojure.contrib.sql :as sql])

(sql/with-connection jazeek.db/db
  (-> (sql/drop-table :blocks) (try (catch Exception e)) with-out-str)
  (sql/create-table :blocks
                    [:id "varchar(6)" "PRIMARY KEY"]
                    [:text :text]))
