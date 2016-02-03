(ns template.utils
  (:require [clojure.data.priority-map :as prio]
            [template.farm :as farm]))

(defn- make-heap
  "Returns a heap for the given key function (eg :agony) and a comparator (eg <) and the elements in colls"
  [keyfn compar colls]
  (->> (map first colls)
       (mapcat vector (range))
       (apply prio/priority-map-keyfn-by keyfn compar)))

(defn safe-pop
  "Safely pops from a vect. Returns empty vector instead of throwing an exception."
  [vect]
  (if (seq vect) (subvec vect 1) []))

(defn- make-next-value
  "Helper function for merge-sorted-collections-by-aux"
  [colls coll-idx keyfn compar]
  (or (first (get colls coll-idx))
      {keyfn (condp = compar
               < (Integer/MAX_VALUE)
               > (Integer/MIN_VALUE))}))

(defn- merge-sorted-collections-by-aux
  "Merges k sorted arrays together into a sorted array.
   The implementation uses a heap of size k for keeping track of of the next element to insert into the result array.
   It can be used when the k sorted arrays cannot fit in memory and we need to load only as little data as possible (Only a heap of size k) to build up the result array.

   Complexity
   ----------
   - N: total number of elements in the collections
   - k: Number of collections
   - time O(N log k)
   - space O(log k) if colls are on disk and merged-coll-acc is on disk too

   Input:
   - min-heap-acc is a priority map with the following structure and payload
     - key: index of the coll that the element belongs to
     - value: value of the element (get colls i)

   - keyfn: function to insert new elements in heap (eg: :agony)
   - compar: a comparator (eg: > or <)
   - colls: a sequence of collections
   - merged-coll-acc: accumulator for keeping track of the merged values from collections so far
   - remaining-count-acc: a counter (Integer) that keeps track of the total number of elements in colls to be merged

   Output: merged sequence of collections colls"
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

(defn heap-merge-sorted-collections-by
  [keyfn compar colls]
  (merge-sorted-collections-by-aux keyfn
                                   compar
                                   (mapv safe-pop colls)
                                   []
                                   (make-heap keyfn compar colls)
                                   (reduce + (map count colls))))


(defn- merge-two-sorted-coll-aux
  [keyfn compar acc [x1 & xs1 :as coll1] [x2 & xs2 :as coll2]]
  (cond
    (not (seq coll1))              (concat acc coll2)
    (not (seq coll2))              (concat acc coll1)
    (compar (keyfn x1) (keyfn x2)) (recur keyfn compar (conj acc x1) xs1 coll2)
    :else                          (recur keyfn compar (conj acc x2) coll1 xs2)))

(defn- merge-two-sorted-coll
  "Merges tow sorted collections coll1 and coll2 given a keyfunction and a
   comparator

   Note: Similar to the merge sort helper function merge

   Complexity
   ----------
   - n1: number of elements in coll1
   - n2: number of elements in coll2
   - time: O(n1+n2)
   - space: O(n1+n2)"
  [keyfn compar coll1 coll2]
  (merge-two-sorted-coll-aux keyfn compar [] coll1 coll2))

(defn merge-sorted-collections-by
  "Merges k collections given a keyfunction and a comparator

   Complexity
   ----------
   - n1: number of elements in coll1
   - n2: number of elements in coll2
   - ...
   - nk: number of elements in collk
   - time: O(n1+n2+...+nk)
   - space: O(n1+n2+...+nk)"
  [keyfn compar colls]
  (reduce (partial merge-two-sorted-coll keyfn compar) [] colls))


(defmacro benchmark
  "Returns in milliseconds the ellapsed time to compute the code
   {:time float (in ms) :result anything}"
  [& code]
  `(let [start# (. java.lang.System (clojure.core/nanoTime))
       result# (do ~@code)
       ellapsed# (/ (double (- (. java.lang.System (clojure.core/nanoTime))
                               start#)) 1000000.0)]
     {:time ellapsed# :result result#}))
