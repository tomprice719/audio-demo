(ns audio-stuff2.instruments.message-generators)

(defmulti generate-messages (fn [inst inst-key event-data] (:input-type inst)))

(defmethod generate-messages :white-notes [inst inst-key {:keys [event-type
                                                                 white-note-num
                                                                 velocity
                                                                 pb-value
                                                                 mod-wheel-value]}]
  (case event-type
    :white-note-on [[inst-key :note-on white-note-num velocity]]
    :white-note-off [[inst-key :note-off white-note-num]]
    :black-note-on [[inst-key :next-scale]]
    :pitch-bend [[inst-key :pitch-bend pb-value]]
    :mod-wheel [[inst-key :mod-wheel mod-wheel-value]]
    []))

(defmethod generate-messages :all-notes [inst inst-key [event-type
                                                        note-num
                                                        velocity
                                                        pb-value
                                                        mod-wheel-value]]
  (case event-type
    :note-on [[inst-key :note-on note-num velocity]]
    :note-off [[inst-key :note-off note-num]]
    :pitch-bend [[inst-key :pitch-bend pb-value]]
    :mod-wheel [[inst-key :mod-wheel mod-wheel-value]]
    []))

(def horizontal-keymap (zipmap "zxcvbnm,./asdfghjkl;qwertyuiop" (range)))

(defmethod generate-messages :typing [{:keys [default-velocity default-mod-wheel]}
                                      inst-key
                                      {:keys [event-type key-char]}]
  (if (= [event-type key-char] [:key-pressed \space])
    [[inst-key :next-scale]]
    (if-let [note-num (horizontal-keymap key-char)]
      (case event-type
        :key-pressed [[inst-key :mod-wheel default-mod-wheel]
                      [inst-key :note-on note-num default-velocity]]
        :key-released [[inst-key :note-off note-num]]
        [])
      [])))