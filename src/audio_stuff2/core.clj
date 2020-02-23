(ns audio-stuff2.core
  (:require [audio-stuff2.synths :refer :all]
            [audio-stuff2.instruments.poly-instrument
             :refer [make-poly-instrument make-wt-poly-instrument]]
            [audio-stuff2.instruments.mono-instrument :refer [make-mono-instrument add-resonator]]
            [audio-stuff2.scale-utils :refer [combination-chord-prog
                                              add-scale-prog
                                              equal-temperament
                                              update-scale
                                              cents->ratio]]
            [audio-stuff2.control :refer [make-music]]))

(def chords
  [[400.0 5/11 1 5/4 5/3]
   [400.0 5/13 1 5/4 5/3]])

(def chords2
  [[400.0 1 5/4 5/3]])

(comment
  (def chords
    [[200.0 1 11/4 11/3]
     [200.0 1 (cents->ratio 1472.73) (cents->ratio 1745.46)]
     [200.0 1 7/4 7/3]
     [200.0 1 (cents->ratio 1472.73) (cents->ratio 1745.46)]]))

(comment
  (def chords
    [[400.0 1 4/5 4/8]
     [400.0 1 4/5 4/9]
     [400.0 1 4/5 4/11]
     [400.0 1 4/5 4/13]
     [400.0 1 4/5 4/9]
     [400.0 1 4/5 4/11]
     [400.0 1 4/5 4/13]
     [400.0 1/10]]))


(def chord-prog (combination-chord-prog 50 chords))

(def chord-prog2 (combination-chord-prog 50 chords2))

(def left-ir-file
  (clojure.java.io/file
    "impulse-responses" "left.wav"))

(def right-ir-file
  (clojure.java.io/file
    "impulse-responses" "right.wav"))

(def instruments
  {:woodwind-nores
   (->
     (make-mono-instrument woodwind :white-notes
                           left-ir-file 1.0 0.0
                           right-ir-file 1.0 0.0)
     (add-resonator resonator)
     (add-scale-prog chord-prog))

   :woodwind-res
   (->
     (make-mono-instrument woodwind :white-notes
                           left-ir-file 1.0 0.0
                           right-ir-file 1.0 0.0)
     (add-resonator resonator)
     (add-scale-prog chord-prog))

   :keys
   (->
     (make-wt-poly-instrument wt-keys :white-notes
                              left-ir-file 1.0 0.0
                              right-ir-file 1.0 0.0)
     (add-scale-prog chord-prog))
   :keys2
   (->
     (make-wt-poly-instrument wt-keys :white-notes
                              left-ir-file 1.0 0.0
                              right-ir-file 1.0 0.0)
     (add-scale-prog chord-prog2))
   :octave-keys
   (->
     (make-wt-poly-instrument octave-keys :white-notes
                              left-ir-file 1.0 0.0
                              right-ir-file 1.0 0.0)
     (add-scale-prog chord-prog))
   :additive
   (->
     (make-poly-instrument additive :white-notes
                           left-ir-file 1.0 0.0
                           right-ir-file 1.0 0.0)
     (add-scale-prog chord-prog))
   })

;:12edo-keys
;(->
;  (make-wt-poly-instrument wt-keys :all-notes
;                           "~/impulse-responses/left1.wav" 1.0 0.0
;                           "~/impulse-responses/right1.wav" 1.0 0.0)
;  (update-scale (equal-temperament 100.0 128 50.0)))

(defn on-refresh []
  (make-music instruments
              {\q :woodwind-nores, \w :woodwind-res,
               \a :keys, \s :octave-keys, \d :keys2}
              :keys
              "saved"))
