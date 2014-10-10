(defproject chromate "0.37.0-SNAPSHOT"
  :description "A ClojureScript adapter to the Chrome Extensions API"
  :url "https://github.com/scottrabin/chromate"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2342"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]]

  :profiles {:dev {:plugins [[lein-cljsbuild "1.0.4-SNAPSHOT"]
                             [com.cemerick/clojurescript.test "0.3.1"]]
                   :hooks [leiningen.cljsbuild]}}

  :cljsbuild {:builds {:test
                       {:source-paths  ["src/chromate" "test/chromate"]
                        :compiler {:output-to "target/cljs/testable.js"
                                   :output-dir "target/cljs"}
                        :optimizations :whitespace
                        :pretty-print true}}
              :test-commands {"unit-tests" ["phantomjs" :runner
                                            "target/cljs/testable.js"]}})
