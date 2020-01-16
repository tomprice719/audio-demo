(ns audio-stuff2.core
  (:require [audio-stuff2.synths :refer [saw-keys]]
            [audio-stuff2.instruments.poly-instrument :refer [make-poly-instrument]]
            [audio-stuff2.scale-utils :refer [make-scale-vec add-scale-vec load-chords]]
            [audio-stuff2.overtone-utils :refer [refresh-overtone]]
            [audio-stuff2.breakpoints :refer [breakpoint]]
            [audio-stuff2.control :refer [make-music]]
            [debux.core :refer [dbg dbgn]]))


(defn make-instruments []
  {:keys
   (->
     (make-poly-instrument saw-keys :white-notes
                           "~/impulse-responses/left1.wav" 1.0 0.0
                           "~/impulse-responses/right1.wav" 1.0 0.0)
     (add-scale-vec (make-scale-vec (load-chords "/home/tom/audio-stuff2/chords"))))
   })

(defn on-refresh []
  (refresh-overtone)
  (make-music
    (make-instruments)
    :keys
    )
  )
