(ns jazeek.test.db
  (:use [jazeek.db] :reload)
  (:use [clojure.test]))

;; TODO
;;  - get-block should check for unique result

(deftest create-and-read
  (let [test_text "Test text"
        id (create-block! test_text)]
    (is (clob-to-string
         ((first @(get-block id)):text)))))
