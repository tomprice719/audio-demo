(ns audio-stuff2.instruments.control)

(defn instrument-reducer [message-handlers]
  (fn [state [instrument-key fn-key & args]]
    (update-in state [:instruments instrument-key]
               #(apply (message-handlers fn-key) % args))))

(defn handle-messages [message-generator message-handlers]
  (let [r (instrument-reducer message-handlers)]
    (fn [state]
      (reduce r state (message-generator state)))))

(comment
  (->
    (assoc )
    (assoc state (recording "filename" instrument-reducer stop-fn))))

(comment
  (defn control-recording [state event-data key->recording-fn]
    (update state :recording (key->recording-fn (TODO event-data)))))

(comment
  (defmulti event->messages
            (fn [instrument event-data]
              [(:input-type instrument)
               (:event-type event-data)])))

(comment
  (fn [event-data state]
    (let [messages (event->messages (:current-instrument state) event-data)]
      (-> state
          (reduce instrument-reducer messages)
          (reduce recording-reducer messages)
          (update-current-instrument event-data key->instrument)
          (control-recording event-data key->recording-fn)))))

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
