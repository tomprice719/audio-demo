(ns audio-demo.core
  (:require [audio-demo.synths :refer :all]
            [audio-demo.instruments.poly-instrument
             :refer [make-poly-instrument make-wt-poly-instrument]]
            [audio-demo.instruments.mono-instrument :refer [make-mono-instrument add-resonator]]
            [audio-demo.scale-utils :refer [combination-scale-prog
                                            add-scale-prog]]
            [audio-demo.control :refer [make-music]]))

(def scales
  "The sequence of scales used.
  Each row represents one scale. Each scale is a union of harmonic series.
  The first number of each row (400.0 here) is the reference frequency.
  The fundamental frequency of each harmonic series included in the scale is specified as a ratio
  of the reference frequency, e.g. 400 * 5/ 11 = 181.8 Hz fundamental is included in the first scale."
  [[400.0 5/11 1 5/4 5/3]
   [400.0 5/13 1 5/4 5/3]])

(def scale-prog (combination-scale-prog 50 scales))

(def left-ir-file
  (clojure.java.io/file
    "impulse-responses" "left.wav"))

(def right-ir-file
  (clojure.java.io/file
    "impulse-responses" "right.wav"))

(def instruments
  {
   :woodwind-res
   (->
     (make-mono-instrument                                  ;make a monophonic instrument
       woodwind                                             ;what synth to use
       :typing                                              ;the input type. See instruments.message-generators for more information.
       left-ir-file 1.0 0.0                                 ;reverb parameters for each channel:
       right-ir-file 1.0 0.0)                               ;impulse response, wet volume, dry volume
     (add-resonator resonator)                              ;play a sustained sound on top of each note
     (add-scale-prog scale-prog)                            ;specify the scale progression
     (assoc :default-velocity 0.5 :default-mod-wheel 0.5))  ;velocity and modwheel values used when not supported by input type

   :woodwind-nores
   (->
     (make-mono-instrument woodwind :typing
                           left-ir-file 1.0 0.0
                           right-ir-file 1.0 0.0)
     (add-scale-prog scale-prog)
     (assoc :default-velocity 0.5 :default-mod-wheel 0.5))

   :keys
   (->
     (make-wt-poly-instrument keys :typing
                              left-ir-file 1.0 0.0
                              right-ir-file 1.0 0.0)
     (add-scale-prog scale-prog)
     (assoc :default-velocity 0.5 :default-mod-wheel 0.5))

   :octave-keys
   (->
     (make-wt-poly-instrument octave-keys :typing
                              left-ir-file 1.0 0.0
                              right-ir-file 1.0 0.0)
     (add-scale-prog scale-prog)
     (assoc :default-velocity 0.5 :default-mod-wheel 0.5))
   })

(defn on-refresh []
  (make-music instruments
              {\5 :woodwind-res, \6 :woodwind-nores,
               \7 :keys, \8 :octave-keys}
              :woodwind-res
              "saved"))
