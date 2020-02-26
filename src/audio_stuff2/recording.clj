(ns audio-stuff2.recording
  (:import (java.util.concurrent TimeUnit)
           (java.util.concurrent Executors)))

(defn seconds->nanos [seconds]
  (long (* 1000000000.0 seconds)))

(defn nanos->seconds [nanos]
  (/ nanos 1000000000.0))

(defn current-time-seconds []
  (nanos->seconds (System/nanoTime)))

(defn schedule-recursively [stpe play-fn absolute-start-time events]
  (let [now (- (current-time-seconds) absolute-start-time)
        [head [[[tail-time _] _] :as tail]] (split-at 100 events)]
    (when (not-empty tail)
      (.schedule stpe
                 #(schedule-recursively stpe play-fn absolute-start-time tail)
                 (seconds->nanos (- tail-time now))
                 TimeUnit/NANOSECONDS))
    (doseq [[[time _] event] head]
      (.schedule stpe
                 #(play-fn event)
                 (seconds->nanos (- time now))
                 TimeUnit/NANOSECONDS))))

(defn before? [start-time [[time _] _]]
  (> start-time time))

;; Public interface

(defn stop-playing [{:keys [stpe] :as recording}]
  (some-> stpe .shutdownNow)
  (assoc recording :currently-recording false))

(defn start-playing [{:keys [events play-fn time-offset] :as recording}]
  (let [stpe (Executors/newScheduledThreadPool 1)
        start-time (- (current-time-seconds) time-offset)]
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
    (assoc-in recording [:events [(- (current-time-seconds) recording-start-time) (gensym)]] event)
    recording))

(defn update-time-offset [recording time-offset]
  (assoc recording :time-offset (long (* time-offset 1000000000))))

(defn initial-events [{:keys [events time-offset]}]
  (->> (seq events)
       (take-while (partial before? time-offset))
       (map second)))

(defn recording-time [{:keys [recording-start-time]}]
  (when recording-start-time
    (- (current-time-seconds) recording-start-time)))

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

(defn load-recording
  ([{:keys [path] :as recording} file-num]
   (->> (str file-num)
        (clojure.java.io/file path)
        slurp
        read-string
        (into (sorted-map))
        (assoc recording :events)))
  ([{:keys [path] :as recording}]
   (let [counter-file (clojure.java.io/file path "counter")]
     (if (.exists counter-file)
       (load-recording recording (slurp counter-file))
       recording))))

(defn transduce-events [{:keys [events] :as recording} xform]
  (assoc recording :events (into (sorted-map) xform events)))
