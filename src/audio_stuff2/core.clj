(ns audio-stuff2.core
  (:require [overtone.core :refer :all]
            [audio-stuff2.wavetables :refer [get-wt-data]]
            [audio-stuff2.synths :refer [saw-keys]]
            [audio-stuff2.instrument-utils :refer :all]
            [audio-stuff2.input-events :refer [set-handlers]]
            [audio-stuff2.scale-utils :refer [make-scale-vec add-scale-vec load-chords num-notes]]
            [audio-stuff2.reverb :refer [get-ir-spectrum fft-size reverb-synth]]
            [overtone.sc.machinery.server.comms :refer [with-server-sync]]
            [overtone.sc.machinery.server.connection :refer [connection-status*]]
            [overtone.sc.machinery.allocator :refer [clear-ids]]
            [audio-stuff2.debug :refer [show]])
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

(defn play-fn [synth out-bus modwheel-bus]
  (fn [[freq freq-bus] pitch-bend velocity]
    (control-bus-set! freq-bus freq)
    [(synth [:tail notes-g] out-bus freq-bus modwheel-bus)
     freq
     freq-bus]))

(defn play-fn2 [synth bus]
  (fn [freq freq-bus velocity]
    (let [[wt1 wt2 wt3 wt4] (get-wt-data freq)]
      (synth [:tail notes-g] bus freq wt1 wt2 wt3 wt4))))

(defn stop-synth [[synth freq freq-bus]]
  (node-control synth [:gate 0])
  (after-delay 10000 #(kill synth)))

(defn add-synth [inst synth & bus-args]
  (let [modwheel-bus (control-bus)]
    (assoc inst
      :play-note-fn (play-fn synth (apply make-bus bus-args) modwheel-bus)
      :stop-note-fn stop-synth
      :freq-busses (vec (repeatedly num-notes control-bus))
      :modwheel-bus modwheel-bus)))


(defn add-instruments [state]
  (assoc state :instruments
               {:keys
                (make-poly-instrument
                  (-> {}
                      (add-synth saw-keys
                                 "~/impulse-responses/left1.wav" 1.0 0.0
                                 "~/impulse-responses/right1.wav" 1.0 0.0)
                      (add-scale-vec (make-scale-vec (load-chords "/home/tom/audio-stuff2/chords")))
                      (add-note-data)))
                }))

(defn on-refresh []
  (when (= @connection-status* :disconnected)
    (boot-external-server)
    (with-server-sync #(get-wt-data 2000.0)))
  (clear-all)
  ;(clear-ids :audio-bus) NOT SAFE
  ;(clear-ids :control-bus)
  (def notes-g (group "notes"))
  (def effects-g (group "effects" :after notes-g))
  (-> {:current-instrument :keys}
      add-instruments
      (set-handlers (instrument-updater standard-instrument-handler))))
