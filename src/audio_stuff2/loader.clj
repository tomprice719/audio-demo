(ns audio-stuff2.loader
  (:require [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.core :refer [*e]]))

(defn start []
  (if (= (refresh) :ok)
    ((ns-resolve 'audio-stuff2.core 'on-refresh))
    (println *e)))