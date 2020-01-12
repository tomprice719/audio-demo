(ns audio-stuff2.instruments.poly-instrument
  (:require [overtone.core :refer :all]
            [audio-stuff2.overtone-utils :refer [notes-g effects-g make-bus]]
            [audio-stuff2.instruments.impl :refer [note-on note-off bent-pitch]]
            [audio-stuff2.scale-utils :refer [num-notes]]))

(defn start-synth [{:keys [out-bus mod-wheel-bus synth]}
                   {:keys [freq freq-bus]}
                   pb-value
                   velocity]
  (control-bus-set! freq-bus (bent-pitch freq pb-value))
  (synth [:tail notes-g] out-bus freq-bus mod-wheel-bus))

(defmethod note-on :poly-instrument [{:keys [pitch-bend note-data] :as inst} note-num velocity]
  (if-let [d (and (not (get-in note-data [note-num :synth])) (get note-data note-num))]
    (-> inst
        (assoc-in [:note-data note-num :synth]
                  (start-synth inst d pitch-bend velocity))
        (assoc :current-note-num note-num))
    inst))

(defn stop-synth [synth]
  (node-control synth [:gate 0])
  (after-delay 10000 #(kill synth)))

(defmethod note-off :poly-instrument [{:keys [current-note-num] :as inst} note-num]
  (if-let [synth (get-in inst [:note-data note-num :synth])]
    (do (stop-synth synth)
        (as-> inst this2
              (update-in inst [:note-data note-num] dissoc :synth)
              (if (= note-num current-note-num)
                (dissoc this2 :current-note-num)
                this2)))
    inst))

(defn make-poly-instrument [synth & bus-args]
  {:type :poly-instrument
   :synth         synth
   :out-bus       (apply make-bus bus-args)
   :note-data     (mapv (partial hash-map :freq-bus)
                        (repeatedly num-notes control-bus))
   :mod-wheel-bus (control-bus)
   :pitch-bend    0})

