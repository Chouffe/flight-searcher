(ns template.utils
  (:require [clojure.data.priority-map :as prio]
            [template.farm :as farm]))

;; TODO: add tests + docstrings
(defn- make-heap
  [keyfn compar colls]
  (->> (map first colls)
       (mapcat vector (range))
       (apply prio/priority-map-keyfn-by keyfn compar)))

(defn safe-pop
  [vect]
  (if (seq vect) (subvec vect 1) []))

(defn- make-next-value
  [colls coll-idx keyfn compar]
  (or (first (get colls coll-idx))
      {keyfn (condp = compar
               < (Integer/MAX_VALUE)
               > (Integer/MIN_VALUE))}))

(defn- merge-sorted-collections-by-aux
  "TODO: Add a description

   Input:
   - min-heap-acc is a priority map with the following structure and payload
     - key: index of the coll that the element belongs to
     - value: value of the element (get colls i)

   - keyfn: function to insert new elements in heap (eg: :agony)
   - compar: a comparator (eg: > or <)
   - colls: a sequence of collections
   - merged-coll-acc: accumulator for keeping track of the merged values from collections so far
   - remaining-count-acc: a counter (Integer) that keeps track of the total number of elements in colls to be merged"
  [keyfn compar colls merged-coll-acc min-heap-acc remaining-count-acc]
  (if (= 0 remaining-count-acc)
    merged-coll-acc
    (let [[coll-idx value :as pk] (peek min-heap-acc)
          next-value (make-next-value colls coll-idx keyfn compar)]
      (recur keyfn
             compar
             (vec (map-indexed (fn [idx coll] (if (= idx coll-idx) (safe-pop coll) coll)) colls))
             (conj merged-coll-acc value)
             (assoc (pop min-heap-acc) coll-idx next-value)
             (dec remaining-count-acc)))))

(defn merge-sorted-collections-by
  [keyfn compar colls]
  (merge-sorted-collections-by-aux keyfn
                                   compar
                                   (mapv safe-pop colls)
                                   []
                                   (make-heap keyfn compar colls)
                                   (reduce + (map count colls))))

; (merge-sorted-collections-by :agony < [[{:agony 1} {:agony 2}] [{:agony 0}] [{:agony 1.5} {:agony 4}] [{:agony 1} {:agony 1.3}]])
; (merge-sorted-collections-by :agony > [[{:agony 2} {:agony 1}] [{:agony 0}] [{:agony 4} {:agony 1.5}] [{:agony 1.3} {:agony 1}]])

(defn- merge-two-sorted-coll-aux
  [keyfn compar acc [x1 & xs1 :as coll1] [x2 & xs2 :as coll2]]
  (cond
    (not (seq coll1))              (concat acc coll2)
    (not (seq coll2))              (concat acc coll1)
    (compar (keyfn x1) (keyfn x2)) (recur keyfn compar (conj acc x1) xs1 coll2)
    :else                          (recur keyfn compar (conj acc x2) coll1 xs2)))

(defn- merge-two-sorted-coll
  [keyfn compar coll1 coll2]
  (merge-two-sorted-coll-aux keyfn compar [] coll1 coll2))

;; Tests
; (merge-two-sorted-coll-aux :agony < [] [{:agony 1} {:agony 2} {:agony 3}] [{:agony 0} {:agony 4}])
; (merge-two-sorted-coll-aux :agony > [] [{:agony 3} {:agony 2} {:agony 1}] [{:agony 4} {:agony 1}])

(defn merge-sorted-collections-by
  [keyfn compar colls]
  (reduce (partial merge-two-sorted-coll keyfn compar) [] colls))

; (merge-sorted-collections-by :agony < [ [{:agony 1} {:agony 2} {:agony 3}] [{:agony 0} {:agony 4}] [{:agony 5} {:agony 6}]])
; (merge-sorted-collections-by :agony > [ [{:agony 3} {:agony 2} {:agony 1}] [{:agony 4} {:agony 0}] [{:agony 6} {:agony 5}]])

(defmacro benchmark
  "Returns in milliseconds the ellapsed time to compute the code
   {:time float (in ms) :result anything}"
  [& code]
  `(let [start# (. java.lang.System (clojure.core/nanoTime))
       result# (do ~@code)
       ellapsed# (/ (double (- (. java.lang.System (clojure.core/nanoTime))
                               start#)) 1000000.0)]
     {:time ellapsed# :result result#}))
