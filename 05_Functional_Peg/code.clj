;; Legend:
;; [X] = Bad
;; [Y] = Good
;; *****************************************************************************
;;; Pure functions are Referentially Transparent
;; *****************************************************************************

;; Pure functions rely on 2 things:
;; 1. their own arguments
;; 2. immutable values

;; Mathematical functions are referentially transparent
(+ 1 2)
;; => 3

;; Another Example
(defn wisdom
  [words]
  (str words ", Daniel-san"))

(wisdom "Always bathe on Fridays")
;; => "Always bathe on Fridays, Daniel-san"

;; not refferentially transparent
(defn year-end-evaluation
  []
  (if (> (rand) 0.5)
    "You get a raise!"
    "Better luck next year!"))

(defn analysis
  [text]
  (str "Character count: " (count text)))

(defn analyze-file
  [filename]
  (analysis (slurp filename)))

;; *****************************************************************************
;;; Pure Functions Have No Side Effects
;; *****************************************************************************

;; see code.js file




;; *****************************************************************************
;; Living with Immutable Data Structures
;; *****************************************************************************

;; *****************************************************************************
;;; Recursion instead of for/while
;; *****************************************************************************

(def great-baby-name "Rosanthony")
great-baby-name
;; => "Rosanthony"

(let [great-baby-name "Bloodthunder"]
  great-baby-name)
;; => "Bloodthunder"

great-baby-name
;; => "Rosanthony"

;; Sum Example with
(defn sum
  ([vals] (sum vals 0))
  ([vals accumulating-total]
   (if (empty? vals)
     accumulating-total
     (sum (rest vals) (+ (first vals) accumulating-total)))))

;; Here's what the function calls might look like
(sum [39 5 1]) ;; single-arity body calls two-arity body
(sum [39 5 1] 0)
(sum [5 1] 39)
(sum [1] 44)
(sum [] 45) ;; base case is reached, so return accumulating-total
;; => 45

;; Sum Example with recur.
;; Use recur so stack overflow does not happen
(defn sum
  ([vals] (sum vals 0))
  ([vals accumulating-total]
   (if (empty? vals)
     accumulating-total
     (recur (rest vals) (+ (first vals) accumulating-total)))))

;; Clojure's immutable data structures are implemented using structural sharing.
;; ToDo: understand structural sharing


;; *****************************************************************************
;;; Function Composition insteaad of Attribute Mutation
;; *****************************************************************************


