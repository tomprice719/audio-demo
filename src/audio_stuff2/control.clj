(ns audio-stuff2.control
  (:require [audio-stuff2.input-events :refer [set-handlers]]
            [audio-stuff2.instruments.base-instrument :refer [message-handlers]]
            [audio-stuff2.instruments.message-generators :refer [generate-messages]]
            [debux.core :refer [dbg dbgn]]
            [audio-stuff2.breakpoints :refer [breakpoint]]))

(defn handle-messages [state messages]
  (reduce
    (fn [state [instrument-key fn-key & args]]
      (update-in state [:instruments instrument-key]
                 #(apply (message-handlers fn-key) % args)))
    state
    messages))

(defn make-music [instruments
                  initial-instrument]
  (set-handlers
    {:instruments        instruments
     :current-instrument initial-instrument}
    (fn [event-data state]
      (let [current-instrument-key (:current-instrument state)
            current-instrument (-> state :instruments current-instrument-key)
            messages (generate-messages
                       current-instrument
                       current-instrument-key
                       event-data)]
        (handle-messages state messages)))))

(comment
  (-> {}
      (assoc instruments {})
      (assoc state (recording "filename" handle-messages stop-fn))))

(comment
  (defn control-recording [state event-data key->recording-fn]
    (update state :recording (key->recording-fn (TODO event-data)))))

(comment
  (defmulti event->messages
            (fn [instrument event-data]
              [(:input-type instrument)
               (:event-type event-data)])))

(comment
  (defn update-state [state messages]
    (-> state
        (handle-messages messages)
        (record-messages messages)
        (change-instrument event-data key->instrument)
        (control-recording event-data key->recording-fn)))

  (fn [event-data state]
    (let [messages (event->messages (:current-instrument state) event-data)]
      (update-state state messages))))
