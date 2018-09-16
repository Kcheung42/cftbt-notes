(ns the-divine-cheese-code.visualization.svg
  (:require [clojure.string :as s])
  (:refer-clojure :exclude [min max]))

(defn comparator-over-maps
  "ToDo: Docstring"
  [comparison-fn ks]
  ;; returns a function that takes a map
  (fn [maps]
    ;; zip
    (zipmap ks
            (map (fn [k] (apply comparison-fn (map k maps)))
                 ks))))

(defn comparator-over-maps
  "Return a function that takes a map and returns a map of values filtered
  by the `comparison-fn`"
  [comparison-fn ks]
  ;; returns a function
  (fn [maps]
    (zipmap ks
            ;; need apply because map returns a list but comparison-fn
            ;; needs space seperated args
            (map #(apply comparison-fn (map % maps))
                 ks))))

(def min (comparator-over-maps clojure.core/min [:lat :lng]))
(def max (comparator-over-maps clojure.core/max [:lat :lng]))

(def min (comparator-over-maps clojure.core/min [:a :b]))
(min [{:a 1 :b 3} {:a 5 :b 0}])


(defn translate-to-00
  [locations]
  (let [mincoords (min locations)]
    (map #(merge-with - % mincoords) locations)))

 (defn scale
  [width height locations]
  (let [maxcoords (max locations)
        ratio {:lat (/ height (:lat maxcoords))
               :lng (/ width (:lng maxcoords))}]
    (map #(merge-with * % ratio) locations)))

(defn latlng->point
  "Convert lat/lng map to comma-separated string" 
  [latlng]
  (str (:lat latlng) "," (:lng latlng)))

(defn points
  "Given a seq of lat/lng maps, return string of ponits joined by space"
  [locations]
  (s/join " " (map latlng->point locations)))

(defn line
  [points]
  (str "<polyline points=\"" points "\" />"))

(defn transform
  "Just chains other functions"
  [width height locations]
  (->> locations
       translate-to-00
       (scale width height)))

(defn xml
  "svg 'template', which also flips the coordinate system"
  [width height locations]
  (str "<svg height=\"" height "\" width=\"" width "\">"
       ;; These two <g> tags change the coordinate system so that
       ;; 0,0 is in the lower-left corner, instead of SVG's default
       ;; upper-left corner
       "<g transform=\"translate(0," height ")\">"
       "<g transform=\"rotate(-90)\">"
       (-> (transform width height locations)
           points
           line)
       "</g></g>"
       "</svg>"))
