(ns audio-stuff2.wavetables
  (:import (org.jtransforms.fft DoubleFFT_1D)
           (java.util Random))
  (:require [overtone.core :refer [buffer buffer-write-relay! signal->wavetable]]
            [overtone.sc.machinery.server.comms :refer [with-server-sync]]))

(defn normalize [new-max s]
  (let [s-max (reduce max s)]
    (map (partial * (/ new-max s-max)) s)))

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
(comment
  (defn get-wt-data [freq]
    [1 2 3 4]))

(def wt-data
  (map generate-wt-data (iterate (partial * 1.2) 40.0)))

(defn get-wt-data [freq]
  (let [[[f wavetables]]
        (filter (fn [[f _]] (> f freq)) wt-data)]
    (shuffle wavetables)))

;(def lpf-noise-seq
;  (normalize 1.0 (take-nth 2 (seq lpf-noise-array))))
;
;(println (count lpf-noise-seq))
;(buffer-write-relay! lpf-noise-buffer (signal->wavetable lpf-noise-seq))

