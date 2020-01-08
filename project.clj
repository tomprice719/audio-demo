(defproject audio-stuff2 "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [overtone "0.10.3"]
                 [org.clojure/tools.namespace "0.2.11"]
                 [com.github.wendykierp/JTransforms "3.0"]
                 [clj-stacktrace "0.2.8"]]
  :repl-options {:init-ns audio-stuff2.loader})