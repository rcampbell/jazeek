(ns jazeek.person
  (:require [jazeek.db :as db]
            [sandbar.stateful-session :as session]
            [sandbar.auth :as auth]
            [jazeek.loginza :as loginza]))

(defn- get-gravatar
  "Retrives gravatar image for given account"
  [account]
  (let [email  (.toLowerCase (.trim (:email account)))
        hash (.toString (new BigInteger
                             1
                             (.digest (java.security.MessageDigest/getInstance "MD5") (.getBytes email "UTF-8"))) 16)]
    (str "http://www.gravatar.com/avatar/"
         (loop [result hash]
           (if (not (= 32 (count result)))
             (recur (str "0" result))
                   result)) 
         "?s=128&d=identicon&r=PG")))

(defn get-photo
  "Returns photo image for given account"
  [account]
  (if (not (nil? (:email account)))
    (get-gravatar account)
    nil))

(defn get-account
  ""
  [account-id]
  (db/get-account account-id))


                                        ; Security related code

(defn get-roles-for-account
  "Returns a list of roles for given account"
  [account]
  #{:user})

(defn do-login-user!
  [account-id]
  (let [account (db/get-account account-id)
        roles (get-roles-for-account account)]
    (session/session-put! :current-user
                          {:name (:name account)
                           :email (:email account)
                           :account account
                           :roles roles})))

(defn current-useremail
  []
  (:email (auth/current-user)))

(defn current-account-id
  []
  (:id (:account (auth/current-user))))

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
        (do-login-user! new-account-id))
      (do-login-user! (:account_id info)))))

(defn current-username
  "Returns current user name"
  []
  (auth/current-username))

(defn auth-failed-callback
  "Callback from loginza if auth failed."
  [result]
  (println "This code executed after failed auth"))

(defn logout
  "Handler for logout action"
  []
  (loginza/logout "/"))

(defn login
  "Handler for login action"
  [params]
  (let [handler (loginza/create-auth-handler auth-success-callback auth-failed-callback)] 
       (handler params)))

(defn authenticator
  "Authenticating processor"
  [request]
  (loginza/loginza-authenticator request))

