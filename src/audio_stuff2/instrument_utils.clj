(ns audio-stuff2.instrument-utils
  (:require
    [overtone.core :refer :all]
    [audio-stuff2.input-events :refer [event-data]]
    [audio-stuff2.scale-utils :refer [next-scale num-notes]]
    [audio-stuff2.breakpoints :refer [breakpoint]]
    [overtone.sc.machinery.server.comms :refer [with-server-sync]]
    [audio-stuff2.reverb :refer [get-ir-spectrum fft-size reverb-synth]]
    [debux.core :refer [dbg dbgn]]))

(def notes-g (group "notes"))
(def effects-g (group "effects" :after notes-g))

(defn make-bus [left-ir left-wet-gain left-dry-gain
                right-ir right-wet-gain right-dry-gain]
  (let [bus (audio-bus)
        left-ir-spec (with-server-sync #(get-ir-spectrum left-ir))
        right-ir-spec (with-server-sync #(get-ir-spectrum right-ir))]
    (println bus)
    (reverb-synth [:tail effects-g] bus 0 left-ir-spec left-wet-gain left-dry-gain)
    (reverb-synth [:tail effects-g] bus 1 right-ir-spec right-wet-gain right-dry-gain)
    bus))

(defn stop-synth [synth]
  (node-control synth [:gate 0])
  (after-delay 10000 #(kill synth)))

(defmulti start-synth (fn [inst note-data pitch-bend velocity] (:type inst)))
(defmulti note-on (fn [inst note-num velocity] (:type inst)))
(defmulti note-off (fn [inst note-num] (:type inst)))

(defn bent-pitch [freq pb-value] (* freq (+ 1 (* pb-value 0.1))))

(defmethod start-synth :poly-instrument [{:keys [out-bus mod-wheel-bus synth]}
                                         {:keys [freq freq-bus]}
                                         pb-value
                                         velocity]
  (control-bus-set! freq-bus (bent-pitch freq pb-value))
  (synth [:tail notes-g] out-bus freq-bus mod-wheel-bus))

(defmethod note-on :poly-instrument [{:keys [pitch-bend note-data] :as inst} note-num velocity]
  (breakpoint "Note On")
  (if-let [d (and (not (get-in note-data [note-num :synth])) (get note-data note-num))]
    (-> inst
        (assoc-in [:note-data note-num :synth]
                  (start-synth inst d pitch-bend velocity))
        (assoc :current-note-num note-num))
    inst))

(defmethod note-off :poly-instrument [{:keys [current-note-num] :as inst} note-num]
  (if-let [synth (get-in inst [:note-data note-num :synth])]
    (do (stop-synth synth)
        (as-> inst this2
              (update-in inst [:note-data note-num] dissoc :synth)
              (if (= note-num current-note-num)
                (dissoc this2 :current-note-num)
                this2)))
    inst))

(defn make-poly-instrument [synth & bus-args]
  {:type :poly-instrument
   :synth         synth
   :out-bus       (apply make-bus bus-args)
   :note-data     (mapv (partial hash-map :freq-bus)
                        (repeatedly num-notes control-bus))
   :mod-wheel-bus (control-bus)
   :pitch-bend    0})

(defn pitch-bend [{:keys [current-note-num] :as inst} pb-value]
  (when-let [{:keys [freq freq-bus]}
             (get-in inst [:note-data current-note-num])]
    (control-bus-set! freq-bus (bent-pitch freq pb-value)))
  (assoc inst :pitch-bend pb-value))

(defn mod-wheel [{:keys [mod-wheel-bus] :as inst} mod-wheel-value]
  (control-bus-set! mod-wheel-bus mod-wheel-value)
  inst)

(def instrument-fn-map {:note-on    note-on
                        :note-off   note-off
                        :next-scale next-scale
                        :pitch-bend pitch-bend
                        :mod-wheel  mod-wheel})

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


;(defn play-fn2 [synth bus]
;  (fn [freq freq-bus velocity]
;    (let [[wt1 wt2 wt3 wt4] (get-wt-data freq)]
;      (synth [:tail notes-g] bus freq wt1 wt2 wt3 wt4))))
;
;(defn play-fn [synth out-bus mod-wheel-bus pitch-bend-fn]
;  (fn [[freq freq-bus] pb-value velocity]
;    (control-bus-set! freq-bus (pitch-bend-fn freq pb-value))
;    [(synth [:tail notes-g] out-bus freq-bus mod-wheel-bus)
;     freq
;     freq-bus]))
;
;(defprotocol Instrument
;  (note-on [this note-num velocity])
;  (note-off [this note-num]))
;
;(defrecord Poly-Instrument
;  [play-note-fn stop-note-fn note-data notes current-note-num pitch-bend]
;  Instrument
;
;  (note-off [this note-num]
;    (if-let [synth (notes note-num)]
;      (do (stop-note-fn synth)
;          (as-> this this2
;                (update this :synths dissoc note-num)
;                (if (= note-num current-note-num)
;                  (assoc this2 :current-note-num nil)
;                  this2)))
;      this)))
;
;(defn make-poly-instrument [field-map]
;  (map->Poly-Instrument (merge {:synths {} :current-note-num nil :pitch-bend 0}
;                               field-map)))
;
;(defrecord Mono-Instrument
;  [play-note-fn stop-note-fn note-data freq-fn current-note-num pitch-bend]
;  Instrument
;  (note-on [this note-num velocity]
;    (if-let [freq (freq-fn note-num pitch-bend)]
;      (do (play-note-fn freq (note-data note-num) velocity)
;          (assoc this :current-note-num note-num))
;      this))
;  (note-off [this note-num]
;    (if (= current-note-num note-num)
;      (do (stop-note-fn)
;          (assoc this current-note-num nil))
;      this)))
;
