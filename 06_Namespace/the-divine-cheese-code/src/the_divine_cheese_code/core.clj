
;; (ns the-divine-cheese-code.core)

;; require takes a symbol designating a namespace and ensures that the namespace
;; exists and is ready to be used;
;; (require '[the-divine-cheese-code.visualization.svg :as svg])

;; After requiring the namespace, you can refer it so that you don’t have to
;; use fully qualified names to reference the functions.
;; (refer 'the-divine-cheese-code.visualization.svg)

;; using `alias` allows using an alias for the namespace instead of having to use the
;; full name
;; (alias 'svg 'the-divine-cheese-code.visualization.svg)
;; (svg/points heists)

;; `use` is equivalent to using `require` and then `refer`
;; (use '[the-divine-cheese-code.visualization.svg :as svg])

;; you never have to quote symbols in ns
(ns the-divine-cheese-code.core
  (:require [clojure.java.browse :as browse]
            [the-divine-cheese-code.visualization.svg :refer [xml]])
  (:gen-class))

(def heists [{:location "Cologne, Germany"
              :cheese-name "Archbishop Hildebold's Cheese Pretzel"
              :lat 50.95
              :lng 6.97}
             {:location "Zurich, Switzerland"
              :cheese-name "The Standard Emmental"
              :lat 47.37
              :lng 8.55}
             {:location "Marseille, France"
              :cheese-name "Le Fromage de Cosquer"
              :lat 43.30
              :lng 5.37}
             {:location "Zurich, Switzerland"
              :cheese-name "The Lesser Emmental"
              :lat 47.37
              :lng 8.55}
             {:location "Vatican City"
              :cheese-name "The Cheese of Turin"
              :lat 41.90
              :lng 12.45}])

;; (defn -main
;;   [& args]
  ;; (println (points heists)))

(defn url
  [filename]
  (str "file:///"
       (System/getProperty "user.dir")
       "/"
       filename))

(defn template
  [contents]
  (str "<style>polyline { fill:none; stroke:#5881d8; stroke-width:3}</style>"
       contents))

(defn -main
  [& args]
  (let [filename "map.html"]
    (->> heists
         (xml 50 100)
         template
         (spit filename))
    (browse/browse-url (url filename))))
