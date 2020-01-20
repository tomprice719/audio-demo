(ns audio-stuff2.core
  (:require [audio-stuff2.synths :refer [saw-keys square-keys]]
            [audio-stuff2.instruments.poly-instrument :refer [make-poly-instrument]]
            [audio-stuff2.scale-utils :refer [combination-chord-prog add-scale-prog]]
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
  {:saw-keys
   (->
     (make-poly-instrument saw-keys :white-notes
                           "~/impulse-responses/left1.wav" 1.0 0.0
                           "~/impulse-responses/right1.wav" 1.0 0.0)
     (add-scale-prog (combination-chord-prog 50 chords)))
   :square-keys
   (->
     (make-poly-instrument square-keys :white-notes
                           "~/impulse-responses/left1.wav" 1.0 0.0
                           "~/impulse-responses/right1.wav" 1.0 0.0)
     (add-scale-prog (combination-chord-prog 50 chords)))
   })

(defn on-refresh []
  (make-music instruments
              {\q :saw-keys, \w :square-keys}
              :saw-keys
              "/home/tom/new-recordings"))
