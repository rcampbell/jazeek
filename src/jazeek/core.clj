(ns jazeek.core
  (:use [clojure.string :only (join)]
        [net.cgrand.enlive-html :only (deftemplate do-> set-attr prepend content clone-for)]
        [compojure.core :only (GET POST PUT DELETE defroutes)]
        [ring.adapter.jetty :only (run-jetty)] )
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]
            [ring.util.response :as ring-response]
            [jazeek.db :as db]
            [jazeek.person :as person]
            [jazeek.block :as block]))


(letfn [(redirect [status]
                  (fn [uri] (assoc (ring-response/redirect uri) :status status)))]
  (def ^{:doc "moved permanently" :private true}
    moved-to  (redirect 301)))


(deftemplate user-view "user/user.html" [account]
  [:#name] (content (:name account))
  [:#email] (content (:email account))
  [:#photo] (set-attr :src (person/get-photo account)))

(deftemplate edit-view "block-form.html" [id text]
  [:form]     (do-> (set-attr :action (str "/block/" id))
                    (prepend {:tag :input :attrs {:type "hidden"
                                                  :name "_method"
                                                  :value "PUT"}}))
  [:textarea] (content text))

(defn- snippet
  "Returst a snippet of the text"
  [text]
  (let [limit 20
        stub "..."
        offset (- limit (count stub))]
    (if (>= (count text) limit)  (str (.substring text 0 offset) "...") text)))

(deftemplate list-view "list.html" [blocks name account-id]
  [:#username] (do-> (content name)
                     (set-attr :href (str "/user/" account-id)))
  {[:dt] [:dd]}  (clone-for [{:keys [id text]} blocks]
             [:a]  (do-> (set-attr :href (str "/block/" id))
                         (content id))
             [:dd] (content (snippet text))))


(defn create-block! [text account-id]
  (let [id (block/create! text account-id)]
    (do
      (moved-to (str "/block/" id)))))

(defn delete-block! [id]
  (block/delete! id)
  (moved-to "/blocks/"))

(defn get-block [id] (edit-view id (block/one id)))

(defn update-block! [{id :id, :as block}]
  ""
  (block/update! block)
  (moved-to "/blocks/"))


(defroutes main-routes
  (GET    "/"              []                 (ring-response/redirect "/blocks/"))

  (GET    "/block/"        []                 (response/resource "block-form.html"))
  (POST   "/block/"        [& {:keys [text]}] (create-block! text (person/current-account-id)))
  (GET    "/block/:id"     [id]               (get-block id))
  (PUT    "/block/:id"     [& params]         (update-block! params))
  (DELETE "/block/:id"     [id]               (delete-block! id))

  (GET    "/blocks/"       []                 (list-view (block/all (person/current-account-id))
                                                         (person/current-username)
                                                         (person/current-account-id)))

  (GET    "/user/:id"      [id]               (user-view (db/get-account id)))

  (GET    "/login"         []                 (response/resource "login.html"))
  (GET    "/logout"        []                 (person/logout))
  (POST   "/auth_callback" [& params]         (person/login params))

  (route/resources "/")

  (route/not-found "Page not found"))

(def security-policy
  [#"/blocks/.*" [:user :nossl]
   #"/block/.*"  [:user :nossl]
   #"/user/.*"   [:user :nossl]
   #".*"         [:any :nossl]])

(def app
  (-> (handler/site main-routes)
      (sandbar.auth/with-security security-policy person/authenticator)
      sandbar.stateful-session/wrap-stateful-session))
  
(defonce *server* (future (run-jetty (var app) {:port 3000})))
