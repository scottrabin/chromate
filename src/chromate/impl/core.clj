(ns chromate.impl.core)

(defmacro channelate
  "Convert a function call with a callback parameter as its last argument into
  a single-value channel that closes immediately after"
  [f & args]
  `(let [rc# (cljs.core.async/chan)]
     (~f ~@args (fn [v#]
                    (when-not (nil? v#)
                      (cljs.core.async/put! rc# v#))
                    (cljs.core.async/close! rc#)))
     rc#))

(defmacro defevent
  "Define an event attachment point for the Chrome Extension API"
  [& body]
  ; TODO
  )
