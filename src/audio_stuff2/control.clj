(ns audio-stuff2.control
  (:require [audio-stuff2.input-events :refer [set-handlers close-window]]
            [audio-stuff2.instruments.base-instrument :refer [message-handlers initialize audible]]
            [audio-stuff2.instruments.message-generators :refer [generate-messages]]
            [audio-stuff2.recording :refer [make-recording
                                            initial-events
                                            record-event
                                            recording-time
                                            transduce-events]]
            [overtone.core]
            [clojure.repl :refer [pst]]
            [debux.core :refer [dbg dbgn]]
            [audio-stuff2.breakpoints :refer [breakpoint]]
            [clojure.algo.generic.functor :refer [fmap]]
            [audio-stuff2.overtone-utils :refer [refresh-overtone]]))

(declare state-agent)

(defn initialize-instruments [state]
  (update state :instruments (partial fmap initialize)))

(defn consume-message [instrument-state [instrument-key fn-key & args]]
  (update instrument-state instrument-key
          #(apply (message-handlers fn-key) % args)))

(defn recording-play-fn [message]
  (send-off state-agent update :instruments consume-message message))

(defn get-instrument-messages [state event-data]
  (let [selected-instrument-key (:selected-instrument state)
        selected-instrument (-> state :instruments selected-instrument-key)]
    (generate-messages
      selected-instrument
      selected-instrument-key
      event-data)))

(defn apply-keymap-fn [{:keys [keymap] :as state} {:keys [event key-char]}]
  (if-let
    [func (and (= event :key-pressed)
               (keymap key-char))]
    (func state)
    state))

(defn reduce2 [val f coll]
  "This is just reduce but with the arguments in a different order,
  which is more convenient sometimes in threading macros."
  (reduce f val coll))

(defn input-event-handler [state event-data]
  (let [messages (get-instrument-messages state event-data)]
    (-> state
        (update :instruments reduce2 consume-message messages)
        (update :recording reduce2 record-event messages)
        (apply-keymap-fn event-data))))

(defn refresh-cached-instruments [{:keys [initial-instruments recording] :as state}]
  (assoc state :cached-instruments
               (binding [audible false]
                 (reduce consume-message
                         initial-instruments
                         (initial-events recording)))))

(defn update-time-offset-wrapper [state time-offset]
  (-> state
      (update :recording audio-stuff2.recording/update-time-offset
              time-offset)
      refresh-cached-instruments))

(defn stop-playing-wrapper [{:keys [cached-instruments] :as state}]
  (-> state
      (update :recording audio-stuff2.recording/stop-playing)
      (assoc :instruments cached-instruments)
      refresh-overtone
      initialize-instruments))

(defn stop-playing-and-recording-audio [state]
  (when-let [path (overtone.core/recording-stop)]
    (println "New recording at " path))
  (stop-playing-wrapper state))

(defn start-playing-wrapper [state]
  (-> state
      stop-playing-wrapper
      (update :recording audio-stuff2.recording/start-playing)))

(defn play-and-record-wrapper [state]
  (-> state
      stop-playing-wrapper
      (update :recording audio-stuff2.recording/play-and-record)))

(defn clear-recording [{:keys [initial-instruments recording] :as state}]
  (-> state
      stop-playing-wrapper
      (assoc :recording (make-recording recording-play-fn
                                        (:path recording))
             :cached-instruments initial-instruments
             :instruments initial-instruments)
      initialize-instruments))

(defn load-recording-wrapper [state]
  (-> state
      clear-recording
      (update :recording audio-stuff2.recording/load-recording)))

(defn revert-recording-wrapper [state file-num]
  (-> state
      clear-recording
      (update :recording audio-stuff2.recording/revert-recording file-num)))

(defn save-recording-wrapper [{:keys [recording] :as state}]
  (audio-stuff2.recording/save-recording recording)
  state)

(defn remove-events [state min-time max-time pred]
  (update state :recording transduce-events
          (remove
            (fn [[[time _] event]]
              (and (> time min-time)
                   (< time max-time)
                   (pred event))))))

(defn remove-instrument [state min-time max-time removed-key]
  (remove-events state min-time max-time
                 (fn [[instrument-key & _]]
                   (= instrument-key removed-key))))

(defn expand-events [state start-time interval]
  (update state :recording transduce-events
          (map (fn [[[time g] event]]
                 [[(if (> time start-time) (+ time interval) time) g]
                  event]))))

(def default-keymap
  {\1 start-playing-wrapper
   \2 play-and-record-wrapper
   \3 stop-playing-and-recording-audio})

(defn instrument-keymap-fn [[key-char instrument-key]]
  [key-char
   (fn [state]
     (assoc state :selected-instrument instrument-key))])

(defn make-state-agent [instruments instrument-keymap selected-instrument path]
  (def state-agent (-> {:instruments         instruments
                        :initial-instruments instruments
                        :cached-instruments  instruments
                        :selected-instrument selected-instrument
                        :recording           (audio-stuff2.recording/load-recording
                                               (make-recording recording-play-fn path))
                        :keymap              (into default-keymap
                                                   (map instrument-keymap-fn)
                                                   instrument-keymap)}
                       initialize-instruments
                       agent))
  (set-error-mode! state-agent :continue)
  (set-error-handler! state-agent #(pst %2)))

(defn intern-updates [& bindings]
  (doseq [[name func] bindings]
    (intern *ns* name
            (fn [& args]
              (send-off state-agent
                        #(apply func % args))
              nil))))

(defn intern-fns [& bindings]
  (doseq [[name func] bindings]
    (intern *ns* name
            (fn [& args]
              (apply func @state-agent args)))))

(defn make-music [instruments
                  instrument-keymap
                  initial-instrument
                  path]
  (refresh-overtone)
  (make-state-agent
    instruments
    instrument-keymap
    initial-instrument path)
  (set-handlers
    state-agent
    input-event-handler)
  (intern-updates
    ['play start-playing-wrapper]
    ['stop stop-playing-and-recording-audio]
    ['rec play-and-record-wrapper]
    ['rec-wav (fn [state]
                (overtone.core/recording-start
                  (clojure.java.io/file
                    path (str (java.util.Date.) ".wav")))
                (start-playing-wrapper state))]
    ['load load-recording-wrapper]
    ['revert revert-recording-wrapper]
    ['save save-recording-wrapper]
    ['clear clear-recording]
    ['set-time update-time-offset-wrapper]
    ['remove-instrument remove-instrument]
    ['expand expand-events])
  (intern-fns
    ['get-time (fn [{:keys [recording]}]
                 (recording-time recording))]
    ['get-recording-info (fn [{:keys [recording]} k]
                           (recording k))]))

(defn shutdown []
  (when (bound? #'state-agent)
    (send-off state-agent stop-playing-wrapper))
  (close-window))