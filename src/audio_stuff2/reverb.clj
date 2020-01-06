(ns audio-stuff2.reverb
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

(defsynth reverb-synth [in-bus -1 out-bus -1 ir-spec -1 wet-gain 1 dry-gain 1]
          (let [sig    (in:ar in-bus 1)
                reverb (part-conv (* sig wet-gain) fft-size ir-spec)
                ]
            (out out-bus (+ reverb (* sig dry-gain)))))