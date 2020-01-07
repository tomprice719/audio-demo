(ns audio-stuff2.loader
  (:require [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.core :refer [*e]]
            [overtone.core :refer :all]))

(defn error []
  (println *e))

(defn start []
  (when (= (refresh) :ok)
    ((ns-resolve 'audio-stuff2.core 'on-refresh))))