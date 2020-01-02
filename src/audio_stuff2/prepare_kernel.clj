(ns audio-stuff2.prepare-kernel
  (:require [overtone.core :refer :all]))
(def fft-size 512)

(defn num-partitions [ir-buffer]
  (let [partition-size (/ fft-size 2)
        size (num-frames ir-buffer)]
    (Math/ceil (/ size partition-size))))

(defn buf-size [ir-buffer]
  (* fft-size (num-partitions ir-buffer)))

(defn prepare-part-conv [src-buffer dest-buffer]
  (snd "/b_gen" (buffer-id dest-buffer) "PreparePartConv" (buffer-id src-buffer) fft-size))

(defn get-ir-spectrum [ir-location]
  (let [my-ir (load-sample ir-location)
        ir-spectrum (buffer (buf-size my-ir))]
    (prepare-part-conv my-ir ir-spectrum)
    ir-spectrum))