(ns audio-stuff2.loader
  (:require [clojure.tools.namespace.repl]
            [overtone.core :refer :all :exclude [stop clear]]
            [clojure.repl :refer :all]
            [audio-stuff2.breakpoints :refer :all]))

(defn refresh []
  (when (find-ns 'audio-stuff2.control)
    ((ns-resolve 'audio-stuff2.control 'shutdown)))
  (flush-bp)
  (clojure.tools.namespace.repl/refresh
    :after 'audio-stuff2.core/on-refresh))