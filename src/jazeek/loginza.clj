(ns jazeek.loginza
  (:require [sandbar.stateful-session :as session]
            [ring.util.response :as response]
            [http.async.client :as c]
            [clojure.contrib.json :as json]))


(defn loginza-authenticator
  "Authenticator to use with loginza.ru"
  [request]
  (do
    (session/session-put! :auth-redirect-uri (:uri request))
    (response/redirect "/login")))

(defn- check-token
  "Checks token against loginza API. Turns reply into map"
  [token]
  (let [response (c/GET (str "http://loginza.ru/api/authinfo?token=" token))]
    (c/await response)
    (json/read-json (c/string response))))

(defn- success?
  "Checks if auth results indicate success"
  [result]
  (contains? result :identity))

;; TODO implement this
(defn- roles-for-identity
  "Returns a set of roles for given identity"
  [identity]
  #{:user})

(defn auth-callback
  "Check authentication results and do redirect"
  [req]
  (let [result (check-token (:token req))
        success (or (session/session-get :auth-redirect-uri)
                    "/")
        failure "/login"]
    (if (success? result)
      (do
        (session/session-put! :current-user
                              {:name (:full_name (:name result))
                               :identity (:identity result)
                               :email (:email result)
                               :roles (roles-for-identity (:identity result) )})
        (session/session-delete-key! :auth-redirect-uri)
        (response/redirect success))
      (response/redirect failure))))

(defn logout
  "Logs current user out"
  [after-logout]
  (do
    (session/session-delete-key! :current-user)
    (response/redirect after-logout)))
