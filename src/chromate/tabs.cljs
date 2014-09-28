(ns chromate.tabs
  (:refer-clojure :exclude [get remove])
  (:require
    [chromate.windows]
    [cljs.core.async])
  (:require-macros
    [chromate.impl.core :refer [channelate defevent]]
    [cljs.core.async.macros :refer [go]]))

(defrecord Tab [^int    id          ; unique ID of tab
                ^int    index       ; The zero-based index of the tab within its window
                ^int    windowId    ; The ID of the window owner of this tab
                ^int    openerTabId ; The ID of the tab that caused this tab to be open (if applicable)
                ; selected          ; deprecated
                ^bool   highlighted ; Whether the tab is highlighted
                ^bool   active      ; Whether the tab is active in its window
                ^bool   pinned      ; Whether the tab is pinned
                ^String url         ; URL displayed by the tab
                ^String title       ; The title of the tab (requires "tabs" perm)
                ^String favIconUrl  ; URL of the tab's favicon
                        status      ; status of tab (:loading or :complete)
                ^bool   incognito   ; Whether the tab is in an incognito window
                ^int    width       ; Width of tab (in pixels)
                ^int    height      ; Height of tab (in pixels)
                ^String sessionId   ; Session ID used to identify tab from Sessions API
                ])

(defn js->Tab
  [tab]
  (map->Tab (js->clj tab :keywordize-keys true)))

(defn get
  "Get the details of a specific tab by its ID"
  [tabid]
  (go
    (js->Tab (<! (channelate js/chrome.tabs.get tabid)))))

(defn get-current
  "Get the details of the current tab that this call is being made from"
  []
  (go
    (js->Tab (<! (channelate js/chrome.tabs.getCurrent)))))

(defn connect
  "Connect to the each of the content scripts in a given tab"
  (^js/chrome.runtime.Port
    [tabid]
    (connect tabid nil))
  (^js/chrome.runtime.Port
    [tabid connectinfo]
    (js/chrome.tabs.connect tabid connectinfo)))

; js/chrome.tabs.sendRequest deprecated

(defn send-message
  "Send a message to each of the content scripts in a given tab"
  [tabid msg]
  (go
    (js->clj
      (<! (channelate js/chrome.tabs.sendMessage tabid (clj->js msg)))
      :keywordize-keys true)))

; js/chrome.tabs.getSelected deprecated

; js/chrome.tabs.getAllInWindow deprecated

(defn create
  "Create a new tab with the given properties"
  [createprops]
  (go
    (js->Tab (<! (channelate js/chrome.tabs.create
                             (-> createprops
                                 (select-keys [:windowId
                                               :index
                                               :url
                                               :active
                                               ; :selected ; deprecated
                                               :pinned
                                               :openerTabId])
                                 clj->js))))))

(defn duplicate
  "Duplicate the given tab"
  [tabid]
  (go
    (js->Tab (<! (channelate js/chrome.tabs.duplicate tabid)))))

(defn query
  "Get the set of tabs matching the query parameters"
  [queryparams]
  (go
    (map js->Tab (<! (channelate js/chrome.tabs.query
                                 (-> queryparams
                                     (select-keys [:active
                                                   :pinned
                                                   :highlighted
                                                   :currentWindow
                                                   :lastFocusedWindow
                                                   :status
                                                   :title
                                                   :url
                                                   :windowId
                                                   :windowType
                                                   :index])
                                     clj->js))))))

(defn highlight
  "Highlight the given tab or set of tabs"
  [hlinfo]
  (go
    (chromate.windows/js->Window
      (<! (channelate js/chrome.tabs.highlight
                      (-> hlinfo
                          (select-keys [:windowId :tabs])
                          clj->js))))))

(defn update
  "Modify the given tab with the given properties"
  [tabid updprops]
  (go
    (js->Tab (<! (channelate js/chrome.tabs.update
                             tabid
                             (-> updprops
                                 (select-keys [:url
                                               :active
                                               :highlighted
                                               ; :selected ; deprecated
                                               :pinned
                                               :openerTabId])
                                 clj->js))))))

(defn move
  "Move the specified tabs to a new position or window"
  [tab-or-tabs moveprops]
  (go
    (js->Tab (<! (channelate (clj->js tab-or-tabs)
                              (-> moveprops
                                  (select-keys [:windowId :index])
                                  clj->js))))))

(defn reload
  "Reload the specified tab"
  [tabid reloadprops]
  (channelate js/chrome.tabs.reload
              tabid
              (-> reloadprops
                  (select-keys [:bypassCache])
                  clj->js)))

(defn remove
  "Closes the specified tab"
  [tab-or-tabs]
  (channelate js/chrome.tabs.remove (clj->js tab-or-tabs)))

(defn detect-language
  "Detect the primary language of the content in a tab"
  ([]
   ; defaults to active tab of the current window
   (channelate js/chrome.tabs.detectLanguage nil))
  ([tabid]
   (channelate js/chrome.tabs.detectLanguage tabid)))

(defn capture-visible-tab
  "Captures the visible area of the currently active tab in the specified window"
  [windowId options]
  (channelate js/chrome.tabs.captureVisibleTab
              windowId
              (-> options
                  (select-keys [:format :quality])
                  clj->js)))

(defn execute-script
  "Injects Javascript into a page.
  Yields an array containing the result of the last evaluated statement in the script."
  [tabid details]
  (go
    (map #(js->clj % :keywordize-keys true)
         (<! (channelate js/chrome.tabs.executeScript
                         tabid
                         (-> details
                             (select-keys [:code
                                           :file
                                           :allFrames
                                           :matchAboutBlank
                                           :runAt])
                             clj->js))))))

(defn insert-css
  "Injects CSS into a page"
  [tabid details]
  (channelate js/chrome.tabs.insertCSS
              tabid
              (-> details
                  (select-keys [:code
                                :file
                                :allFrames
                                :matchAboutBlank
                                :runAt])
                  clj->js)))

; setZoom is dev channel only

; getZoom is dev channel only

; setZoomSettings is dev channel only

; getZoomSettings is dev channel only

; EVENTS

(defevent on-created js/chrome.tabs.onCreated)

(defevent on-updated js/chrome.tabs.onUpdated)

(defevent on-moved js/chrome.tabs.onMoved)

; (defevent on-selection-changed js/chrome.tabs.onSelectionChanged) ; deprecated

; (defevent on-active-changed js/chrome.tabs.onActiveChanged) ; deprecated

(defevent on-activated js/chrome.tabs.onActivated)

; (defevent on-highlight-changed js/chrome.tabs.onHighlightChanged) ; deprecated

(defevent on-highlighted js/chrome.tabs.onHighlighted)

(defevent on-detached js/chrome.tabs.onDetached)

(defevent on-attached js/chrome.tabs.onAttached)

(defevent on-removed js/chrome.tabs.onRemoved)

(defevent on-replaced js/chrome.tabs.onReplaced)

; (defevent on-zoom-change js/chrome.tabs.onZoomChange) ; beta channel only
