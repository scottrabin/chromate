(ns chromate.types)

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

(defn js->Tab
  [tab]
  (map->Tab (js->clj tab :keywordize-keys true)))

(defn js->Window
  [win]
  (update-in (map->Window (js->clj win :keywordize-keys true))
             [:tabs]
             #(mapv map->Tab %)))
