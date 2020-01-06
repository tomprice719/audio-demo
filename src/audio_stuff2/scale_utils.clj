(ns audio-stuff2.scale-utils)

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

(defn make-freq-fn [pb-range freqs]
  (fn [note-num midi-pb]
    (+ (freqs note-num) (* pb-range midi-pb))))

(defn load-chords [filename]
  ((fn [x] (println x) x)
   (for [line (clojure.string/split-lines (slurp filename))
         :when (not= (first line) \#)]
     (map read-string (clojure.string/split line #"\s+")))))

(defn combination-chord [[fundamental-freq & chords]]
  (vec (take 30 (dedupe (sort (for [x (range 1 30) y chords] (* x y fundamental-freq)))))))

(defn make-freq-fn-vec [chord-seq pb-range]
  (mapv (comp (partial make-freq-fn pb-range) combination-chord) chord-seq))

(defn add-freq-fn-vec [inst freq-fn-vec]
  (assoc inst :freq-fn-index 0
              :freq-fn-vec freq-fn-vec
              :freq-fn (freq-fn-vec 0)))