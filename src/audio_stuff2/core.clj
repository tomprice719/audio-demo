(ns audio-stuff2.core
  (:require [overtone.core :refer :all]
            [audio-stuff2.synths :refer [saw-keys]]
            [audio-stuff2.instrument-utils :refer :all]
            [audio-stuff2.input-events :refer [add-handlers]]
            [audio-stuff2.prepare-kernel :refer [get-ir-spectrum fft-size]]
            [overtone.sc.machinery.server.comms :refer [with-server-sync]])
  (:import (audio_stuff2.instrument_utils Poly-Instrument)))

(boot-external-server)

(defn simple-scale [freqs]
  (fn [note-num midi-pb]
    (freqs note-num)))

(def my-scale (simple-scale [100 200 300 400 500 600 700 800 900 1000 1100 1200 1300 1400 1500 1600]))
(def note-data (vec (repeat 50 nil)))

(def notes-g (group "notes"))
(def effects-g (group "effects" :after notes-g))
(def pos1-bus (audio-bus))

(defsynth reverb [in-bus -1 out-bus -1 ir-spec -1 gain 1]
          (let [sig    (in:ar in-bus 1)
                reverb (part-conv (* sig 0.1 gain) fft-size ir-spec)
                ]
            (out out-bus (delay-n reverb 0.01 0.01))))

(def left-ir-spec1 (with-server-sync #(get-ir-spectrum "~/impulse-responses/left1.wav")))
(def right-ir-spec1 (with-server-sync #(get-ir-spectrum "~/impulse-responses/right1.wav")))

(reverb [:tail effects-g] pos1-bus 0 left-ir-spec1 6.0)
(reverb [:tail effects-g] pos1-bus 1 right-ir-spec1 6.0)

(defn play-keys [freq _ velocity]
  (saw-keys [:tail notes-g] pos1-bus freq))

(defn stop-synth [synth]
  (node-control synth [:gate 0])
  (after-delay 10000 #(kill synth)))

(def key-instrument (Poly-Instrument. play-keys stop-synth note-data my-scale {} nil 1))

(add-handlers (instrument-updater standard-instrument-handler)
              {:instruments {:keys key-instrument} :current-instrument :keys})

(defn -main
  "I don't do a whole lot."
  [& args]
  (println "Hello, World!"))
