(ns audio-stuff2.core
  (:require [audio-stuff2.synths :refer :all]
            [audio-stuff2.instruments.poly-instrument
             :refer [make-poly-instrument make-wt-poly-instrument]]
            [audio-stuff2.instruments.mono-instrument :refer [make-mono-instrument add-resonator]]
            [audio-stuff2.scale-utils :refer [combination-chord-prog
                                              add-scale-prog]]
            [audio-stuff2.control :refer [make-music]]))

(def chords
  [[400.0 5/11 1 5/4 5/3]
   [400.0 5/13 1 5/4 5/3]])

(def chord-prog (combination-chord-prog 50 chords))

(def left-ir-file
  (clojure.java.io/file
    "impulse-responses" "left.wav"))

(def right-ir-file
  (clojure.java.io/file
    "impulse-responses" "right.wav"))

(def instruments
  {:woodwind-res
   (->
     (make-mono-instrument                                  ;make a monophonic instrument
       woodwind                                             ;what synth to use
       :typing                                              ;the input type. See instruments.message-generators for more information.
       left-ir-file 1.0 0.0                                 ;reverb parameters for each channel:
       right-ir-file 1.0 0.0)                               ;impulse response, wet volume, dry volume
     (add-resonator resonator)                              ;play a sustained sound on top of each note
     (add-scale-prog chord-prog)                            ;specify the scale progression
     (assoc :default-velocity 0.5 :default-mod-wheel 0.5))   ;velocity and modwheel values used when not supported by input type

   :woodwind-nores
   (->
     (make-mono-instrument woodwind :typing
                           left-ir-file 1.0 0.0
                           right-ir-file 1.0 0.0)
     (add-scale-prog chord-prog)
     (assoc :default-velocity 0.5 :default-mod-wheel 0.5))

   :keys
   (->
     (make-wt-poly-instrument keys :typing
                              left-ir-file 1.0 0.0
                              right-ir-file 1.0 0.0)
     (add-scale-prog chord-prog)
     (assoc :default-velocity 0.5 :default-mod-wheel 0.5))

   :octave-keys
   (->
     (make-wt-poly-instrument octave-keys :typing
                              left-ir-file 1.0 0.0
                              right-ir-file 1.0 0.0)
     (add-scale-prog chord-prog)
     (assoc :default-velocity 0.5 :default-mod-wheel 0.5))
   })

(defn on-refresh []
  (make-music instruments
              {\5 :woodwind-res, \6 :woodwind-nores,
               \7 :keys, \8 :octave-keys}
              :keys
              "saved"))
