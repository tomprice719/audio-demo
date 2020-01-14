(ns audio-stuff2.instruments.message-generators)

(defmulti generate-messages (fn [inst inst-key event-data] (:input-type inst)))

(defmethod generate-messages :white-notes [inst inst-key event-data]
  (case (:event event-data)
    :white-note-on [[inst-key :note-on (:white-note-num event-data) (:velocity event-data)]]
    :white-note-off [[inst-key :note-off (:white-note-num event-data)]]
    :black-note-on [[inst-key :next-scale]]
    :pitch-bend [[inst-key :pitch-bend (:pb-value event-data)]]
    :mod-wheel [[inst-key :mod-wheel (:mod-wheel-value event-data)]]
    []))

(defmethod generate-messages :all-notes [inst inst-key event-data]
  (case (:event event-data)
    :note-on [[inst-key :note-on (:note-num event-data) (:velocity event-data)]]
    :note-off [[inst-key :note-off (:note-num event-data)]]
    :pitch-bend [[inst-key :pitch-bend (:pb-value event-data)]]
    :mod-wheel [[inst-key :mod-wheel (:mod-wheel-value event-data)]]
    []))