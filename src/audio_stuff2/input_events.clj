(ns audio-stuff2.input-events
  (:require [overtone.libs.event :as e]
            [clojure.stacktrace :refer [print-stack-trace]]))

(def white-keys
  (vec
    (for [octave (range 20)
          white-key [0 nil 1 nil 2 3 nil 4 nil 5 nil 6]]
      (if white-key
        (+ white-key (* 7 octave))))))

(def black-keys
  (vec
    (for [octave (range 20)
          black-key [nil 0 nil 1 nil nil 2 nil 3 nil 4 nil]]
      (if black-key
        (+ black-key (* 5 octave))))))


(def ^:dynamic event-data)

(defn add-handlers [state-fn initial-state]
  (let [state (agent initial-state)]
    (e/on-event [:midi :note-on]
                #(binding [event-data {:event    :note-on
                                       :note-num (:note %)
                                       :velocity (:velocity-f %)}]
                   (send state state-fn))
                ::note-on)
    (e/on-event [:midi :note-on]
                #(when-let [white-note-num (white-keys (:note %))]
                   (binding [event-data {:event          :white-note-on
                                         :white-note-num white-note-num
                                         :velocity       (:velocity-f %)}]
                     (send state state-fn)))
                ::white-note-on)
    (e/on-event [:midi :note-on]
                #(when-let [black-note-num (black-keys (:note %))]
                   (binding [event-data {:event          :black-note-on
                                         :black-note-num black-note-num
                                         :velocity       (:velocity-f %)}]
                     (send state state-fn)))
                ::black-note-on)
    (e/on-event [:midi :note-off]
                #(binding [event-data {:event    :note-off
                                       :note-num (:note %)}]
                   (send state state-fn))
                ::note-off)
    (e/on-event [:midi :note-off]
                #(when-let [white-note-num (white-keys (:note %))]
                   (binding [event-data {:event          :white-note-off
                                         :white-note-num white-note-num}]
                     (send state state-fn)))
                ::white-note-off)
    (e/on-event [:midi :note-on]
                #(when-let [black-note-num (black-keys (:note %))]
                   (binding [event-data {:event          :black-note-off
                                         :black-note-num black-note-num}]
                     (send state state-fn)))
                ::black-note-off)))
