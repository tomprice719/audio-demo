(ns audio-stuff2.loader
  "Initial namespace loaded by the repl.
  We avoid requiring any other namespaces in this project,
  so that if an error is encountered while compiling one of those namespaces
  it won't prevent this namespace from compiling. This is handy when tweaking things.
  For this reason, functions like play and rec are interned from the control namespace
  rather than required."
  (:require [clojure.tools.namespace.repl]
            [overtone.core :refer :all :exclude [stop clear remove-instrument]]
            [clojure.repl :refer :all]))

(clojure.tools.namespace.repl/disable-reload!)

(defn refresh []
  (when (find-ns 'audio-stuff2.control)
    ((ns-resolve 'audio-stuff2.control 'shutdown)))
  (clojure.tools.namespace.repl/refresh
    :after 'audio-stuff2.core/on-refresh))

(refresh)