(ns audio-stuff2.instrument-utils
  (:require [audio-stuff2.input-events :refer [event-data]]))

(defprotocol Instrument
  (note-on [this note-num velocity])
  (note-off [this note-num]))

;play-note-fn must also set the frequency bus
;modwheel bus can be incorporated in play-note-fn as a closure
(defrecord Poly-Instrument
  [play-note-fn stop-note-fn note-data freq-fn notes current-note-num pitch-bend]
  Instrument
  (note-on [this note-num velocity]
    (let [freq (freq-fn note-num pitch-bend)]
      (if (and freq (not (notes note-num)))
        (-> this
            (assoc-in [:notes note-num]
                      (play-note-fn freq (note-data note-num) velocity))
            (assoc :current-note-num note-num))
        this)))
  (note-off [this note-num]
    (if-let [synth (notes note-num)]
      (do (stop-note-fn synth)
          (as-> this this2
                (update this :notes dissoc note-num)
                (if (= note-num current-note-num)
                  (assoc this2 :current-note-num nil)
                  this2)))
      this)))

(defrecord Mono-Instrument
  [play-note-fn stop-note-fn note-data freq-fn current-note-num pitch-bend]
  Instrument
  (note-on [this note-num velocity]
    (if-let [freq (freq-fn note-num pitch-bend)]
      (do (play-note-fn freq (note-data note-num) velocity)
          (assoc this :current-note-num note-num))
      this))
  (note-off [this note-num]
    (if (= current-note-num note-num)
      (do (stop-note-fn)
          (assoc this current-note-num nil))
      this)))

(defn next-freq-fn [{:keys [freq-fn-vec freq-fn-index] :as inst}]
  (let [new-index (mod (inc freq-fn-index) (count freq-fn-vec))]
    (assoc inst :freq-fn (freq-fn-vec new-index)
                :freq-fn-index new-index)))

(def instrument-fn-map {:note-on note-on
                        :note-off note-off
                        :next-freq-fn next-freq-fn})

(defn update-instrument [[fn-key & args] instrument]
  (apply (instrument-fn-map fn-key) instrument args))

(defn consume-instrument-message [state [instrument-key & message]]
  (update-in state [:instruments instrument-key] (partial update-instrument message)))

(defn instrument-updater [instrument-handlers]
  (fn [state]
    (reduce consume-instrument-message state (instrument-handlers state))))

(defn standard-instrument-handler [state]
  (case (:event event-data)
    :white-note-on [[(:current-instrument state) :note-on (:white-note-num event-data) (:velocity event-data)]]
    :white-note-off [[(:current-instrument state) :note-off (:white-note-num event-data)]]
    :black-note-on [[(:current-instrument state) :next-freq-fn]]
    []))

