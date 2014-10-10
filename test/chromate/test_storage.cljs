(ns chromate.test-storage
  (:require
    [chromate.storage]
    [cemerick.cljs.test :as t])
  (:require-macros
    [cemerick.cljs.test :refer [are is deftest with-test run-tests testing]]))

(defn make-test-storage [k v]
  (let [store-contents (atom {(str k) (prn-str v)})
        storage (clj->js {"get" (fn [key callback]
                                  (callback (clj->js (select-keys @store-contents [key]))))
                          "set" (fn [o callback]
                                  (swap! store-contents merge o)
                                  (callback))})]
    (chromate.storage/make-storage-area storage k)))

(deftest storage-deref
  (let [test-storage (make-test-storage "key" {:test "value"
                                               :complex {:a 1
                                                         :b 'two}})]
    (are [x y] (= x y)
         (:test @test-storage) "value"
         (-> test-storage deref :complex :a) 1
         (get-in @test-storage [:complex :b]) 'two)))

(deftest storage-reset!

  )

(deftest storage-swap!

  )

(deftest storage-watch!

  )
