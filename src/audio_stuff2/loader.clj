(ns audio-stuff2.loader
  (:require [clojure.tools.namespace.repl]
            [overtone.core :refer :all :exclude [stop clear]]
            [clojure.repl :refer :all]
            [audio-stuff2.breakpoints :refer :all]))

(defn refresh []
  (println "abc")
  (flush-bp)
  (clojure.tools.namespace.repl/refresh
    :after 'audio-stuff2.core/on-refresh))