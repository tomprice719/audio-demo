(ns audio-stuff2.debug)

(defn show
  ([label x]
   (show label identity x))
  ([label f x]
   (do (println label (f x)) x)))
