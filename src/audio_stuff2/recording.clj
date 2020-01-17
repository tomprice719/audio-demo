(ns audio-stuff2.recording
  (:import (java.util.concurrent TimeUnit)
           (java.util.concurrent Executors)))

;start-playing (with recording option), stop playing, change-time, maybe-record

;Functions needed:
;start-recording, stop-recording, clear-recording
;start-playing, stop-playing,
;save-recording, load-recording
;change-offset
;initial-events

;DON'T HAVE START RECORDING FUNCTION.
;just have start recording option on start-playing function
;You need to do these things in order
; Might as well stop playing and recording together also

;Use after, not at

;; HELPER FUNCTIONS

(defn time []
  (/ (System/nanoTime) 1000000000.0))

(defn schedule-rec [stpe event-handler absolute-start-time events]
  (let [now (- (time) absolute-start-time)
        [head [[tail-time _] :as tail]] (split-at 100 events)]
    (schedule stpe #(schedule-rec stpe event-handler absolute-start-time tail)
              (- tail-time now))
    (doseq [[time event] head]
      (.schedule stpe
                #(event-handler event)
                (- time now)
                 TimeUnit/SECONDS))))

(defn before? [start-time [time event]]
  (> start-time time))

;; Public interface

(defn stop [{:keys [stpe] :as recording}]
  (some-> stpe .shutdown)
  (assoc recording :currently-recording false))

(defn play [{:keys [events event-handler time-offset :as recording]}]
  (let [stpe (Executors/newScheduledThreadPool 1)
        now (time)]
    (.setExecuteExistingDelayedTasksAfterShutdownPolicy stpe false)
    (->> (seq events)
         (drop-while (partial before? time-offset))
         (schedule-rec stpe event-handler now))
    (-> recording
        stop
        (assoc :stpe stpe
               :recording-start-time (- now time-offset)))))

(defn play-and-record [recording]
  (play recording)
  (assoc recording :currently-recording true))

(defn record-event [{:keys [currently-recording] :as recording} event]
  (if currently-recording
    (update-in recording [:events (time)] concat event)
    recording))

(defn update-time-offset [recording time-offset]
  ((assoc recording :time-offset time-offset)))

(defn initial-events [{:keys [events time-offset]}]
  (->> (seq events)
       (take-while (partial before? time-offset))
       (map second)
       flatten))

(defn new-recording [event-handler]
  {:time-offset 0
   :currently-recording false
   :events (sorted-map)
   :event-handler event-handler
   })

(comment

  (binding [silent true]
    (doseq [[time event] (take-while before-start? (seq events))]
      (event-handler event)))

  (->> (seq events)
       (drop-while (partial before? time-offset))
       (schedule-rec event-handler (System/nanoTime)))

  (binding [silent true]
    (->> events
         seq
         (take-while (partial before? time-offset))
         (map (fn [[time event]] (event)))
         doall))
  (->> (seq events)
       (drop-while (partial before? time-offset))
       (schedule-initial)
       (schedule-later)
       (map (fn [time event] (.schedule s #(event-handler event) time)))
       doall)


  (let [before-start? (fn [[time event]] (> time-offset time))]
    (binding [silent true]
      (doseq [[time event] (take-while before-start? (seq events))]
        (event-handler event)))
    (->> (seq events)
         (drop-while before-start?)
         (map (fn [time event] (.schedule s #(event-handler event) time )))
         doall))


  (let [before-start? (fn [[time event]] (> time-offset time))]
    (binding [silent true]
      (send-off state-agent
                #(->> (seq events)
                      (take-while before-start?)
                      (reduce reducer %))))
    (await state-agent)
    (->> (seq events)
         (drop-while (partial before? time-offset))
         vec
         (schedule-rec (System/nanoTime))))
  ;;DO RECORDING STUFF HERE


  (schedule-rec (vec ()))
  (doseq [time (take-while (partial <= time-offset) (keys events))]
    (schedule-at (partial event-handler (events time)) (+ time now)))
  (mapv #())

  (defn schedule-rec [event-handler now-time events]
    (if (<= (count events) 10)
      (doseq [[time event] events]
        (schedule (- time now-time) #(event-handler event)))
      (let [[first-half [[half-time _] :as last-half]]
            (split-at (quot (count events) 2) events)]
        (schedule (- half-time now-time)
                  #(schedule-rec event-handler half-time last-half))
        (recur event-handler now-time first-half)))))