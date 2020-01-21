(ns audio-stuff2.scale-utils)

(defn ratio->cents [r]
  (* (/ (Math/log r) (Math/log 2))
     1200))

(defn cents->ratio [cents]
  (java.lang.Math/pow 2.0 (/ cents 1200.0)))

(defn equal-temperament [step-size num-notes starting-freq]
  (map #(* starting-freq (cents->ratio (* step-size %))) (range num-notes)))

(defn combination-chord [num-notes [fundamental-freq & chords]]
  (->> (for [x (range num-notes) y chords]
         (* (inc x) y fundamental-freq))
       sort
       dedupe
       (take num-notes)))

(defn combination-chord-prog [num-notes chord-seq]
  (mapv (partial combination-chord num-notes) chord-seq))

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