(ns jazeek.core
  (:use compojure.core)
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]
            [ring.util.response :as  ring-response]
            [sandbar.stateful-session :as session]
            [sandbar.auth :as auth]
            [jazeek.db :as db]
            [jazeek.loginza :as loginza]))


;; TODO this can be replaced with ring-response/redirect + chaning :status
(letfn [(redirect [status]
                  (fn [uri] {:status status
                            :headers {"Location" uri}}))]
  (def ^{:doc "moved permanently" :private true}
    moved-to  (redirect 301)))

(defn create-block! [text]
  (let [id (db/create-block! text)]
    (moved-to (str "/blocks/" id))))

(defn- clob-to-string [clob]
  "Turn a JdbcClob into a String"
  (with-open [rdr (java.io.BufferedReader. (.getCharacterStream clob))]
    (apply str (line-seq rdr))))

(defn get-block [id]
  (let [clob (:text (first @(db/get-block id)))
        text (clob-to-string clob)]
    text))

(defn update-block! [params]
  (println ">>> " params)
  ;[{id :id, :as block}]
;  (db/update-block! block)
 ; (get-block id)
  )

(defn delete-block! [id]
  (db/delete-block! id)
  (moved-to "/"))

(defn list-blocks []
  "TODO")

(def security-policy
  [#"/blocks.*" [:user :nossl]
   #"/login.*"  [:any :nossl]
   #".*"        [:any :nossl]])

(defroutes main-routes
  (GET    "/"           []                 (response/resource "index.html"))
  (GET    "/blocks/"    []                 (list-blocks))
  (POST   "/blocks/"    [& {:keys [text]}] (create-block! text))
  (GET    "/blocks/:id" [id]               (get-block id))
  (PUT    "/blocks/:id" [& params]         (update-block! params))
  (DELETE "/blocks/:id" [id]               (db/delete-block! id))
  (GET    "/login"      []                 (response/resource "login.html"))
  (POST   "/auth_callback" [& params]      (loginza/check-auth params))
;  (GET    "/logout")    []                 ()
  (route/not-found "Page not found"))

(def app
  (-> (handler/site main-routes)
      (auth/with-security security-policy loginza/loginza-authenticator)
      session/wrap-stateful-session))
