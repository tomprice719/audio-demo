(ns audio-stuff2.instrument-utils
  (:require [audio-stuff2.input-events :refer [event-data]]
            [audio-stuff2.debug :refer [show labeller]]
            [overtone.core :refer :all]))

(defprotocol Instrument
  (note-on [this note-num velocity])
  (note-off [this note-num]))

(defrecord Poly-Instrument
  [play-note-fn stop-note-fn note-data notes current-note-num pitch-bend]
  Instrument
  (note-on [this note-num velocity]
    (if-let [d (and (not (notes note-num)) (get note-data note-num))]
      (-> this
          (assoc-in [:notes note-num]
                    (play-note-fn d pitch-bend velocity))
          (assoc :current-note-num note-num))
      this))
  (note-off [this note-num]
    (if-let [synth (notes note-num)]
      (do (stop-note-fn synth)
          (as-> this this2
                (update this :notes dissoc note-num)
                (if (= note-num current-note-num)
                  (assoc this2 :current-note-num nil)
                  this2)))
      this)))

(defn make-poly-instrument [field-map]
  (map->Poly-Instrument (merge {:notes {} :current-note-num nil :pitch-bend 0}
                               field-map)))

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

(defn pitch-bend [{:keys [current-note-num pitch-bend-fn] :as inst} pb-value]
  (when-let [[synth freq freq-bus]
             (get-in inst [:notes current-note-num])]
    (control-bus-set! freq-bus (pitch-bend-fn freq pb-value)))
  (assoc inst :pitch-bend pb-value))

(defn mod-wheel [{:keys [mod-wheel-bus] :as inst} mod-wheel-value]
  (control-bus-set! mod-wheel-bus mod-wheel-value)
  inst)

(defn add-note-data [{:keys [scale freq-busses] :as inst}]
  (assoc inst :note-data (mapv vector scale freq-busses)))

(defn next-scale [{:keys [scale-vec scale-index] :as inst}]
  (let [new-index (mod (inc scale-index) (count scale-vec))]
    (assoc inst :scale (scale-vec new-index)
                :scale-index new-index)))

(def instrument-fn-map {:note-on    note-on
                        :note-off   note-off
                        :next-scale (comp add-note-data next-scale)
                        :pitch-bend pitch-bend
                        :mod-wheel mod-wheel})

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
    :black-note-on [[(:current-instrument state) :next-scale]]
    :pitch-bend [[(:current-instrument state) :pitch-bend (:pb-value event-data)]]
    :mod-wheel [[(:current-instrument state) :mod-wheel (:mod-wheel-value event-data)]]
    []))

