(ns audio-stuff2.overtone-utils
  (:require
    [overtone.core :refer :all]
    [overtone.sc.machinery.server.comms :refer [with-server-sync]]
    [overtone.sc.machinery.server.connection :refer [connection-status*]])
  (:import (org.jtransforms.fft DoubleFFT_1D)
           (java.util Random)))

(def notes-g)
(def effects-g)

;; Noise oscillator stuff

(def buffer-length 1024)
(def max-freq 18000)

(def fftObject (DoubleFFT_1D. buffer-length))

(def randomObject (Random.))

(defn noise-wavetable [freq]
  (println freq)
  (let [a (into-array Double/TYPE
                      (for [i (range (* 2 buffer-length))
                            :let [d (min i (- (* 2 buffer-length) i))]]
                        (if (and (> d 1)
                                 (< (* d freq) max-freq))
                          (/ (.nextGaussian randomObject) (Math/pow d 1))
                          0)))
        b (buffer (* 2 buffer-length))]
    (.complexForward fftObject a)
    (buffer-write-relay! b (signal->wavetable (take-nth 1.5 (seq a))))
    b))

(defn generate-wt-data [freq]
  [freq [(noise-wavetable freq)
         (noise-wavetable freq)
         (noise-wavetable freq)
         (noise-wavetable freq)]])

(def wt-data
  (map generate-wt-data (iterate (partial * 1.2) 40.0)))

(defn get-wt-data [freq]
  (let [[[f wavetables]]
        (filter (fn [[f _]] (> f freq)) wt-data)]
    (shuffle wavetables)))

;; Reverb / make-bus stuff

(def fft-size 512)

(defn num-partitions [ir-buffer]
  (let [partition-size (/ fft-size 2)
        size (num-frames ir-buffer)]
    (Math/ceil (/ size partition-size))))

(defn buf-size [ir-buffer]
  (* fft-size (num-partitions ir-buffer)))

(defn prepare-part-conv [src-buffer dest-buffer]
  (snd "/b_gen" (buffer-id dest-buffer) "PreparePartConv" (buffer-id src-buffer) fft-size))

(defn get-ir-spectrum [ir-location]
  (let [my-ir (load-sample ir-location)
        ir-spectrum (buffer (buf-size my-ir))]
    (prepare-part-conv my-ir ir-spectrum)
    ir-spectrum))

(defsynth reverb-synth [in-bus -1 out-bus -1 ir-spec -1 wet-gain 1 dry-gain 1]
          (let [sig (in:ar in-bus 1)
                reverb (part-conv (* sig wet-gain) fft-size ir-spec)
                ]
            (out out-bus (+ reverb (* sig dry-gain)))))

(defn make-bus [left-ir left-wet-gain left-dry-gain
                right-ir right-wet-gain right-dry-gain]
  (let [bus (audio-bus)
        left-ir-spec (with-server-sync #(get-ir-spectrum left-ir))
        right-ir-spec (with-server-sync #(get-ir-spectrum right-ir))]
    (println bus)
    (reverb-synth [:tail effects-g] bus 0 left-ir-spec left-wet-gain left-dry-gain)
    (reverb-synth [:tail effects-g] bus 1 right-ir-spec right-wet-gain right-dry-gain)
    bus))

;; Refresh

(defn refresh-overtone []
  (when (= @connection-status* :disconnected)
    (boot-external-server))
  (with-server-sync #(get-wt-data 2000.0))
  (clear-all)
  (def notes-g (group "notes"))
  (def effects-g (group "effects" :after notes-g)))
