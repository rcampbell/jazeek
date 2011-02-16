(ns jazeek.test.core
  (:use [jazeek.core] :reload)
  (:use [clojure.test])
  (:require [jazeek.db :as db]))
  

;;(deftest replace-me ;; FIXME: write
;; (is false "No tests have been written."))

(deftest create-and-read
         (is (= "Test text" 
                     ((db/get-block (db/create-block! "Test text")) :text))))
