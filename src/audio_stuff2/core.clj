(ns audio-stuff2.core
  (:require [overtone.core :refer :all]
            [audio-stuff2.synths :refer [saw-keys]]
            [audio-stuff2.instrument-utils :refer :all]
            [audio-stuff2.input-events :refer [add-handlers]]
            [audio-stuff2.scale-utils :refer [make-freq-fn-vec add-freq-fn-vec load-chords]]
            [audio-stuff2.reverb :refer [get-ir-spectrum fft-size reverb-synth]]
            [overtone.sc.machinery.server.comms :refer [with-server-sync]]
            [overtone.sc.machinery.server.connection :refer [connection-status*]]
            [overtone.sc.machinery.allocator :refer [clear-ids]])
  (:import (audio_stuff2.instrument_utils Poly-Instrument)))

(def notes-g)
(def effects-g)

(defn make-bus [left-ir left-wet-gain left-dry-gain
                right-ir right-wet-gain right-dry-gain]
  (let [bus (audio-bus)
        left-ir-spec (with-server-sync #(get-ir-spectrum left-ir))
        right-ir-spec (with-server-sync #(get-ir-spectrum right-ir))]
    (println bus)
    (reverb-synth [:tail effects-g] bus 0 left-ir-spec left-wet-gain left-dry-gain)
    (reverb-synth [:tail effects-g] bus 1 right-ir-spec right-wet-gain right-dry-gain)
    bus))

(def note-data (vec (repeat 50 nil)))

(defn play-fn [synth bus]
  (fn [freq _ velocity]
    (synth [:tail notes-g] bus freq)))

(defn stop-synth [synth]
  (node-control synth [:gate 0])
  (after-delay 10001 #(kill synth)))


(defn make-instruments []
  {:keys
   (add-freq-fn-vec (Poly-Instrument. (play-fn saw-keys
                                               (make-bus "~/impulse-responses/left1.wav" 1.0 0.0
                                                         "~/impulse-responses/right1.wav" 1.0 0.0))
                                      stop-synth note-data nil {} nil 1)
                    (make-freq-fn-vec (load-chords "/home/tom/audio-stuff2/chords") 0.0))})

(defn on-refresh []
  (when (= @connection-status* :disconnected)
    (boot-external-server))
  (clear-all)
  (clear-ids :audio-bus)
  (clear-ids :control-bus)
  (def notes-g (group "notes"))
  (def effects-g (group "effects" :after notes-g))
  (add-handlers (instrument-updater standard-instrument-handler)
                {:instruments (make-instruments) :current-instrument :keys}))
