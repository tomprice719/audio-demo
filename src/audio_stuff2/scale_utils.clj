(ns audio-stuff2.scale-utils)

(def num-notes 20)

(comment
  (defn bent-pitch [pitch pb-ratio midi-pb]
    (let [high-pitch (* pitch pb-ratio)
          low-pitch (/ pitch pb-ratio)]
      (float (if (< midi-pb 64)
               (lin-interpolate pitch
                                high-pitch
                                (/ midi-pb 63))
               (lin-interpolate low-pitch
                                pitch
                                (/ (- midi-pb 64) 64)))))))

(comment
  (defn make-freq-fn [pb-range freqs]
    (fn [note-num midi-pb]
      (+ (freqs note-num) (* pb-range midi-pb)))))

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