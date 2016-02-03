(ns template.utils-test
  (:require [clojure.test :refer :all]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [template.utils :refer :all]))

;; --------------
;; Random testing

(defn sorted-vec [compar]
  (->> gen/int
       gen/vector
       (gen/fmap (partial sort compar))
       (gen/fmap vec)
       (gen/vector)))

(def merge-sorted-asc
  (prop/for-all [xxs (sorted-vec <)]
    (= (merge-sorted-collections-by identity < xxs)
       (sort < (apply concat xxs)))))

(def heap-merge-sorted-asc
  (prop/for-all [xxs (sorted-vec <)]
    (= (heap-merge-sorted-collections-by identity < xxs)
       (sort < (apply concat xxs)))))

(def merge-sorted-desc
  (prop/for-all [xxs (sorted-vec >)]
    (= (merge-sorted-collections-by identity > xxs)
       (sort > (apply concat xxs)))))

(def heap-merge-sorted-desc
  (prop/for-all [xxs (sorted-vec >)]
    (= (heap-merge-sorted-collections-by identity > xxs)
       (sort > (apply concat xxs)))))

(tc/quick-check 100 merge-sorted-desc)
(tc/quick-check 100 heap-merge-sorted-asc)
(tc/quick-check 100 merge-sorted-asc)
(tc/quick-check 100 heap-merge-sorted-asc)

;; --------------------
;; Regular Unit testing

(deftest regular-merge
  (testing "asc on :agony"
    (is (= (merge-sorted-collections-by :agony < [[{:agony 1} {:agony 2} {:agony 3}]
                                                  [{:agony 0} {:agony 4}]
                                                  [{:agony 5} {:agony 6}]])
           [{:agony 0} {:agony 1} {:agony 2} {:agony 3} {:agony 4} {:agony 5} {:agony 6}])))
  (testing "desc on :agony"
    (is (= (merge-sorted-collections-by :agony > [[{:agony 3} {:agony 2} {:agony 1}]
                                                  [{:agony 4} {:agony 0}]
                                                  [{:agony 6} {:agony 5}]])
           [{:agony 6} {:agony 5} {:agony 4} {:agony 3} {:agony 2} {:agony 1} {:agony 0}]))))

(deftest heap-merge
  (testing "asc on :agony"
    (is (= (heap-merge-sorted-collections-by :agony < [[{:agony 1} {:agony 2}]
                                                       [{:agony 0}] [{:agony 1.5} {:agony 4}]
                                                       [{:agony 1} {:agony 1.3}]])
           [{:agony 0} {:agony 1} {:agony 1} {:agony 1.3} {:agony 1.5} {:agony 2} {:agony 4}])))
  (testing "desc on :agony"
    (is (= (heap-merge-sorted-collections-by :agony > [[{:agony 2} {:agony 1}]
                                                       [{:agony 0}] [{:agony 4} {:agony 1.5}]
                                                       [{:agony 1.3} {:agony 1}]])
           [{:agony 4} {:agony 2} {:agony 1.5} {:agony 1.3} {:agony 1} {:agony 1} {:agony 0}]))))
