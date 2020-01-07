(ns audio-stuff2.loader
  (:require [clojure.tools.namespace.repl :refer [refresh]]
            [overtone.core :refer :all]
            [clojure.repl :refer :all]))

(defn start []
  (when (= (refresh) :ok)
    ((ns-resolve 'audio-stuff2.core 'on-refresh))))