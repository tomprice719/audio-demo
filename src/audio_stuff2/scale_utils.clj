(ns audio-stuff2.scale-utils)

(def num-notes 20)

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

(defn add-scale-vec [inst scale-vec]
  (assoc inst :scale-index 0
              :scale-vec scale-vec
              :scale (scale-vec 0)))