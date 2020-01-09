(ns audio-stuff2.breakpoints
  (:require
    [clojure.core.async :refer [chan poll! >!!]]))

(def ^:private buffer-size 1000000)

(def breakpoint-channel (atom (chan buffer-size)))
(def current-breakpoint)

(defn put-context [context]
  (>!! @breakpoint-channel context))

(defmacro breakpoint [label & extra-symbols]
  (let [symbols (concat (keys &env) extra-symbols)
        local-context (zipmap (map (fn [sym] `(quote ~sym)) symbols) symbols)
        breakpoint-data {:local-context local-context :label label}]
    `(audio-stuff2.breakpoints/put-context ~breakpoint-data)))

(defn next-bp []
  (if-let [next-breakpoint (poll! @breakpoint-channel)]
    (do
      (def current-breakpoint next-breakpoint)
      (println "At breakpoint: " (:label current-breakpoint)))
    (println "Breakpoint queue is empty.")))

(defmacro at-bp [expr]
  (let [context-sym (gensym)
        context-keys (keys (:local-context current-breakpoint))]
    `(let [~context-sym (:local-context audio-stuff2.breakpoints/current-breakpoint)
           ~@(mapcat (fn [k] [k `(~context-sym '~k)]) context-keys)]
       ~expr)))

(defn flush-bp []
  (reset! breakpoint-channel (chan buffer-size)))
