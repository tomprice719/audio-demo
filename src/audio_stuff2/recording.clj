(ns audio-stuff2.recording
  (:import (java.util.concurrent TimeUnit)
           (java.util.concurrent Executors)))

;TODO: load, save

(defn schedule-recursively [stpe play-fn absolute-start-time events]
  (let [now (- (System/nanoTime) absolute-start-time)
        [head [[tail-time _] :as tail]] (split-at 100 events)]
    (when (not-empty tail)
      (.schedule stpe
                 #(schedule-recursively stpe play-fn absolute-start-time tail)
                 (- tail-time now)
                 TimeUnit/NANOSECONDS))
    (doseq [[time event] head]
      (.schedule stpe
                 #(play-fn event)
                 (- time now)
                 TimeUnit/NANOSECONDS))))

(defn before? [start-time [time event]]
  (> start-time time))

;; Public interface

(defn stop-playing [{:keys [stpe] :as recording}]
  (some-> stpe .shutdownNow)
  (assoc recording :currently-recording false))

(defn start-playing [{:keys [events play-fn time-offset] :as recording}]
  (let [stpe (Executors/newScheduledThreadPool 1)
        start-time (- (System/nanoTime) time-offset)]
    (->> (seq events)
         (drop-while (partial before? time-offset))
         (schedule-recursively stpe play-fn start-time))
    (assoc recording :stpe stpe
                     :recording-start-time start-time)))

(defn play-and-record [recording]
  (-> recording
      start-playing
      (assoc :currently-recording true)))

(defn record-event [{:keys [currently-recording recording-start-time] :as recording} event]
  (if currently-recording
    (update-in recording [:events (- (System/nanoTime) recording-start-time)] concat event)
    recording))

(defn update-time-offset [recording time-offset]
  (assoc recording :time-offset (long (* time-offset 1000000000))))

(defn initial-events [{:keys [events time-offset]}]
  (->> (seq events)
       (take-while (partial before? time-offset))
       (mapcat second)))

(defn current-time [{:keys [recording-start-time]}]
  (/ (- (System/nanoTime) recording-start-time)
     1000000000.0))

(defn make-recording [play-fn path]
  {:time-offset         0
   :currently-recording false
   :events              (sorted-map)
   :play-fn             play-fn
   :path path
   })

(defn save-recording [{:keys [path events]}]
  (let [counter-file (clojure.java.io/file path "counter")
        counter-num (if (.exists counter-file)
                      (inc (read-string (slurp counter-file)))
                      0)]
    (spit (clojure.java.io/file path (str counter-num))
          (seq events))
    (spit counter-file counter-num)
    (println "Now at file " counter-num)))

(defn revert-recording [{:keys [path] :as recording} file-num]
  (->> (str file-num)
       (clojure.java.io/file path)
       slurp
       read-string
       (into (sorted-map))
       (assoc recording :events)))

(defn load-recording [{:keys [path] :as recording}]
  (->> (clojure.java.io/file path "counter")
       slurp
       (revert-recording recording)))

(comment

  (defn save-recording [recording path]
    (println "saving")
    (let [[name num] (read-string (slurp clojure.java.io/file path "counter"))]
      (spit (str name (inc num)) (vec @sound-events))
      (spit namefile-name [name (inc num)])))

  (defn load-recording [path]
    (println "loading")
    (let [[name num] (read-string (slurp namefile-name))]
      (reset! sound-events (read-string (slurp (str name num)))))))

