(ns audio-stuff2.loader
  (:require [clojure.tools.namespace.repl :refer [refresh]]
            [overtone.core :refer :all]
            [clojure.repl :refer :all]
            [audio-stuff2.breakpoints :refer :all]))

(defn start []
  (flush-bp)
  (when (= (refresh) :ok)
    ((ns-resolve 'audio-stuff2.core 'on-refresh))))