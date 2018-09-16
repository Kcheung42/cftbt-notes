;; ********************** Creating Board ************************************

(defn tri*
  "Generates lazy sequence (infinite) of triangular numbers.
  e.g. 1, 3, 6, 10, 15, etc."
  ;; set default sum and n using airity overloading
  ([] (tri* 0 1))
  ([sum n]
   (let [new-sum (+ sum n)]
     (cons new-sum (lazy-seq (tri* new-sum (inc n)))))))

(def tri (tri*))

(defn triangular?
  "Given `n` return the nth triangular number.
  e.g. 1, 3, 6, 10, 15, etc."
  [n]
  (= n (last (take-while #(>= n %) tri))))

(defn row-tri
  "Given `n`th row return the triangular at the end of that row."
  [n]
  (last (take n tri)))

(defn row-num
  "Given `pos` return the row number on the game board."
  [pos]
  (inc (count (take-while #(> pos %) tri))))

(defn connect
  "Add a new connection into the board map.
  A connection is a valid destination jumping over one peg and remaning on the
  board."
  [board max-pos pos neighbor dest]
  ;; test if destination is on the board
  (if (<= dest max-pos)
    (reduce (fn [new-board [p1 p2]]
              (assoc-in new-board [p1 :connections p2] neighbor))
            board
            ;; e.g. reduce over (1,4) (4,1) so you build connection both ways
            [[pos dest] [dest pos]])
    ;; else return board
    board))

(defn connect-right
  "Add a connection horizontally towards the right"
  [board max-pos pos]
  (let [neighbor (inc pos)
        destination (inc neighbor)]
    ;; test if my neighbor or pos is the end of the row
    (if-not (or (triangular? neighbor) (triangular? pos))
      (connect board max-pos pos neighbor destination)
      board)))

(defn connect-down-left
  "Add a connection downwards towards the left"
  [board max-pos pos]
  (let [row (row-num pos)
        neighbor (+ pos row)
        destination (+ 1 row neighbor)]
    (connect board max-pos pos neighbor destination)))

(defn connect-down-right
  "Add a connection downwards towards the right"
  [board max-pos pos]
  (let [row (row-num pos)
        neighbor (+ 1 pos row)
        destination (+ 2 row neighbor)]
    (connect board max-pos pos neighbor destination)))

(connect-down-right {} 15 3)

(defn add-pos
  ""
  [board max-pos pos]
  (let [pegged-board (assoc-in board [pos :pegged] true)]
    (reduce (fn [new-board connection-creation-fn]
              (connection-creation-fn new-board max-pos pos))
            pegged-board
            [connect-right connect-down-left connect-down-right])))

(defn new-board
  "Creates a new board given number of rows"
  [rows]
  (let [initial-board {:rows rows}
        max-pos (row-tri rows)]
    (reduce (fn [new-board pos]
              (add-pos new-board max-pos pos))
            initial-board
            (range 1 (inc max-pos)))))

(new-board 5)
(filter :pegged (vals (new-board 5)))


;; ********************** Moving Peg ************************************
;; This section defines functions to check for valid moves and to update the
;; board with new moves

(defn pegged?
  "Pegged? takes a `board` and a `pos` and checks if the `:pegged` value is
  true."
  [boards pos]
  (get-in boards [pos :pegged]))

(defn remove-peg
  "Remove-peg takes a `board` and a `pos` and overwrites the `pegged` value to
  false."
  [board pos]
  (assoc-in board [pos :pegged] false))

(defn place-peg
  "Remove-peg takes a `board` and a `pos` and overwrites the `pegged` value to
  true."
  [board pos]
  (assoc-in board [pos :pegged] true))

(defn move-peg
  "Take peg out of `p1` and put it into `p2`.
  Return board."
  [board p1 p2]
  (place-peg (remove-peg board p1) p2))

(defn valid-moves
  "Return a map of all valid moves (e.g destination is not pegged and jumped
  has a peg) for `pos`, where the key is the
  destination and the value is the jumped position"
  [board pos]
  (into {}
        (filter (fn [[destination jumped]]
                  (and (not (pegged? board destination))
                       (pegged? board jumped)))
                (get-in board [pos :connections]))))

(defn valid-move?
  [board p1 p2]
  (get (valid-moves board p1) p2))

(defn make-move
  [board p1 p2]
  (if-let [jumped (valid-move? board p1 p2)]
    (move-peg (remove-peg board jumped) p1 p2)))

(defn can-move?
  [board]
  (some (comp not-empty (partial valid-moves board))
        (map first (filter #(get (second %) :pegged) board))))

;; ********************** Display ************************************

;;creating the alphabet
(def alpha-start 97)
(def alpha-end 123)
(def letters (map (comp str char) (range alpha-start alpha-end)))
(def pos-chars 3)

(defn render-pos
  [board pos]
  (str (nth letters (dec pos))
       (if (get-in board [pos :pegged])
         "0"
         "-")))

(defn row-positions
  "Return all positions in the given row"
  [row-num]
  (range (inc (or (row-tri (dec row-num)) 0))
         (inc (row-tri row-num))))

(row-positions 5)
;; =>(11 12 13 14 15)

(defn row-padding
  "String of spaces to add to the beginning of a row to center it"
  [row-num rows]
  (let [pad-length (/ (* (- rows row-num) pos-chars) 2)]
    (apply str (take pad-length (repeat " ")))))

(row-padding 1 5)

(defn render-row
  [board row-num]
  (str (row-padding row-num (:rows board))
       (clojure.string/join " " (map (partial render-pos board)
                                     (row-positions row-num)))))
(defn print-board
  [board]
  (doseq [row-num (range 1 (inc (:rows board)))]
    (println(render-row board row-num))))

;; ********************** Player Interaction **************************
;;;; Player Interactions

(defn letter->pos
  [letter]
  (inc (- (int (first letter)) alpha-start)))

(defn get-input
  "Waits for user to enter text and hit enter, then cleans input"
  ([] (get-input nil)) ;;arity overload to supply default nil
  ([default]
   ;; read next line from input stream and clean whitespace
   (let [input (clojure.string/trim (read-line))]
     (if (empty? input)
       default
       (clojure.string/lower-case input)))))

(defn characters-as-strings
  "take in a string and return a collection of letters with all
  nonalphabetic input discarded"
  [string]
  (map #(str %)
       (filter
        #(and (>= (int %) alpha-start) (<= (int %) alpha-end))
        (seq string))))

(characters-as-strings "a     bbc dd")

(defn user-entered-valid-move
  "Handle next step after a user has entered a valid move"
  [board]
  (if (can-move? board)
    (prompt-move board)
    (end-game board)))

(defn user-entered-invalid-move
  "Handle next step after a user has entered a invalid move"
  [board]
  (println "\n!! This was an invalid move: \n"
           (prompt-move board)))

(defn prompt-move
  "Given the state of the `board`, wait for user to input a move.
  If it is a valid move then return board with updated move otherwise return
  original board"
  [board]
  (println "\nHere's your board:")
  (print-board board)
  (println "Move from where to where? Enter two letters:")
  (let [input (map letter->pos (characters-as-strings (get-input)))]
    (if-let [newboard (make-move board (first input) (second input))]
      (user-entered-valid-move newboard)
      (user-entered-invalid-move board))))


(defn prompt-empty-peg
  "Given the state of the `board` prompt use"
  [board]
  (println "Here's your board:")
  (print-board board)
  (println "which peg to remove? [e]")
  (prompt-move (remove-peg board (letter->pos (get-input "e")))))

(defn prompt-rows
  []
  (println "How many rows do you want? [5]")
  (let [rows (Integer. (get-input 5))
        board (new-board rows)]
    (prompt-empty-peg board)))


(defn end-game
  ""
  [board]
  (let [remaining-pegs (count (filter :pegged (vals board)))]
    (println "Game Over you had " remaining-pegs " pegs left:")
    (print-board board)
    (println "Play again? y/n [y]")
    (let [input (get-input "y")]
      (if (= (first input) "y")
        (prompt-rows)
        (do
          (println "Bye!")
          (System/exit 0))))))
