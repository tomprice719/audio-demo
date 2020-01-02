(ns audio-stuff2.synths
  (:require [overtone.core :refer :all]))

(defsynth saw-keys [out-bus -1 freq 400 gate 1]
          (out out-bus
               (* (lpf (saw freq)
                       (+ 1000 (* 4000 (env-gen (perc 0.00 3.0)))))
                  ;(modwheel)
                  0.1
                  (env-gen (adsr 0.01 0.0 1.0 0.1) :gate gate))))