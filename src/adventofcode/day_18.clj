(ns adventofcode.day-18
  (:require [clojure.math.combinatorics]))

(defn parse-lights
  "Converts a row of lights to a seq of boolean values,
  e.g., .#.## becomes [false true false true true]."
  [string]
  (replace {\. false \# true} (seq string)))

(defn in-corner?
  "Returns true if the point is in a corner of an n-by-n, else false."
  [n [x y]]
  (boolean
   (or (= 0 x y)
       (= (dec n) x y)
       (and (= x (dec n)) (= 0 y))
       (and (= y (dec n)) (= 0 x)))))

(def in-corner?-memo (memoize in-corner?))

(defn nearest-neighbors
  "Returns the nearest neighbors of a point [x y] in an n-by-n grid."
  [n point]
  (let [neighbor-fns
        (rest (clojure.math.combinatorics/selections [identity inc dec] 2))]
    (filter #(and (<= 0 (first %) (dec n)) (<= 0 (second %) (dec n)))
            (map #(map (fn [f p] (f p)) %1 %2)
                 neighbor-fns (repeat point)))))

(def nearest-neighbors-memo (memoize nearest-neighbors))

(defn neighbor-status
  "Given a grid and a point, returns a map with the number
  of nearest neighbors which are on and off, e.g., {true 3, false 5}"
  [grid point]
  (let [neighbors (nearest-neighbors-memo (count grid) point)]
    (frequencies
     (map (fn [[x y]] (nth (nth grid x) y)) neighbors))))

(defn animate-light
  "Returns the new value of a light at a point on the grid
  according to the status of it's neighbors."
  [grid [x y :as point]]
  (let [status (nth (nth grid x) y)
        neighbors-on (get (neighbor-status grid point) true 0)]
    (if (or (and (true? status) (< 1 neighbors-on 4))
            (and (false? status) (= 3 neighbors-on)))
      true false)))

(defn light-corners
  "Returns the new value of a light at a point on the grid:
  all corner lights are turned on, all others are unchanged."
  [grid [x y :as point]]
  (if (in-corner?-memo (count grid) point)
    true (nth (nth grid x) y)))

(defn step-grid
  "Returns a new grid where the value at each point
  is determined by the value of the step function
  applied to the point and the input grid."
  [step-fn grid]
  (let [grid-size (range (count grid))]
    (persistent!
     (reduce
      (fn [row x]
        (conj! row (persistent!
                    (reduce
                     (fn [column y]
                       (conj! column (step-fn grid [x y])))
                     (transient []) grid-size))))
      (transient []) grid-size))))

(defn solve
  "Given the input for the day, returns the solution."
  [input]
  (let [grid (map parse-lights
                  (clojure.string/split-lines input))
        num-on (comp #(get % true) frequencies flatten)]
    [(num-on (reduce #(step-grid animate-light %1 #_%2)
                     grid (range 100)))
     (num-on (reduce #(step-grid light-corners
                                 (step-grid animate-light %1) #_%2)
                     (step-grid light-corners grid) (range 100)))]))
