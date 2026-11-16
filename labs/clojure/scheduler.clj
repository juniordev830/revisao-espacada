(ns scheduler
  "Pure spaced-repetition scheduling with the classic SM-2 ease update."
  (:require [clojure.test :refer [deftest is]]))

(defn review
  "Return updated card fields for a 0..5 recall score."
  [{:keys [repetitions interval ease] :as card} quality]
  (when-not (and (integer? quality) (<= 0 quality 5)
                 (integer? repetitions) (<= 0 repetitions)
                 (integer? interval) (<= 0 interval)
                 (number? ease) (<= 1.3 ease))
    (throw (ex-info "Invalid card or review" {:quality quality :card card})))
  (let [new-ease (max 1.3 (+ ease
                             (- 0.1 (* (- 5 quality) (+ 0.08 (* (- 5 quality) 0.02))))))]
    (if (< quality 3)
      (assoc card :repetitions 0 :interval 1 :ease new-ease)
      (assoc card
             :repetitions (inc repetitions)
             :interval (cond (zero? repetitions) 1
                             (= repetitions 1) 6
                             :else (max 1 (Math/round (* interval ease))))
             :ease new-ease))))

(deftest review-test
  (let [card {:repetitions 0 :interval 0 :ease 2.5}]
    (is (= 1 (:interval (review card 5))))
    (is (= 1 (:repetitions (review card 5))))
    (is (= 0 (:repetitions (review card 1))))
    (is (thrown? Exception (review card 6)))))
