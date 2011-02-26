(ns jazeek.loginza
  (:require [sandbar.stateful-session :as session]
            [ring.util.response :as response]
   ))



(defn loginza-authenticator
  "Authenticator to use with loginza.ru"
  [request]
  (do
    (session/session-put! :auth-redirect-uri (:uri request))
    (response/redirect "/login")))

(defn check-auth
  "Check authentication and redirect"
  [req]
  (str (:token req)))
