(ns audio-stuff2.loader
  (:require [clojure.tools.namespace.repl]
            [overtone.core :refer :all :exclude [stop clear remove-instrument]]
            [clojure.repl :refer :all]))

(clojure.tools.namespace.repl/disable-reload!)

(defn refresh []
  (when (find-ns 'audio-stuff2.control)
    ((ns-resolve 'audio-stuff2.control 'shutdown)))
  (clojure.tools.namespace.repl/refresh
    :after 'audio-stuff2.core/on-refresh))

(println "A")