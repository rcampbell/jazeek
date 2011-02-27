(ns jazeek.core
  (:use [clojure.string :only (join)]
        [net.cgrand.enlive-html :only (deftemplate do-> set-attr prepend content clone-for)]
        [compojure.core :only (GET POST PUT DELETE defroutes)]
        [ring.adapter.jetty :only (run-jetty)] )
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]
            [ring.util.response :as ring-response]
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

(deftemplate current-user-view "user/user.html" [name email]
  [:#name] (content name)
  [:#email] (content email))

(deftemplate edit-view "block-form.html" [id text]
  [:form]     (do-> (set-attr :action (str "/block/" id))
                    (prepend {:tag :input :attrs {:type "hidden"
                                                  :name "_method"
                                                  :value "PUT"}}))
  [:textarea] (content text))

(deftemplate list-view "list.html" [blocks name]
  [:#username] (content name)
  {[:dt] [:dd]} (clone-for [{:keys [id text]} blocks]
                           [:a]  (do-> (set-attr :href (str "/block/" id))
                                       (content id))
                           [:dd] (content (db/clob->str text))))
                           
(defn create-block! [text]
  (let [id (db/create-block! text (current-account-id))]
    (do
      (moved-to (str "/block/" id)))))

(defn get-block [id]
  (->> @(db/get-block id) first :text db/clob->str (edit-view id)))

(defn update-block! [{id :id, :as block}]
  (db/update-block! block)
  (get-block id))

(defn delete-block! [id]
  (db/delete-block! id)
  (moved-to "/blocks/"))

(defn load-account!
  "Updates account for :current-user"
  [account-id]
  (let [user (auth/current-user)
        account (db/get-account account-id)]
    (session/session-put! :current-user
                          (conj user {:account account}))))

(defn auth-success-callback
  "Callback from loginza if auth was successful"
  [result]
  (let [identity (:identity result)
        info (db/get-info identity)
        name (:full_name (:name result))
        email (:email result)]
    (if (nil? info)
      (let [new-account-id (db/create-account! name email)]
        (db/create-info! identity new-account-id result)
        (load-account! new-account-id))
      (load-account! (:account_id info)))))

(defn auth-failed-callback
  "Callback from loginza if auth failed."
  [result]
  (println "This code executed after failed auth"))

(defn current-useremail
  []
  (:email (auth/current-user)))

(defn current-account-id
  []
  (:id (:account (auth/current-user))))

(defroutes main-routes
  (GET    "/"              []                 (ring-response/redirect "/blocks/"))

  (GET    "/block/"        []                 (response/resource "block-form.html"))
  (POST   "/block/"        [& {:keys [text]}] (create-block! text))
  (GET    "/block/:id"     [id]               (get-block id))
  (PUT    "/block/:id"     [& params]         (update-block! params))
  (DELETE "/block/:id"     [id]               (delete-block! id))

  (GET    "/blocks/"       []                 (list-view @(db/list-blocks (current-account-id)) (auth/current-username)))

  (GET    "/user/"         []                 (current-user-view (auth/current-username) (current-useremail)))
  
  (GET    "/login"         []                 (response/resource "login.html"))
  (POST   "/auth_callback" [& params]         ((loginza/create-auth-handler
                                                auth-success-callback
                                                auth-failed-callback) params))
  (GET    "/logout"        []                 (loginza/logout "/"))
  
  (route/not-found "Page not found"))

(def security-policy
  [#"/blocks/.*" [:user :nossl]
   #"/block/.*" [:user :nossl]
   #"/user/.*" [:user :nossl]
   #".*"        [:any :nossl]])

(def app
  (-> (handler/site main-routes)
      (auth/with-security security-policy loginza/loginza-authenticator)
      session/wrap-stateful-session))
  
(defonce *server* (future (run-jetty (var app) {:port 3000})))
