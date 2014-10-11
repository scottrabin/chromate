(ns chromate.storage
  (:require
    [cljs.reader]))

(deftype StorageArea [store key ^:mutable state watchers]
  IAtom
  IDeref
  (-deref [_] state)
  IReset
  (-reset! [_ new-value]
    (.set store (clj->js {key (prn-str new-value)}))
    (set! state new-value)
    new-value)
  ISwap
  (-swap! [this f]
    (reset! this (f state)))
  (-swap! [this f a]
    (reset! this (f state a)))
  (-swap! [this f a b]
    (reset! this (f state a b)))
  (-swap! [this f a b args]
    (reset! this (apply f state a b args)))
  IWatchable
  (-notify-watches [_ oldval newval])
  (-add-watch [_ key f])
  (-remove-watch [_ key]))

(defn make-storage-area [store key]
  (let [storage-area (StorageArea. store key nil nil)]
    (.get store key (fn [v]
                      (set! (.-state storage-area) (cljs.reader/read-string (aget v key)))))
    storage-area))

(try
  (def local (make-storage-area js/chrome.storage.local "com.scottrabin/chromate"))
  (def sync (make-storage-area js/chrome.storage.sync "com.scottrabin/chromate"))
  (catch :default e))
