(ns audio-stuff2.control
  (:require [audio-stuff2.input-events :refer [set-handlers]]
            [audio-stuff2.instruments.base-instrument :refer [message-handlers initialize audible]]
            [audio-stuff2.instruments.message-generators :refer [generate-messages]]
            [audio-stuff2.recording :refer [make-recording
                                            initial-events
                                            record-event
                                            current-time
                                            update-time-offset]]
            [clojure.repl :refer [pst]]
            [debux.core :refer [dbg dbgn]]
            [audio-stuff2.breakpoints :refer [breakpoint]]
            [clojure.algo.generic.functor :refer [fmap]]
            [audio-stuff2.overtone-utils :refer [refresh-overtone]]))

(declare state-agent)

(defn instrument-reducer [instrument-state [instrument-key fn-key & args]]
  (update instrument-state instrument-key
          #(apply (message-handlers fn-key) % args)))

(defn instrument-message-handler [{:keys [instruments] :as state} messages]
  (assoc state :instruments (reduce instrument-reducer instruments messages)))

(defn initialize-instruments [state]
  (update state :instruments (partial fmap initialize)))

(defn recording-play-fn [messages]
  (send-off state-agent
            (fn [state] (instrument-message-handler state messages))))

(defn make-state-agent [instruments selected-instrument]
  (def state-agent (-> {:instruments         instruments
                        :initial-instruments instruments
                        :cached-instruments  instruments
                        :selected-instrument selected-instrument
                        :recording           (make-recording recording-play-fn)}
                       initialize-instruments
                       agent))
  (set-error-mode! state-agent :continue)
  (set-error-handler! state-agent #(pst %2)))

(defn get-instrument-messages [state event-data]
  (let [selected-instrument-key (:selected-instrument state)
        selected-instrument (-> state :instruments selected-instrument-key)]
    (generate-messages
      selected-instrument
      selected-instrument-key
      event-data)))

(defn input-event-handler [state event-data]
  (let [messages (get-instrument-messages state event-data)]
    (-> state
        (instrument-message-handler messages)
        (update :recording record-event messages))))

(defn refresh-cached-instruments [{:keys [initial-instruments recording] :as state}]
  (assoc state :cached-instruments
               (binding [audible false]
                 (reduce instrument-reducer
                         initial-instruments
                         (initial-events recording)))))

(defn update-recording-time-offset [{:keys [initial-instruments] :as state} time-offset]
  (-> state
      (update :recording audio-stuff2.recording/update-time-offset
              time-offset)
      refresh-cached-instruments))

(defn stop-playing [{:keys [cached-instruments] :as state}]
  (-> state
      (update :recording audio-stuff2.recording/stop-playing)
      (assoc :instruments cached-instruments)
      refresh-overtone
      initialize-instruments))

(defn start-playing [state]
  (-> state
      stop-playing
      (update :recording audio-stuff2.recording/start-playing)))

(defn play-and-record [state]
  (-> state
      stop-playing
      (update :recording audio-stuff2.recording/play-and-record)))

(defn clear-recording [{:keys [initial-instruments] :as state}]
  (-> state
      (update :recording stop-playing)
      (assoc :recording (make-recording recording-play-fn)
             :cached-instruments initial-instruments
             :instruments initial-instruments)
      initialize-instruments))

(defmacro defintern [name args & forms]
  `(intern *ns* '~name (fn [~@(rest args)]
                         (send-off state-agent
                                   (fn ~args ~@forms)
                                   ~@(rest args))
                         nil)))

(defn make-music [instruments
                  initial-instrument]
  (refresh-overtone)
  (make-state-agent instruments initial-instrument)
  (set-handlers
    state-agent
    input-event-handler)
  (defintern play [state] (start-playing state))
  (defintern stop [state] (stop-playing state))
  (defintern rec [state] (play-and-record state))
  (defintern clear [state] (clear-recording state))
  (defintern get-time [state] (-> state :recording current-time println))
  (defintern set-time [state time] (update-recording-time-offset state time)))

