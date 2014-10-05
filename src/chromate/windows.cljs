(ns chromate.windows
  (:refer-clojure :exclude [get remove])
  (:require
    [chromate.types]
    [cljs.core.async])
  (:require-macros
    [chromate.impl.core :refer [channelate defevent]]
    [cljs.core.async.macros :refer [go]]))

(defn get
  "Get details about the specified window"
  [winid get-info]
  (go
    (chromate.types/js->Window
      (<! (channelate js/chrome.windows.get
                      winid
                      (-> get-info
                          (select-keys [:populate])
                          clj->js))))))

(defn get-current
  "Get details about the current window"
  ([]
   (get-current nil))
  ([get-info]
   (go
     (chromate.types/js->Window
       (<! (channelate js/chrome.windows.getCurrent
                       (-> get-info
                           (select-keys [:populate])
                           clj->js)))))))

(defn get-last-focused
  "Get the last focused window"
  [get-info]
  (go
    (chromate.types/js->Window
      (<! (channelate js/chrome.windows.getLastFocused
                      (-> get-info
                          (select-keys [:populate])
                          clj->js))))))

(defn get-all
  "Get all windows"
  [get-info]
  (go
    (map chromate.types/js->Window
         (<! (channelate js/chrome.windows.getAll
                         (-> get-info
                             (select-keys [:populate])
                             clj->js))))))

(defn create
  "Create a new window with the specified properties"
  [createprops]
  (go
    (chromate.types/js->Window
      (<! (channelate js/chrome.windows.create
                      (-> createprops
                          (select-keys [:url
                                        :tabId
                                        :left
                                        :top
                                        :width
                                        :height
                                        :focused
                                        :incognito
                                        :type])
                          clj->js))))))

(defn update
  "Modify a window with the specified properties"
  [winid updateprops]
  (go
    (chromate.types/js->Window
      (<! (channelate js/chrome.windows.update
                      winid
                      (-> updateprops
                          (select-keys [:left
                                        :top
                                        :width
                                        :height
                                        :focused
                                        :drawAttention
                                        :state])
                          clj->js))))))

(defn remove
  "Close a window and all contained tabs"
  [win]
  (let [id (if (= (type win) chromate.types/Window)
             (:id win)
             win)]
    (channelate js/chrome.windows.remove id)))

; EVENTS

(defevent on-created js/chrome.windows.onCreated)

(defevent on-removed js/chrome.windows.onRemoved)

(defevent on-focus-changed js/chrome.windows.onFocusChanged)
