(ns audio-stuff2.instruments.mono-instrument
  (:require
    [overtone.core :refer :all]
    [audio-stuff2.overtone-utils :refer [notes-g effects-g make-bus get-wt-data managed-control-bus]]
    [audio-stuff2.instruments.base-instrument :refer [note-on
                                                      note-off
                                                      bent-pitch
                                                      pitch-bend
                                                      initialize
                                                      audible]]))

(defn change-note [{:keys [synth synth-fn resonator-fn
                           freq-bus pitch-bend mod-wheel-bus out-bus] :as inst}
                   {:keys [freq]}
                   velocity]
  (when resonator-fn
    (let [[wt1 wt2 wt3 wt4] (get-wt-data freq)
          resonator (resonator-fn [:tail notes-g]
                                  out-bus freq velocity
                                  wt1 wt2 wt3 wt4)]
      (after-delay 10000 #(kill resonator))))
  (control-bus-set! freq-bus (bent-pitch freq pitch-bend))
  (if synth
    inst
    (assoc inst :synth (synth-fn [:tail notes-g] out-bus freq-bus mod-wheel-bus))))

(defmethod note-on :mono-instrument [{:keys [note-data] :as inst}
                                     note-num
                                     velocity]
  (if-let [d (get note-data note-num)]
    (-> inst
        (change-note d velocity)
        (assoc :current-note-num note-num))
    inst))

(defn stop-synth [{:keys [synth] :as inst}]
  (node-control synth [:gate 0])
  (after-delay 10000 #(kill synth))
  (dissoc inst :synth))

(defmethod note-off :mono-instrument [{:keys [current-note-num] :as inst} note-num]
  (if (and audible (= current-note-num note-num))
    (stop-synth inst)
    inst))

(defmethod pitch-bend :mono-instrument [{:keys [current-note-num freq-bus] :as inst} pb-value]
  (when-let [{:keys [freq]}
             (and audible
                  (get-in inst [:note-data current-note-num]))]
    (control-bus-set! freq-bus (bent-pitch freq pb-value)))
  (assoc inst :pitch-bend pb-value))

(defmethod initialize :mono-instrument [{:keys [bus-args mod-wheel-value] :as instrument}]
  (assoc instrument :out-bus (apply make-bus bus-args)
                    :mod-wheel-bus (doto (managed-control-bus) (control-bus-set! mod-wheel-value))
                    :freq-bus (managed-control-bus)))

(defn make-mono-instrument [synth-fn input-type & bus-args]
  {:type            :mono-instrument
   :synth-fn        synth-fn
   :input-type      input-type
   :bus-args        bus-args
   :pitch-bend      0
   :mod-wheel-value 0
   :note-data       (repeat {})})

(defn add-resonator [inst resonator-fn]
  (assoc inst :resonator-fn resonator-fn))