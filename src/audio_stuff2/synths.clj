(ns audio-stuff2.synths
  (:require [overtone.core :refer :all]))

(defcgen log-interpolate [fraction low high]
         (:ar (* low (pow (/ high low) fraction)))
         (:kr (* low (pow (/ high low) fraction))))

(defcgen noise-osc [freq phase1 phase2 wt1 wt2 wt3 wt4]
         (:ar
           (+ (* (sin-osc 0.3 phase1)
                    (osc wt1 freq))
                 (* (sin-osc 0.3 (+ phase1 1.57))
                    (osc wt2 freq))
                 (* (sin-osc 0.2 phase2)
                    (osc wt3 freq))
                 (* (sin-osc 0.2 (+ phase2 1.57))
                    (osc wt4 freq)))))

(defcgen my-comb "" [in freq]
         (:ar (- in (* 1.0 (delay-n in 0.1 (/ 1.0 freq))))))

(defsynth plain-saw [out-bus -1 freq-bus -1 mod-wheel-bus -1 velocity 1.0
                     wt1 -1 wt2 -1 wt3 -1 wt4 -1 gate 1]
          (let [freq (lpf (in:kr freq-bus) 100)
                mod-wheel-value (lag (in:kr mod-wheel-bus))]
            (out out-bus
                 (my-comb
                   (* (+ (saw freq)
                         (* 2.0 (sin-osc freq))
                         (* 0.5 (noise-osc freq
                                           (t-rand:kr 0.0 6.28)
                                           (t-rand:kr 0.0 6.28)
                                           wt1 wt2 wt3 wt4)))
                      0.05
                      (env-gen (adsr 0.5 0.0 1.0 0.1) :gate gate))
                   2000))))

(defsynth plain-square [out-bus -1 freq-bus -1 mod-wheel-bus -1 velocity 1.0 gate 1]
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
                   (* (+
                        (*
                          (noise-osc freq
                                     (t-rand:kr 0.0 6.28)
                                     (t-rand:kr 0.0 6.28)
                                     wt1 wt2 wt3 wt4)
                          0.5
                          (env-gen (adsr 0.0 3.0 0.0 3.0 :curve 0) :gate gate :action FREE))
                        (* 5.0
                           (sin-osc freq)
                           (env-gen (adsr 0.0 3.0 0.0 3.0 :curve -2) :gate gate :action FREE)))

                      0.02
                      (pow (/ 400 freq) 0.5)
                      (log-interpolate velocity 0.6 4))
                   100))))

(defsynth octave-keys [out-bus -1 freq-bus -1 mod-wheel-bus -1 velocity 1.0
                       wt1 -1 wt2 -1 wt3 -1 wt4 -1 gate 1]
          (out out-bus
               (let [freq (lpf (in:kr freq-bus) 100)]
                 (hpf
                   (* (+
                        (noise-osc freq
                                   (t-rand:kr 0.0 6.28)
                                   (t-rand:kr 0.0 6.28)
                                   wt1 wt2 wt3 wt4)
                        (* 3 (sin-osc freq))
                        (* 7 (sin-osc (* freq 0.5))))
                      (env-gen (adsr 0.0 6.0 0.0 6.0 :curve -6) :gate gate :action FREE)
                      0.025
                      (log-interpolate velocity 0.6 4))
                   100))))

(defsynth woodwind [out-bus -1 freq-bus -1 modwheel-bus -1 gate 1]
          (let [freq (lpf (in:kr freq-bus) 100)]
            (out out-bus
                 (* 0.2
                    (hpf
                      (+
                        (comb-l (* (bpf (white-noise)
                                        (* (lag (in:kr freq-bus)) 4)
                                        0.5)
                                   0.2
                                   (env-gen (adsr 0.01 0.5 0.1 0.01) :gate gate))
                                0.2 (/ 0.5 (in:kr freq-bus)) -0.2)
                        (+
                          (* 2.0
                             (env-gen (adsr 0.2 0.0 1.0 0.01 :curve 0) :gate gate)
                             (tanh
                               (* 3.0 (sin-osc freq)
                                  (log-interpolate (lag (in:kr modwheel-bus)) 0.2 1.0))))
                          (* (env-gen (adsr 0.2 0.0 1.0 0.01 :curve 0) :gate gate)
                             (* 1.0 (saw freq))
                             (log-interpolate (lag (in:kr modwheel-bus)) 0.2 0.5))))
                      50)))))

(defsynth sine-mono [out-bus -1 freq-bus -1 modwheel-bus -1 velocity 1.0 gate 1]
          (let [freq (lpf (in:kr freq-bus) 100)]
            (out out-bus
                 (* 0.2
                    (sin-osc freq)
                    (env-gen (adsr 0.01 0.0 1.0 0.05 :curve 0) :gate gate)))))

(defsynth additive [out-bus -1 freq-bus -1 modwheel-bus -1 velocity 1.0 gate 1]
          (let [freq (* 1/2 (lpf (in:kr freq-bus) 100))]
            (out out-bus
                 (* 0.1
                    (+
                      (sin-osc freq)
                      (sin-osc (* 2 freq)))
                    (env-gen (adsr 0.01 3.0 0.0 3.0 :curve -2) :gate gate)))))

(defsynth resonator [out-bus -1 freq -1 velocity 1.0
                     wt1 -1 wt2 -1 wt3 -1 wt4 -1 gate 1]
          (out out-bus
               (* (+
                    (noise-osc freq
                               (t-rand:kr 0.0 6.28)
                               (t-rand:kr 0.0 6.28)
                               wt1 wt2 wt3 wt4))
                  (env-gen (adsr 0.0 3.0 0.0 3.0 :curve 0) :gate gate :action FREE)
                  0.02
                  (log-interpolate velocity 0.6 4))))
