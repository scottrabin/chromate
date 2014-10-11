(ns chromate.storage
  (:refer-clojure :exclude [sync])
  (:require
    [cljs.reader]))

(deftype StorageArea [store key ^:mutable state ^:mutable watches]
  IAtom

  IDeref
  (-deref [_] state)

  IReset
  (-reset! [this new-value]
    (.set store (clj->js {key (prn-str new-value)}))
    (let [old-value state]
      (set! state new-value)
      (-notify-watches this old-value new-value))
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
  (-notify-watches [this oldval newval]
    (doseq [[key f] watches]
      (f key this oldval newval)))
  (-add-watch [_ key f]
    (set! watches (assoc watches key f)))
  (-remove-watch [_ key]
    (set! watches (dissoc watches key))))

(defn make-storage-area [store key]
  (let [storage-area (StorageArea. store key nil nil)]
    (.get store key (fn [v]
                      (set! (.-state storage-area)
                            (cljs.reader/read-string (aget v key)))))
    storage-area))

(try
  (def local (make-storage-area js/chrome.storage.local "com.scottrabin/chromate"))
  (def sync (make-storage-area js/chrome.storage.sync "com.scottrabin/chromate"))
  (catch :default e))
