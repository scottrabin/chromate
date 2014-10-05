(ns chromate.tabs
  (:refer-clojure :exclude [get remove])
  (:require
    [goog.math.ExponentialBackoff]
    [chromate.types]
    [cljs.reader]
    [cljs.core.async.impl.protocols]
    [cljs.core.async])
  (:require-macros
    [chromate.impl.core :refer [channelate defevent]]
    [cljs.core.async.macros :refer [go go-loop]]))

(defn get
  "Get the details of a specific tab by ID, or the current tab if none is specified"
  ([]
   (go
     (chromate.types/js->Tab
       (<! (channelate js/chrome.tabs.getCurrent)))))
  ([tabid]
   (go
     (chromate.types/js->Tab
       (<! (channelate js/chrome.tabs.get tabid))))))

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
    (chromate.types/js->Tab
      (<! (channelate js/chrome.tabs.create
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
    (chromate.types/js->Tab
      (<! (channelate js/chrome.tabs.duplicate tabid)))))

(defn query
  "Get the set of tabs matching the query parameters"
  [queryparams]
  (go
    (map chromate.types/js->Tab
         (<! (channelate js/chrome.tabs.query
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
    (chromate.types/js->Window
      (<! (channelate js/chrome.tabs.highlight
                      (-> hlinfo
                          (select-keys [:windowId :tabs])
                          clj->js))))))

(defn update
  "Modify the given tab with the given properties"
  [tabid updprops]
  (go
    (chromate.types/js->Tab
      (<! (channelate js/chrome.tabs.update
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
    (chromate.types/js->Tab
      (<! (channelate (clj->js tab-or-tabs)
                      (-> moveprops
                          (select-keys [:windowId
                                        :index])
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
                  (select-keys [:format
                                :quality])
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

(deftype Port [port ^:mutable closed tx rx]
  cljs.core.async.impl.protocols/WritePort
  (put! [this val ^not-native handler]
    (cljs.core.async.impl.protocols/put! tx val handler))

  cljs.core.async.impl.protocols/ReadPort
  (take! [this ^not-native handler]
    (cljs.core.async.impl.protocols/take! rx handler))

  cljs.core.async.impl.protocols/Channel
  (closed? [_] closed)
  (close! [this]
    (set! closed true)
    (.disconnect port)
    (cljs.core.async/close! tx)
    (cljs.core.async/close! rx)))

; port map is a cache of active connected ports for tabs
(def ^:private port-map (atom {}))

(js/chrome.tabs.onRemoved.addListener
  (fn [tabid removeinfo]
    (when-let [port (cljs.core/get port-map tabid)]
      (cljs.core.async/close! port)
      (swap! port-map dissoc tabid))))

(defn- -send-retry
  "Repeatedly attempt to send a message across a chrome.runtime.Port until it
  appears futile"
  [tab-port msg]
  (go
    (let [max-backoff 5121
          backoff (goog.math.ExponentialBackoff. 20 max-backoff)]
      (loop []
        (try
          (.postMessage tab-port msg)
          true
          (catch js/Error e
            (.backoff backoff)
            (if (= max-backoff (.getValue backoff))
              false
              (do
                (<! (cljs.core.async/timeout (.getValue backoff)))
                (recur)))))))))

(defn- make-port
  [tab-port]
  (let [tx (cljs.core.async/chan)
        rx (cljs.core.async/chan)
        port (Port. tab-port false tx rx)]
    ; when the port disconnects, close the channels
    (.onDisconnect.addListener tab-port (fn [_]
                                          (cljs.core.async/close! port)))
    ; when a message comes in from the port, put it on the receive channel
    (.onMessage.addListener tab-port (fn [msg sender respond]
                                       (cljs.core.async/put! rx [(cljs.reader/read-string msg)
                                                                 sender
                                                                 respond])))
    (go-loop []
             ; wait for user to send values from tx channel
             (let [v (<! tx)]
               (when-not (nil? v)
                 (when (<! (-send-retry tab-port (prn-str v)))
                   (recur)))))
    port))

(defn tab-connect
  [tab]
  (if-let [port (cljs.core/get port-map (:id tab))]
    port
    (let [port (make-port (js/chrome.tabs.connect (:id tab)))]
      (swap! port-map assoc (:id tab) port)
      port)))

(defn tab-accept
  []
  (let [wait (cljs.core.async/chan)]
    (js/chrome.runtime.onConnect.addListener
      (fn waiter [tab-port]
        (js/chrome.runtime.onConnect.removeListener waiter)
        (cljs.core.async/put! wait (make-port tab-port))
        (cljs.core.async/close! wait)))
    wait))

(defn really-send-message
  [tab msg]
  (go
    (cljs.core.async/put! (tab-connect tab) msg)))
