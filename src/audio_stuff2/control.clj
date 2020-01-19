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

(defn make-music [instruments
                  initial-instrument]
  (refresh-overtone)
  (make-state-agent instruments initial-instrument)
  (set-handlers
    state-agent
    input-event-handler))

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
  (refresh-overtone)
  (-> state
      (update :recording audio-stuff2.recording/stop-playing)
      (assoc :instruments cached-instruments)
      initialize-instruments
      ))

(defn start-playing [state]
  (-> state
      stop-playing
      (update :recording audio-stuff2.recording/start-playing)))

(defn play-and-record [state]
  (-> state
      stop-playing
      (update :recording audio-stuff2.recording/play-and-record)))

(intern 'audio-stuff2.loader 'stop-playing #(send-off state-agent stop-playing))
(intern 'audio-stuff2.loader 'start-playing #(send-off state-agent start-playing))
(intern 'audio-stuff2.loader 'play-and-record #(send-off state-agent play-and-record))
(intern 'audio-stuff2.loader 'update-recording-time-offset
        #(send-off state-agent update-recording-time-offset %))
(intern 'audio-stuff2.loader 'print-time #(-> @state-agent :recording current-time println))
(intern 'audio-stuff2.loader 'get-recording #(:recording @state-agent))
