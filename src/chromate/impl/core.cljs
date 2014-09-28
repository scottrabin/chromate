(ns chromate.impl.core
  (:refer-clojure :exclude [js->clj])
  (:require
    [cljs.core.async])
  (:require-macros
    [cljs.core.async.macros :refer [go]]))

(defn js->clj
  [js]
  (cljs.core/js->clj js :keywordize-keys true))
