(ns chromate.test-storage
  (:require
    [chromate.storage]
    [cemerick.cljs.test :as t])
  (:require-macros
    [cemerick.cljs.test :refer [are is deftest with-test run-tests testing]]))

(defn make-test-storage [k v]
  (let [store-contents (atom {:calls {:get [] :set []}
                              :store {(str k) (prn-str v)}})
        storage (clj->js {"get" (fn [key callback]
                                  (swap! store-contents update-in [:calls :get] conj [key callback])
                                  (when (ifn? callback)
                                    (callback (clj->js (select-keys (:store @store-contents) [key])))))
                          "set" (fn [o callback]
                                  (swap! store-contents update-in [:calls :set] conj [o callback])
                                  (swap! store-contents update-in [:store] merge (js->clj o))
                                  (when (ifn? callback)
                                    (callback)))})]
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
  (let [test-storage (make-test-storage "reset!-key" {:some "val"})]
    (are [x y] (= x y)
         (:some @test-storage) "val"
         {:new "v"} (reset! test-storage {:new "v"})
         (-> test-storage deref :new) "v")))

(deftest storage-swap!
   (let [onearg-store (make-test-storage "swap!-1-key" 1)
         twoarg-store (make-test-storage "swap!-2-key" #{:one 'two})
         threearg-store (make-test-storage "swap!-3-key" {:three :initial :four "nope"})
         manyarg-store (make-test-storage "swap!-many-key" {:one :init})]
     (testing "one argument - fn"
       (are [x y] (= x y)
            2 (swap! onearg-store inc)
            2 (deref onearg-store)))
     (testing "two arguments - fn + extra arg"
       (are [x y] (= x y)
            #{:one 'two 3} (swap! twoarg-store conj 3)
            #{:one 'two 3} (deref twoarg-store)))
     (testing "three arguments - fn + 2 more args"
       (are [x y] (= x y)
            {:three :initial} (swap! threearg-store dissoc :four)
            {:three :initial} (deref threearg-store)))
     (testing "many arguments"
       (are [x y] (= x y)
            {:one :init :two 2 :three 'three} (swap! manyarg-store assoc :two 2 :three 'three)
            {:one :init :two 2 :three 'three} (deref manyarg-store)))))

(deftest storage-watch!
  (testing "basic add/notify/remove of watches"
    (let [storage (make-test-storage "watchable" {:init :value})
          calls (atom [])
          watch-fn (fn [& args] (swap! calls conj args))]
      (is (= storage) (add-watch storage :thekey watch-fn))
      (-notify-watches storage :old :new)
      (are [x y] (= x y)
           (count @calls) 1
           (first @calls) [:thekey storage :old :new])
      (remove-watch storage :not-the-key)
      (-notify-watches storage :theold :thenew)
      (are [x y] (= x y)
           (count @calls) 2
           (second @calls) [:thekey storage :theold :thenew])
      (remove-watch storage :thekey)
      (-notify-watches storage :new :old)
      (is (= (count @calls) 2)))
    )
  (testing "notify should be called for all reset!/swap! calls"
    (let [storage (make-test-storage "watchable" {:init :value})
          calls (atom [])
          watch-fn (fn [& args] (swap! calls conj args))]
      (add-watch storage :reset-swap watch-fn)
      (reset! storage {:init :changed})
      (swap! storage dissoc :init)
      (swap! storage assoc :two "args")
      (swap! storage assoc :three 'three 4 :four)
      (are [x y] (= x y)
           (count @calls) 4
           (nth @calls 0) [:reset-swap storage {:init :value} {:init :changed}]
           (nth @calls 1) [:reset-swap storage {:init :changed} {}]
           (nth @calls 2) [:reset-swap storage {} {:two "args"}]
           (nth @calls 3) [:reset-swap storage {:two "args"} {:two "args" :three 'three 4 :four}]))))
