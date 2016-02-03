(ns template.decorators
  (:require [template.utils :refer [benchmark]]))

;; TODO: use better logging functions
(def info println)
(def error println)

(defn logger [f]
  (fn [& args]
    (let [{:keys [time result]} (benchmark (apply f args))]
      (info (format "[time ellapsed: %s] %s - %s" time (str f) args))
      result)))

(defn recover-and-report [f]
  (fn [& args]
    (try
      (apply f args)
      (catch Exception e
        (error (format "Exception: %s" e))
        nil))))
