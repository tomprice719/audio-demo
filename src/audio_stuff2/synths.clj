(ns audio-stuff2.synths
  (:require [overtone.core :refer :all]))

(defcgen log-interpolate [fraction low high]
         (:ar (* low (pow (/ high low) fraction)))
         (:kr (* low (pow (/ high low) fraction))))

(defcgen noise-osc [freq phase1 phase2 wt1 wt2 wt3 wt4]
         (:ar (+ (* (sin-osc 0.3 phase1)
                    (osc wt1 freq))
                 (* (sin-osc 0.3 (+ phase1 1.57))
                    (osc wt2 freq))
                 (* (sin-osc 0.2 phase2)
                    (osc wt3 freq))
                 (* (sin-osc 0.2 (+ phase2 1.57))
                    (osc wt4 freq)))))

(defsynth saw-keys [out-bus -1 freq-bus -1 mod-wheel-bus -1 velocity 1.0 gate 1]
          (let [freq (lpf (in:kr freq-bus) 100)
                mod-wheel-value (lag (in:kr mod-wheel-bus))]
            (out out-bus
                 (* (lpf (saw freq)
                         (+ 1000 (* 4000 (env-gen (perc 0.00 3.0)))))
                    (+ 0.3 mod-wheel-value)
                    0.1
                    (env-gen (adsr 0.01 0.0 1.0 0.1) :gate gate)))))

(defsynth square-keys [out-bus -1 freq-bus -1 mod-wheel-bus -1 velocity 1.0 gate 1]
          (let [freq (lpf (in:kr freq-bus) 100)
                mod-wheel-value (lag (in:kr mod-wheel-bus))]
            (out out-bus
                 (* (lpf (square freq)
                         (+ 1000 (* 4000 (env-gen (perc 0.00 3.0)))))
                    (+ 0.3 mod-wheel-value)
                    0.1
                    (env-gen (adsr 0.01 0.0 1.0 0.1) :gate gate)))))

(defsynth wt-keys [out-bus -1 freq-bus -1 mod-wheel-bus -1 velocity 1.0
                   wt1 -1 wt2 -1 wt3 -1 wt4 -1 gate 1]
          (out out-bus
               (let [freq (lpf (in:kr freq-bus) 100)]
                 (hpf
                   (lpf
                     (* (+
                          (noise-osc freq
                                     (t-rand:kr 0.0 6.28)
                                     (t-rand:kr 0.0 6.28)
                                     wt1 wt2 wt3 wt4)
                          (* 5 (sin-osc freq))
                          (* 5 (sin-osc (* freq 0.5))))
                        (env-gen (adsr 0.0 6.0 0.0 6.0 :curve -6) :gate gate :action FREE)
                        0.02
                        (log-interpolate velocity 0.6 4))
                     2000)
                   100))))
