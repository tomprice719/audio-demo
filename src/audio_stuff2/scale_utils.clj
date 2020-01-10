(ns audio-stuff2.scale-utils)

(def num-notes 50)

(defn load-chords [filename]
  ((fn [x] (println x) x)
   (for [line (clojure.string/split-lines (slurp filename))
         :when (not= (first line) \#)]
     (map read-string (clojure.string/split line #"\s+")))))

(defn combination-chord [[fundamental-freq & chords]]
  (->> (for [x (range num-notes) y chords]
         (* (inc x) y fundamental-freq))
       sort
       dedupe
       (take num-notes)
       vec))

(defn make-scale-vec [chord-seq]
  (mapv combination-chord chord-seq))

(defn update-scale [{:keys [note-data] :as inst} new-scale]
  (assoc inst :note-data (mapv #(assoc %1 :freq %2) note-data new-scale)))

(defn next-scale [{:keys [scale-vec scale-index] :as inst}]
  (let [new-index (mod (inc scale-index) (count scale-vec))]
    (-> inst
        (update-scale (scale-vec new-index))
        (assoc :scale-index new-index))))

(defn add-scale-vec [inst scale-vec]
  (-> inst
      (assoc :scale-index 0
             :scale-vec scale-vec)
      (update-scale (scale-vec 0))))