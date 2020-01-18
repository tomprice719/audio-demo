(ns audio-stuff2.recording
  (:import (java.util.concurrent TimeUnit)
           (java.util.concurrent Executors)))

;TODO: load, save

(defn schedule-recursively [stpe play-fn absolute-start-time events]
  (let [now (- (System/nanoTime) absolute-start-time)
        [head [[tail-time _] :as tail]] (split-at 3 events)]
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
        now (System/nanoTime)]
    (->> (seq events)
         (drop-while (partial before? time-offset))
         (schedule-recursively stpe play-fn now))
    (assoc recording :stpe stpe
                     :recording-start-time (- now time-offset))))

(defn play-and-record [recording]
  (-> recording
      start-playing
      (assoc :currently-recording true)))

(defn record-event [{:keys [currently-recording recording-start-time] :as recording} event]
  (if currently-recording
    (update-in recording [:events (- (System/nanoTime) recording-start-time)] concat event)
    recording))

(defn update-time-offset [recording time-offset]
  ((assoc recording :time-offset time-offset)))

(defn initial-events [{:keys [events time-offset]}]
  (->> (seq events)
       (take-while (partial before? time-offset))
       (map second)
       flatten))

(defn make-recording [play-fn]
  {:time-offset         0
   :currently-recording false
   :events              (sorted-map)
   :play-fn             play-fn
   })