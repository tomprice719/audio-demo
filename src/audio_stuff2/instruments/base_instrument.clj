(ns audio-stuff2.instruments.base-instrument
  (:require [audio-stuff2.overtone-utils :refer [notes-g effects-g get-ir-spectrum reverb-synth]]
            [audio-stuff2.scale-utils :refer [next-scale]]
            [overtone.core :refer :all]))

(def ^:dynamic audible true)

(defmulti note-on (fn [inst note-num velocity] (:type inst)))
(defmulti note-off (fn [inst note-num] (:type inst)))
(defmulti pitch-bend (fn [inst pb-value] (:type inst)))
(defmulti initialize :type)

(defn bent-pitch [freq pb-value] (* freq (+ 1 (* pb-value 0.1))))

(defn mod-wheel [{:keys [mod-wheel-bus] :as inst} mod-wheel-value]
  (when audible
    (control-bus-set! mod-wheel-bus mod-wheel-value))
  (assoc inst :mod-wheel-value mod-wheel-value))

(def message-handlers {:note-on    note-on
                       :note-off   note-off
                       :next-scale next-scale
                       :pitch-bend pitch-bend
                       :mod-wheel  mod-wheel})