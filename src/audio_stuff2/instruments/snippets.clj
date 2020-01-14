(ns audio-stuff2.instruments.snippets)

;(defn play-fn2 [synth bus]
;  (fn [freq freq-bus velocity]
;    (let [[wt1 wt2 wt3 wt4] (get-wt-data freq)]
;      (synth [:tail notes-g] bus freq wt1 wt2 wt3 wt4))))
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
