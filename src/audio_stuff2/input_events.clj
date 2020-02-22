(ns audio-stuff2.input-events
  (:require [overtone.libs.event :as e]
            [seesaw.core :refer [native! show! dispose! listen frame]]))

(declare window)

(def white-keys
  (vec
    (for [octave (range 20)
          white-key [0 nil 1 nil 2 3 nil 4 nil 5 nil 6]]
      (if white-key
        (+ white-key (* 7 octave))))))

(def black-keys
  (vec
    (for [octave (range 20)
          black-key [nil 0 nil 1 nil nil 2 nil 3 nil 4 nil]]
      (if black-key
        (+ black-key (* 5 octave))))))

(defn fix-pb [raw-value]
  (if (< raw-value 64)
    (/ raw-value 63.0)
    (/ (- raw-value 128) 64.0)))

(def held-keys (atom #{}))

(defn set-handlers [state-agent state-fn]
  (def window (frame :title "frame" :size [300 :by 300]))
  (show! window)
  (let [handle
        (fn [event-data]
          (send-off state-agent state-fn event-data))]
    (e/on-event [:midi :note-on]
                #(handle {:event-type :note-on
                          :note-num   (:note %)
                          :velocity   (:velocity-f %)})
                ::note-on)
    (e/on-event [:midi :note-on]
                #(when-let [white-note-num (white-keys (:note %))]
                   (handle {:event-type     :white-note-on
                            :white-note-num white-note-num
                            :velocity       (:velocity-f %)}))
                ::white-note-on)
    (e/on-event [:midi :note-on]
                #(when-let [black-note-num (black-keys (:note %))]
                   (handle {:event-type     :black-note-on
                            :black-note-num black-note-num
                            :velocity       (:velocity-f %)}))
                ::black-note-on)
    (e/on-event [:midi :note-off]
                #(handle {:event-type :note-off
                          :note-num   (:note %)})
                ::note-off)
    (e/on-event [:midi :note-off]
                #(when-let [white-note-num (white-keys (:note %))]
                   (handle {:event-type     :white-note-off
                            :white-note-num white-note-num}))
                ::white-note-off)
    (e/on-event [:midi :note-on]
                #(when-let [black-note-num (black-keys (:note %))]
                   (handle {:event-type     :black-note-off
                            :black-note-num black-note-num}))
                ::black-note-off)
    (e/on-event [:midi :pitch-bend]
                #(handle {:event-type :pitch-bend
                          :pb-value   (-> % :data2 fix-pb)})
                ::pitch-bend)
    (e/on-event [:midi :control-change]
                #(when (= (:channel %) 0)
                   (handle {:event-type      :mod-wheel
                            :mod-wheel-value (:data2-f %)}))
                ::mod-wheel)
    (listen window
            :key-pressed
            #(when-not (contains? @held-keys (.getKeyCode %))
               (swap! held-keys conj (.getKeyCode %))
               (handle {:event-type :key-pressed
                        :key-code   (.getKeyCode %)
                        :key-char   (.getKeyChar %)}))
            :key-released
            #(do (swap! held-keys disj (.getKeyCode %))
                 (handle {:event-type :key-released
                          :key-code   (.getKeyCode %)
                          :key-char   (.getKeyChar %)})))))

(defn close-window []
  (when (bound? #'window)
    (dispose! window)))
