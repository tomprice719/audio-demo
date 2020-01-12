(ns audio-stuff2.instruments.impl
  (:require [audio-stuff2.overtone-utils :refer [notes-g effects-g get-ir-spectrum reverb-synth]]
            [audio-stuff2.input-events :refer [event-data]]
            [audio-stuff2.scale-utils :refer [next-scale]]
            [overtone.core :refer :all]))

(defmulti note-on (fn [inst note-num velocity] (:type inst)))
(defmulti note-off (fn [inst note-num] (:type inst)))

(defn bent-pitch [freq pb-value] (* freq (+ 1 (* pb-value 0.1))))

(defn pitch-bend [{:keys [current-note-num] :as inst} pb-value]
  (when-let [{:keys [freq freq-bus]}
             (get-in inst [:note-data current-note-num])]
    (control-bus-set! freq-bus (bent-pitch freq pb-value)))
  (assoc inst :pitch-bend pb-value))

(defn mod-wheel [{:keys [mod-wheel-bus] :as inst} mod-wheel-value]
  (control-bus-set! mod-wheel-bus mod-wheel-value)
  inst)

(defn standard-instrument-message-fn [state]
  (case (:event event-data)
    :white-note-on [[(:current-instrument state) :note-on (:white-note-num event-data) (:velocity event-data)]]
    :white-note-off [[(:current-instrument state) :note-off (:white-note-num event-data)]]
    :black-note-on [[(:current-instrument state) :next-scale]]
    :pitch-bend [[(:current-instrument state) :pitch-bend (:pb-value event-data)]]
    :mod-wheel [[(:current-instrument state) :mod-wheel (:mod-wheel-value event-data)]]
    []))

(def instrument-message-map {:note-on    note-on
                             :note-off   note-off
                             :next-scale next-scale
                             :pitch-bend pitch-bend
                             :mod-wheel  mod-wheel})