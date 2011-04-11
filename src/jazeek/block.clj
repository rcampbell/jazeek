(ns jazeek.block
  (:require [jazeek.db :as db]))

(defn create! [text account-id title]
  (db/create-block! text account-id title))

(defn one [id]
  (->> (db/get-block id) :text))

(defn update! [block]
  (db/update-block! block))

(defn delete! [id]
  (db/delete-block! id))

(defn all [account-id]
  (db/all-blocks account-id))
