(ns audio-stuff2.instruments.poly-instrument
  (:require
    [overtone.core :refer :all]
    [audio-stuff2.overtone-utils :refer [notes-g effects-g make-bus get-wt-data]]
    [audio-stuff2.instruments.base-instrument :refer [note-on
                                                      note-off
                                                      bent-pitch
                                                      pitch-bend
                                                      initialize
                                                      audible]]))

(defmulti start-synth (fn [note-data inst velocity] (:type inst)))

(defmethod start-synth ::poly-instrument
  [{:keys [freq freq-bus synth] :as note-data}
   {:keys [out-bus mod-wheel-bus synth-fn pitch-bend]}
   velocity]
  (if-not synth
    (do
      (control-bus-set! freq-bus (bent-pitch freq pitch-bend))
      (assoc note-data :synth
                       (synth-fn [:tail notes-g]
                                 out-bus freq-bus mod-wheel-bus velocity)))
    note-data))

(defmethod start-synth ::wt-poly-instrument
  [{:keys [freq freq-bus synth] :as note-data}
   {:keys [out-bus mod-wheel-bus synth-fn pitch-bend]}
   velocity]
  (if-not synth
    (do
      (control-bus-set! freq-bus (bent-pitch freq pitch-bend))
      (assoc note-data :synth
                       (let [[wt1 wt2 wt3 wt4] (get-wt-data freq)]
                         (synth-fn [:tail notes-g]
                                   out-bus freq-bus mod-wheel-bus velocity
                                   wt1 wt2 wt3 wt4))))
    note-data))

(defmethod note-on ::poly-instrument [inst note-num velocity]
  (if audible
    (-> inst
        (update-in [:note-data note-num] start-synth inst velocity)
        (assoc :current-note-num note-num))
    inst))

(defn stop-synth [{:keys [synth] :as note-data} inst]
  (when synth
    (node-control synth [:gate 0])
    (after-delay 10000 #(kill synth)))
  (dissoc note-data :synth))

(defmethod note-off ::poly-instrument [{:keys [current-note-num] :as inst} note-num]
  (if audible
    (as-> inst inst2
          (update-in inst2 [:note-data note-num] stop-synth inst2)
          (if (= note-num current-note-num)
            (dissoc inst2 :current-note-num)
            inst2))
    inst))

(defmethod pitch-bend ::poly-instrument [{:keys [current-note-num] :as inst} pb-value]
  (when-let [{:keys [freq freq-bus]}
             (and audible
                  (get-in inst [:note-data current-note-num]))]
    (control-bus-set! freq-bus (bent-pitch freq pb-value)))
  (assoc inst :pitch-bend pb-value))

(defn add-freq-busses [note-data]
  (mapv #(assoc % :freq-bus (control-bus)) note-data))

(defmethod initialize ::poly-instrument [{:keys [bus-args mod-wheel-value] :as instrument}]
  (-> instrument
      (update :note-data add-freq-busses)
      (assoc :out-bus (apply make-bus bus-args)
             :mod-wheel-bus (doto (control-bus) (control-bus-set! mod-wheel-value)))))

(defn make-poly-instrument [synth-fn input-type & bus-args]
  {:type            ::poly-instrument
   :synth-fn        synth-fn
   :input-type      input-type
   :bus-args        bus-args
   :pitch-bend      0
   :mod-wheel-value 0
   :note-data       (repeat {})})

(defn make-wt-poly-instrument [synth-fn input-type & bus-args]
  (assoc (apply make-poly-instrument synth-fn input-type  bus-args)
    :type ::wt-poly-instrument))

(derive ::wt-poly-instrument ::poly-instrument)

