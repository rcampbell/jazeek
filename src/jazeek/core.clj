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

(deftemplate edit-view "block-form.html" [id text]
  [:form]     (do-> (set-attr :action (str "/blocks/" id))
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
  (let [id (db/create-block! text)]
    (moved-to (str "/blocks/" id))))

(defn get-block [id]
  (->> @(db/get-block id) first :text db/clob->str (edit-view id)))

(defn update-block! [{id :id, :as block}]
  (db/update-block! block)
  (get-block id))

(defn delete-block! [id]
  (db/delete-block! id)
  (moved-to "/"))


(def security-policy
  [#"/blocks/.*" [:user :nossl]
   #"/block/.*" [:user :nossl]
   #".*"        [:any :nossl]])


(defroutes main-routes
  (GET    "/"           []                 (ring-response/redirect "/blocks/"))

  (GET    "/block/"    []                 (response/resource "block-form.html"))
  (POST   "/block/"    [& {:keys [text]}] (create-block! text))
  (GET    "/block/:id" [id]               (get-block id))
  (PUT    "/block/:id" [& params]         (update-block! params))
  (DELETE "/block/:id" [id]               (delete-block! id))

  (GET    "/blocks/"    []                 (list-view @(db/list-blocks) (:name (session/session-get :current-user))))
  
  (GET    "/login"      []                 (response/resource "login.html"))
  (POST   "/auth_callback" [& params]      (loginza/auth-callback params))
  (GET    "/logout"     []                 (loginza/logout "/"))
  
  (route/not-found "Page not found"))

(def app
  (-> (handler/site main-routes)
      (auth/with-security security-policy loginza/loginza-authenticator)
      session/wrap-stateful-session))
  
(defonce *server* (future (run-jetty (var app) {:port 3000})))
