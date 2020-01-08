(ns audio-stuff2.debug)

(defn show
  ([x]
   (show identity x))
  ([f x]
   (do (println (f x)) x)))

(defn labeller [label]
  #(vector label %))
