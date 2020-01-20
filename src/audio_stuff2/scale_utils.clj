(ns audio-stuff2.scale-utils)

(def num-notes 100)

(defn combination-chord [[fundamental-freq & chords]]
  (->> (for [x (range num-notes) y chords]
         (* (inc x) y fundamental-freq))
       sort
       dedupe
       (take num-notes)
       vec))

(defn combination-chord-prog [chord-seq]
  (mapv combination-chord chord-seq))

(defn update-scale [{:keys [note-data] :as inst} new-scale]
  (assoc inst :note-data (mapv #(assoc %1 :freq %2) note-data new-scale)))

(defn next-scale [{:keys [scale-vec scale-index] :as inst}]
  (let [new-index (mod (inc scale-index) (count scale-vec))]
    (-> inst
        (update-scale (scale-vec new-index))
        (assoc :scale-index new-index))))

(defn add-scale-prog [inst scale-vec]
  (-> inst
      (assoc :scale-index 0
             :scale-vec scale-vec)
      (update-scale (scale-vec 0))))