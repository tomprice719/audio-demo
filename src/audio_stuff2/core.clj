(ns audio-stuff2.core
  (:require [audio-stuff2.synths :refer [saw-keys square-keys wt-keys horn sine-mono resonator]]
            [audio-stuff2.instruments.poly-instrument
             :refer [make-poly-instrument make-wt-poly-instrument]]
            [audio-stuff2.instruments.mono-instrument :refer [make-mono-instrument add-resonator]]
            [audio-stuff2.scale-utils :refer [combination-chord-prog
                                              add-scale-prog
                                              equal-temperament
                                              update-scale]]
            [audio-stuff2.breakpoints :refer [breakpoint]]
            [audio-stuff2.control :refer [make-music]]
            [debux.core :refer [dbg dbgn]]))

(def chords
  [[400.0 1 4/5 4/8]
   [400.0 1 4/5 4/9]
   [400.0 1 4/5 4/11]
   [400.0 1 4/5 4/13]
   [400.0 1 4/5 4/9]
   [400.0 1 4/5 4/11]
   [400.0 1 4/5 4/13]
   [400.0 1/10]])

(def instruments
  {:saw-poly
   (->
     (make-poly-instrument saw-keys :all-notes
                           "~/impulse-responses/left1.wav" 1.0 0.0
                           "~/impulse-responses/right1.wav" 1.0 0.0)
     (update-scale (equal-temperament 100.0 128 50.0)))
   :woodwind
   (->
     (make-mono-instrument woodwind :white-notes
                           "~/impulse-responses/left1.wav" 1.0 0.0
                           "~/impulse-responses/right1.wav" 1.0 0.0)
     (add-resonator resonator)
     (add-scale-prog (combination-chord-prog 50 chords)))
   :wt-poly
   (->
     (make-wt-poly-instrument wt-keys :white-notes
                              "~/impulse-responses/left1.wav" 1.0 0.0
                              "~/impulse-responses/right1.wav" 1.0 0.0)
     (add-scale-prog (combination-chord-prog 50 chords)))
   })

(defn on-refresh []
  (make-music instruments
              {\q :saw-poly, \w :woodwind, \e :wt-poly}
              :saw-poly
              "/home/tom/new-recordings"))
