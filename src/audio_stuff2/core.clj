(ns audio-stuff2.core
  (:require [overtone.core :refer :all]
            [audio-stuff2.wavetables :refer [get-wt-data]]
            [audio-stuff2.synths :refer [saw-keys]]
            [audio-stuff2.instrument-utils :refer :all]
            [audio-stuff2.input-events :refer [set-handlers]]
            [audio-stuff2.scale-utils :refer [make-scale-vec add-scale-vec load-chords num-notes]]
            [audio-stuff2.reverb :refer [get-ir-spectrum fft-size reverb-synth]]
            [audio-stuff2.breakpoints :refer [breakpoint]]
            [overtone.sc.machinery.server.comms :refer [with-server-sync]]
            [overtone.sc.machinery.server.connection :refer [connection-status*]]
            [debux.core :refer [dbg dbgn]]))


(defn add-instruments [state]
  (assoc state :instruments
               {:keys

                (->
                  (make-poly-instrument saw-keys
                                        "~/impulse-responses/left1.wav" 1.0 0.0
                                        "~/impulse-responses/right1.wav" 1.0 0.0)
                  (add-scale-vec (make-scale-vec (load-chords "/home/tom/audio-stuff2/chords"))))
                }))

(defn on-refresh []
  (when (= @connection-status* :disconnected)
    (boot-external-server)
    (with-server-sync #(get-wt-data 2000.0)))
  (stop-all)
  (-> {:current-instrument :keys}
      add-instruments
      (set-handlers (instrument-updater standard-instrument-handler))))