;; Example of functional composition
(require '[clojure.string :as s])
(defn clean
  [text]
  (s/replace (s/trim text) #"lol" "LOL"))

(clean "My boa constrictor is so sassy lol!  ")
;; => "My boa constrictor is so sassy LOL!"

;; Instead of mutating an objects the clean function passes an immutable value
;; text to a pure function `s/trim` which interns returns an immutable value to
;; `s/replace` which returns a final immutable value




;; *****************************************************************************
;;; Cool Things to Do with Pure Functions
;; *****************************************************************************

;; [Y] use comp
((comp inc *) 2 3)
;; => 7

(def character
  {:name "Smooches McCutes"
   :attributes {:intelligence 10
                :strength 4
                :dexterity 5}})
(def c-int (comp :intelligence :attributes))
(def c-str (comp :strength :attributes))
(def c-dex (comp :dexterity :attributes))

(c-int character)
;; => 10

(c-str character)
;; => 4

(c-dex character)
;; => 5

;; [X] Without comp (not as elegant as using comp)
(fn [c] (:strength (:attributes c)))

(defn spell-slots
  [char]
  (int (inc (/ (c-int char) 2))))

(spell-slots character)
;; => 6

(def spell-slots-comp (comp int inc #(/ % 2) c-int))

(defn two-comp
  [f g]
  (fn [& args]
    (f (apply g args))))

(+ 3 (+ 5 8))

(+ 3 13)

(defn sleepy-identity
  "Returns the given value after 1 second"
  [x]
  (Thread/sleep 2000)
  x)

(sleepy-identity "Mr. Fantastico")
;; => "Mr. Fantastico" after 2 second

(sleepy-identity "Mr. Fantastico")
;; => "Mr. Fantastico" after 2 second


(def memo-sleepy-identity (memoize sleepy-identity))
(memo-sleepy-identity "Mr. Fantastico")
;; => "Mr. Fantastico" after 2 second

(memo-sleepy-identity "Mr. Fantastico")
;; => "Mr. Fantastico" immediately


;;; Peg Thing examples
;;; See peg-thing.clj for complete game

(defn tri*
  "Generates lazy sequence (infinite) of triangular numbers
  e.g. 1, 3, 6, 10, 15, etc"

  ;;;; set default sum and n using airity overloading
  ([] (tri* 0 1))
  ([sum n]
   (let [new-sum (+ sum n)]
     (cons new-sum (lazy-seq (tri* new-sum (inc n)))))))

(def tri (tri*))

(take 5 tri)
;; => (1 3 6 10 15)

(defn triangular?
  "Is the nubmer triangular? e.g. 1, 3, 6, 10, 15, etc"
  [n]
  (= n (last (take-while #(>= n %) tri))))

(triangular? 5)
;; => false

(triangular? 6)
;; => true
(defn row-tri
  "Get the triangular number at th end of nth row"
  [n]
  (last (take n tri)))

(row-tri 1)
;; => 1

(row-tri 2)
;; => 3

(row-tri 3)
;; => 6

(defn row-num
  "Given `pos` return the row number on the game board"
  [pos]
  (inc (count (take-while #(> pos %) tri))))

(row-num 1)
;; => 1
(row-num 5)
;; => 3

(defn connect
  ""
  [board max-pos pos neighbor dest]
  ;;;; test if destination is on the board
  (if (<= dest max-pos)
    (reduce (fn [new-board [p1 p2]]
              (assoc-in new-board [p1 :connections p2] neighbor))
            board
            ;;;; e.g. reduce over (1,4) (4,1) so you build connection both ways
            [[pos dest] [dest pos]])
    board))

(connect {} 15 1 2 4)
;; => {1 {:connections {4 2}}
;; =>  4 {:connections {1 2}}}

(assoc-in {} [:cookie :monster :vocals] "Finntroll")
;; => {:cookie {:monster {:vocals "Finntroll"}}}

(get-in {:cookie {:monster {:vocals "Finntroll"}}} [:cookie :monster])
;; => {:vocals "Finntroll"}

(assoc-in {} [1 :connections 4] 2)
;; => {1 {:connections {4 2}}}

(defn connect-right
  [board max-pos pos]
  (let [neighbor (inc pos)
        destination (inc neighbor)]
    (if-not (or (triangular? neighbor) (triangular? pos))
      (connect board max-pos pos neighbor destination)
      board)))

(defn connect-down-left
  [board max-pos pos]
  (let [row (row-num pos)
        neighbor (+ pos row)
        destination (+ 1 row neighbor)]
    (connect board max-pos pos neighbor destination)))

(connect-down-left {} 15 1)
;; => {1 {:connections {4 2}
;; =>  4 {:connections {1 2}}}}
(defn connect-down-right
  [board max-pos pos]
  (let [row (row-num pos)
        neighbor (+ 1 pos row)
        destination (+ 2 row neighbor)]
    (connect board max-pos pos neighbor destination)))

(connect-down-right {} 15 3)
;; => {3  {:connections {10 6}}
;; =>  10 {:connections {3 6}}}

(defn add-pos
  [board max-pos pos]
  (let [pegged-board (assoc-in board [pos :pegged] true)]
    (reduce (fn [new-board connection-creation-fn]
              (connection-creation-fn new-board max-pos pos))
            pegged-board
            [connect-right connect-down-left connect-down-right])))

(add-pos {} 15 1)
{1 {:connections {6 3, 4 2}, :pegged true}
 4 {:connections {1 2}}
 6 {:connections {1 3}}}

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

(defn pegged?
  "Does the position have a peg in it?"
  [boards pos]
  (get-in boards [pos :pegged]))

(defn remove-peg
  "Remove a peg from the board at given position.
  Return board."
  [board pos]
  (assoc-in board [pos :pegged] false))

(defn place-peg
  "Put a peg in the board at given position.
  Return board."
  [board pos]
  (assoc-in board [pos :pegged] true))

(defn move-peg
  "Take peg out of p1 and put it into p2.
  Return board."
  [board p1 p2]
  (place-peg (remove-peg board p1) p2))

;; Note none of these mutate the original board.
;; Only a new updated board is returned from the function.

;; (defn valid-moves-reduce;;;; does not work
;;   "Return a map of all valid moves for pos, where the key
;;   is the destination and the value is the jumped position"
;;   [board pos]
;;   (reduce (fn [valid-list [destination jumped]]
;;             (if (assoc valid-list [destination jumped])
;;               (and (not (pegged? board destination)
;;                           (pegged? board jumped)))
;;               valid-list))
;;           {}
;;           (get-in board [pos :connections])))

(defn valid-moves
  "Return a map of all valid moves for pos, where the key is the
  destination and the value is the jumped position"
  [board pos]
  (into {}
        (filter (fn [[destination jumped]]
                  (and (not (pegged? board destination))
                       (pegged? board jumped)))
                (get-in board [pos :connections]))))

(valid-moves (remove-peg (new-board 5) 4) 1)

(get (valid-moves (remove-peg (new-board 5) 4) 1) 4)

(def my-board (remove-peg (new-board 5) 4))

(valid-moves my-board 1)  ;; => {4 2}
(valid-moves my-board 6)  ;; => {4 5}
(valid-moves my-board 11) ;; => {4 7}
(valid-moves my-board 5)  ;; => {}
(valid-moves my-board 8)  ;; => {}


(defn valid-move?
  [board p1 p2]
  (get (valid-moves board p1) p2))


(valid-move? my-board 8 4) ;; => nil
(valid-move? my-board 1 4) ;; => 2

(defn make-move
  [board p1 p2]
  (if-let [jumped (valid-move? p1 p2)]
    (move-peg (remove-peg board jumped) p1 p2)))


;; (filter #(get (second %) :pegged) my-board)
;; (map first (filter #(get (second %) :pegged) my-board))

(defn can-move?
  [board]
  (some (comp not-empty (partial valid-moves board))
        (map first (filter #(get (second %) :pegged) board))))

;; Really cool. Composing a new function with a list of fucntions.
;; Then create a partial function since the arg board is always the same
;; for when we want to run this function across all the positions.
;; some will run this function across our positions passing in a different
;; second argument (pos) to valid-moves

(def alpha-start 97)
(def alpha-end 123)
(def letters (map (comp str char) (range alpha-start alpha-end)))
(def pos-chars 3)

(defn render-pos
  [board pos]
  (str (nth letters (dec pos))
       (if (get-in [pos :pegged])
         "0"
         "-")))
(defn row-positions
  "Return all positions in the given row"
  [row-num]
  (range (inc (or (row-tri (dec row-num)) 0))
         (inc (row-tri row-num))))

(row-positions 5)

(defn row-padding
  "String of spaces to add to the beginning of a row to center it"
  [row-num rows]
  (let [pad-length (/ (* (- rows row-num) pos-chars) 2)]
    (apply str (take pad-length (repeat " ")))))

(defn render-row
  [board row-num]
  (str (row-padding row-num (:rows board))
       (clojure.string/join " " (map (partial render-pos board)
                                     (row-positions row-num)))))


(characters-as-strings "a   b")
;; => ("a" "b")

(characters-as-strings "a   cb")
;; => ("a" "c" "b")


;; *****************************************************************************
;;; Chapter Excercises
;; *****************************************************************************

;; Excercise 1:
;; You used (comp :intelligence :attributes) to create a function that returns
;; a character’s intelligence. Create a new function, attr, that you can call
;; like (attr :intelligence) and that does the same thing.

(def character
  {:name "Smooches McCutes"
   :attributes {:intelligence 10
                :strength 4
                :dexterity 5}})

(defn attr
  ([attribute] (attr attribute :attributes))
  ([key1 key2]
   (comp key1 key2)))

((attr :intelligence) character)
;; => 10



;; Excercise 2
;; Implement the `comp` function

(defn my-comp
  ;; accept any number of arguments
  [& f]
  (fn [& args]
    (reduce (fn [result-so-far next-fn] (next-fn result-so-far))
            (apply (last f) args)
            (rest (reverse f)))))

((comp inc inc inc inc *) 2 3 )
((my-comp inc inc inc inc *) 2 3 )
