(ns audio-stuff2.overtone-utils
  (:require
    [audio-stuff2.wavetables :refer [get-wt-data]]
    [overtone.core :refer :all]
    [overtone.sc.machinery.server.comms :refer [with-server-sync]]
    [overtone.sc.machinery.server.connection :refer [connection-status*]]))

(def notes-g)
(def effects-g)

(defn refresh-overtone []
  (when (= @connection-status* :disconnected)
    (boot-external-server))
  (with-server-sync #(get-wt-data 2000.0))
  (clear-all)
  (def notes-g (group "notes"))
  (def effects-g (group "effects" :after notes-g)))
