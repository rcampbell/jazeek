(ns jazeek.db
  (:refer-clojure :exclude [compile distinct drop take sort conj! disj!])
  (:use clojureql.core))

;; TODO:

(def db
  {:classname   "org.h2.Driver"
   :subprotocol "h2"
   :user        "sa"
   :password    ""
   :subname     "~/jazeek"})

(open-global db)

(def *id-length* 6)

;; FIXME clob->str function is used in core.clj, but declared here.
;; db leaked to core.
(defn clob->str [clob]
  "Turn a JdbcClob into a String"
  (with-open [rdr (java.io.BufferedReader. (.getCharacterStream clob))]
    (apply str (line-seq rdr))))

(def
  ^{:arglists '([x])
    :doc "Return true if x is a java.sql.Clob" }
  clob? (fn clob? [x] (instance? java.sql.Clob x)))

(defn- conver-val
  [v]
  (if (clob? v) (clob->str v) v))

(defn- row->map
  "Converts all clobs to strings"
  [row]
  (into {} (for [[k v] row] [k (conver-val v)])))

(defn- id-generator
  "Returns an id generator function for table. Will search for duplicates in column id-col"
  [table id-col]
  (fn [] "id generator that checks for duplicates in table's id-col"
    (let [alphanumeric (map char (concat (range 48 58)    ; 0-9
                                         (range 65 91)    ; A-Z
                                         (range 97 123))) ; a-z
          confusing #{\0 \O \o \1 \L \l \5 \S \s}
          pool (remove confusing alphanumeric)]
      (loop [new-id (->> pool shuffle (take *id-length*) (apply str))]
        (if (zero? (count @(select table (where (= id-col new-id)))))
          new-id
          (recur (->> pool shuffle (take *id-length*) (apply str))))))))


(let [blocks (table :blocks)]
  ;; C
  (let [create-id! (id-generator blocks :id)]
    (defn create-block! [text account_id]
      (let [id (create-id!)]
          (conj! blocks {:id id :text text :account_id account_id})
          id)))
  ;; R
  (defn get-block [id]
    (project (select blocks (where (= :id id))) [:text]))
  ;; U
  (defn update-block! [{:keys [id text]}]
    (update-in! blocks (where (= :id id)) {:text text}))
  ;; D
  (defn delete-block! [id]
    (disj! blocks (where (= :id id))))

  ;; Find
  (defn list-blocks [account_id]
    (select blocks (where (= :account_id account_id))))

  (defn all-blocks
    []
    blocks))
  

(let [auth-info (table :auth_info)]
  ;; C
  (defn create-info!
    [identity account-id info]
    (conj! auth-info {:identity identity :account_id account-id :name "Test" :email "email-here" :gender "M"}) true)
  ;; R
  (defn get-info [identity]
    (let
        [result @(-> (select auth-info (where (= :identity identity)))
                    (project [:email :name :gender :account_id]))]
      (if (empty? result)
        nil
        (row->map (first result))))))


(let [account (table :account)]
  ;; C
  (let [create-id! (id-generator account :id)]
    (defn create-account! [name email]
      (let [id (create-id!)]
          (conj! account {:id id :name name :email email})
          id)))
  ;; R
  (defn get-account [id]
    (row->map (first
               @(-> (select account (where (= :id id)))
                    (project [:email :name]))))))

