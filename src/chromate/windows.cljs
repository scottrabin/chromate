(ns chromate.windows
  (:refer-clojure :exclude [get remove])
  (:require
    [cljs.core.async])
  (:require-macros
    [chromate.impl.core :refer [channelate defevent]]
    [cljs.core.async.macros :refer [go]]))

(defrecord Window [^int    id          ; unique ID of the window
                   ^bool   focused     ; Whether the window is focused
                   ^int    top         ; Offset of window from top of screen, in pixels
                   ^int    left        ; Offset of window from left of screen, in pixels
                   ^int    width       ; Width of window, in pixels
                   ^int    height      ; Height of window, in pixels
                           tabs        ; Array (?) of tabs in the window
                   ^bool   incognito   ; Whether the window is incognito
                           type        ; Type of window
                                       ;   (one of: :normal, :popup, :panel, :app)
                           state       ; State of the browser window
                                       ;   (one of: :normal, :minimized, :maximized, :fullscreen)
                   ^bool   alwaysOnTop ; Whether the window is set to be always on top
                   ^String sessionId   ; Session ID used to identify a window via the Sessions API
                   ])

(defn js->Window
  [win]
  (map->Window (js->clj win :keywordize-keys true)))

(defn get
  "Get details about the specified window"
  [winid get-info]
  (go
    (js->Window (<! (channelate js/chrome.windows.get
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
     (js->Window (<! (channelate js/chrome.windows.getCurrent
                                 (-> get-info
                                     (select-keys [:populate])
                                     clj->js)))))))

(defn get-last-focused
  "Get the last focused window"
  [get-info]
  (go
    (js->Window (<! (channelate js/chrome.windows.getLastFocused
                                (-> get-info
                                    (select-keys [:populate])
                                    clj->js))))))

(defn get-all
  "Get all windows"
  [get-info]
  (go
    (map js->Window
         (<! (channelate js/chrome.windows.getAll
                         (-> get-info
                             (select-keys [:populate])
                             clj->js))))))

(defn create
  "Create a new window with the specified properties"
  [createprops]
  (go
    (js->Window (<! (channelate js/chrome.windows.create
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
    (js->Window (<! (channelate js/chrome.windows.update
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
  [winid]
  (channelate js/chrome.windows.remove winid))

; EVENTS

(defevent on-created js/chrome.windows.onCreated)

(defevent on-removed js/chrome.windows.onRemoved)

(defevent on-focus-changed js/chrome.windows.onFocusChanged)
