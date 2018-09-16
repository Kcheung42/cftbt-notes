;; Chapter 8 Writing Macros
;; legend:
;; fn = functions

;; Table of Contents:
;; ******************
;; Macros Are Essential
;; Anatomy of a Macro
;; Building Lists for Evaluation
;;;; Simple Quoting
;;;; Syntax Quoting
;; Using Syntax Quoting in a Macro
;; Refactoring a Macro and Unquote Splicing
;; Things to look out for
;;;; Variable Capture

;; *****************************************************************************
;; Macros Are Essential
;; *****************************************************************************

;; `when` is actually a Macro

;; fn = functions
(macroexpand '(when boolean-expression
                expression-1
                expression-2
                expression-3))
;; => (if boolean-expression
;; =>  (do expression-1
;; =>      expression-2
;; =>      expression-3))




;; *****************************************************************************
;; Anatomy of a Macro
;; *****************************************************************************

;; Consist Name, Docstring, Arg list, and Body
;; Q?: Can args be datastructures other than lists? (i.e maps)

(defmacro infix
  "Use this macro when you pine for the notation of your childhood"
  [infixed]
  (list (second infixed) (first infixed) (last infixed)))


;; One Key difference between fn and macros:
;; In fn args are fully evaluated before being passed to fn.
;; In macros args are unevaluated
(infix (1 + 1))
;; => 2

(macroexpand '(infix (1 + 1)))
;; => (+ 1 1)

;; Argument destructuring is supported
(defmacro infix-2
  [[operand1 op operand2]]
  (list op operand1 operand2))

;; Multiple-airty (number of args control which expression to evaluate)
(defmacro and
  "Evaluates exprs one at a time, from left to right. If a form
    returns logical false (nil or false), and returns that value and
    doesn't evaluate any of the other expressions, otherwise it returns
    the value of the last expr. (and) returns true."
  {:added "1.0"}
  ([] true)
  ([x] x)
  ([x & next]
   `(let [and# ~x]
      (if and# (and ~@next) and#))))
;; ignore ` and ~ for now. What is important is that this examples support a
;; 0-airity, 1-airity, and n-airity macro body




;; *****************************************************************************
;; Building Lists for Evaluation
;; *****************************************************************************

;; A Macro is all about building a list for clojure to evaluate.

;; Say you want a macro that takes an expression and both prints and returns
;; its value.
;; Return a list that looks like below:
(let [result expression]
  (println result)
  result)


;; You may be tempted to do this
(defmacro my-print-whoopsie
  [expression]
  (list let [result expression]
        (list println result)
        result))
;; => CompilerException java.lang.RuntimeException:
;; => Can't take value of a macro: #'clojure.core/let
;; Why? Macro tries to get value that symbol `let` refers to.
;; We want the symbol itself so use `'`

(defmacro my-print
  [expression]
  (list 'let ['result expression]
        (list 'println 'result)
        'result))
;; `'` turns off evaluation and instead returns the symbol




;; *****************************************************************************
;; Simple Quoting
;; *****************************************************************************

;; Will always use quoting within macros to obtain unevaluated symbol.

(+ 1 2)
;; => 3

(quote (+ 1 2))
;; => (+ 1 2)

+
;; => #<core$_PLUS_ clojure.core$_PLUS_@47b36583>

(quote +)
;; => +

;; Evaluating unbound (unassigned) symbol raises Exception
sweating-to-the-oldies
;; => Unable to resolve symbol: sweating-to-the-oldies in this context


;; But quoting the symbol returns a symbol regardless if symbol is bound
(quote sweating-to-the-oldies)
;; => sweating-to-the-oldies


;; `'` is  a quote character for (quote x)
'(+ 1 2)
;; => (+ 1 2)

'dr-jekyll-and-richard-simmons
;; => dr-jekyll-and-richard-simmons

;; Example
(defmacro when
  "Evaluates test. If logical true, evaluates body in an implicit do."
  {:added "1.0"}
  [test & body]
  (list 'if test (cons 'do body)))


(macroexpand '(when (the-cows-come :home)
                (call me :pappy)
                (slap me :silly)))
#_(=> if (the-cows-come :home)
      (do (call me :pappy)
          (slap me :silly)))


;; Another Example
(defmacro unless
  "Inverted 'if'"
  [test & branches]
  (conj (reverse branches) test 'if)) ;; conj adds args to the front of collection

(macroexpand '(unless (done-been slapped? me)
                      (slap me :silly)
                      (say "I reckon that'll learn me")))
#_(=> (if (done-been slapped? me)
        (say "I reckon that'll learn me")
        (slap me :silly)))




;; *****************************************************************************
;; Syntax Quoting
;; *****************************************************************************

;; Difference between simple quoting `'` and syntax Quoting ```?
;; syntax quoting:
;; 1. Returns fully qualified symbols (with namespace)
;;      - Reason: help avoid name collisions
;; 2. Recursively quotes all elements
;; 3. Allows unquoting of forms `~`

;; Normal quoting does not include namespace
'+
;; => +

;; write out namespace to include it
'clojure.core/+
;; => clojure.core/+

;; syntax quoting
`+
;; => clojure.core/+


'(+ 1 2)
;; => (+ 1 2)


;; Recursively quote all elements
`(+ 1 2)
;; => (clojure.core/+ 1 2)


;; with unquoting `~`
`(+ 1 ~(inc 1))
;; => (clojure.core/+ 1 2)

;; with-out unquoting `~`
`(+ 1 (inc 1))
;; => (clojure.core/+ 1 (clojure.core/inc 1))


;; syntax quoting is more concise than simple quoting
(list '+ 1 (inc 1))
;; => (+ 1 2)

                                        ;vs

`(+ 1 ~(inc 1))
;; => (clojure.core/+ 1 2)



;; *****************************************************************************
;; Using Syntax Quoting in a Macro
;; *****************************************************************************

;; Not using syntax quoting
(defmacro code-critic
  "Phrases are courtesy Hermes Conrad from Futurama"
  [bad good]
  (list 'doting
        (list 'println
              "Great squid of Madrid, this is bad code:"
              (list 'quote bad)) ;; you can't 'bad because you want bad evaluated for list function
        (list 'println
              "Sweet gorilla of Manila, this is good code:"
              (list 'quote good))))

(code-critic (1 + 1) (+ 1 1))
;; => Great squid of Madrid, this is bad code: (1 + 1)
;; => Sweet gorilla of Manila, this is good code: (+ 1 1)



;; Using syntax quoting
(defmacro code-critic
  "Phrases are courtesy Hermes Conrad from Futurama"
  [bad good]
  `(do (println "Great squid of Madrid, this is bad code:"
                (quote ~bad))
       (println "Sweet gorilla of Manila, this is good code:"
                (quote ~good))))

;; *****************************************************************************
;; Refactoring a Macro and Unquote Splicing
;; *****************************************************************************
;; We can improve code-critic from the previous section


;; moving macro guts to an outside function.
;; Notice criticize-code returns a syntax quoted list.
(defn criticize-code
  [criticism code]
  `(println ~criticism (quote ~code)))

(defmacro code-critic
  [bad good]
  `(do ~(criticize-code "Cursed bacteria of Liberia, this is bad code:" bad)
       ~(criticize-code "Sweet sacred boa of Western and Eastern Samoa, this is good code:" good)))

;; Apply the same function to a list of args?
;; Sounds like a job for `map`
(defmacro code-critic
  [bad good]
  `(do ~(map #(apply criticize-code %)
             [["Great squid of Madrid, this is bad code:" bad]
              ["Sweet gorilla of Manila, this is good code:" good]])))

;; However Something is Wrong!
(code-critic (1 + 1) (+ 1 1))
;; => NullPointerException

;; Why?
;; when map the elements in list returned by map gets evaluated
;; in the end

(do (clojure.core/println "criticism" '(1 + 1)))

;; The steps may look like this
(do
  ((clojure.core/println "criticism" '(1 + 1))
   (clojure.core/println "criticism" '(+ 1 1))))

(do
  (nil
   (clojure.core/println "criticism" '(+ 1 1))))


(do
  (nil nil))


`(+ ~(list 1 2 3))
;; => (clojure.core/+ (1 2 3))


`(+ ~@(list 1 2 3))
;; => (clojure.core/+ 1 2 3)


;; Concluseion: USE ~@ when you use map
(defmacro code-critic
  [{:keys [good bad]}] ;; not sure why the book put :keys in here
  `(do ~@(map #(apply criticize-code %)
              [["Sweet lion of Zion, this is bad code:" bad]
               ["Great cow of Moscow, this is good code:" good]])))
(code-critic {:good (+ 1 1) :bad (1 + 1)})

;; original code works too
(defmacro code-critic
  [good bad]
  `(do ~@(map #(apply criticize-code %)
              [["Sweet lion of Zion, this is bad code:" bad]
               ["Great cow of Moscow, this is good code:" good]])))

(code-critic (+ 1 1) (1 + 1))
;; => Sweet lion of Zion, this is bad code: (1 + 1)
;; => Great cow of Moscow, this is good code: (+ 1 1)




;; *****************************************************************************
;; Things to watch out for
;; *****************************************************************************

;; *****************************************************************************
;; Variable Capture
;; *****************************************************************************

;; Variable capture occurs when a macro introduces a binding, unknown to the
;; macro's user, eclipses an existing binding.


;; Not Normal behavior
;; message in the macro eclipses the outer message "Good Job"
(def message "Good job!")
(defmacro with-mischief
  [& stuff-to-do]
  (concat (list 'let ['message "Oh, big deal!"])
          stuff-to-do))

;; Non macro behavior
(def message "Cool")
(concat (let [message "oh, big deal!"]) (println "hello world" message))
;; => Here's how I feel about that thing you did: Cool


(with-mischief
  (println "Here's how I feel about that thing you did: " message))
;; => Here's how I feel about that thing you did: Oh, big deal!

(def message "Good job!")
(defmacro with-mischief
  [& stuff-to-do]
  `(let [message "Oh, big deal!"]
     ~@stuff-to-do))

(with-mischief
  (println "Here's how I feel about that thing you did: " message))
;; Exception: Can't let qualified name: user/message


;; Conclusion: If you want to use `let` bindings in your macros, use `gensym`.
(gensym)
;; => G__655

(gensym 'message)
;; => message4760

(defmacro without-mischief
  [& stuff-to-do]
  (let [macro-message (gensym 'message)]
    `(let [~macro-message "Oh, big deal!"]
       ~@stuff-to-do
       (println "I still need to say: " ~macro-message))))

(without-mischief
 (println "Here's how I feel about that thing you did: " message))
;; => Here's how I feel about that thing you did:  Good job!
;; => I still need to say:  Oh, big deal!


`(blarg# blarg#)
(blarg__2869__auto__ blarg__2869__auto__)

`(let [name# "Larry Potter"] name#)
;; => (clojure.core/let [name__2872__auto__ "Larry Potter"] name__2872__auto__)


(defmacro report
  [to-try]
  `(if ~to-try
     (println (quote ~to-try) "was successful:" ~to-try)
     (println (quote ~to-try) "was not successful:" ~to-try)))

;;;; Thread/sleep takes a number of milliseconds to sleep for
(report (do (Thread/sleep 1000) (+ 1 1)))


(if (do (Thread/sleep 1000) (+ 1 1))
  (println '(do (Thread/sleep 1000) (+ 1 1))
           "was successful:"
           (do (Thread/sleep 1000) (+ 1 1)))

  (println '(do (Thread/sleep 1000) (+ 1 1))
           "was not successful:"
           (do (Thread/sleep 1000) (+ 1 1))))


(defmacro report
  [to-try]
  `(let [result# ~to-try]
     (if result#
       (println (quote ~to-try) "was successful:" result#)
       (println (quote ~to-try) "was not successful:" result#))))


(report (= 1 1))
;; => (= 1 1) was successful: true

(report (= 1 2))
;; => (= 1 2) was not successful: false


(doseq [code ['(= 1 1) '(= 1 2)]]
  (report code))
;; => code was successful: (= 1 1)
;; => code was successful: (= 1 2)


(if
    code
  (clojure.core/println 'code "was successful:" code)
  (clojure.core/println 'code "was not successful:" code))


(defmacro doseq-macro
  [macroname & args]
  `(do
     ~@(map (fn [arg] (list macroname arg)) args)))

(doseq-macro report (= 1 1) (= 1 2))
;; => (= 1 1) was successful: true
;; => (= 1 2) was not successful: false


(def order-details
  {:name "Mitchard Blimmons"
   :email "mitchard.blimmonsgmail.com"})


(validate order-details order-details-validations)
;; => {:email ["Your email address doesn't look like an email address."]}


(def order-details-validations
  {:name
   ["Please enter a name" not-empty]

   :email
   ["Please enter an email address" not-empty

    "Your email address doesn't look like an email address"
    #(or (empty? %) (re-seq #"@" %))]})


(defn error-messages-for
  "Return a seq of error messages"
  [to-validate message-validator-pairs]
  (map first (filter #(not ((second %) to-validate))
                     (partition 2 message-validator-pairs))))


(error-messages-for "" ["Please enter a name" not-empty])
;; => ("Please enter a name")


(defn validate
  "Returns a map with a vector of errors for each key"
  [to-validate validations]
  (reduce (fn [errors validation]
            (let [[fieldname validation-check-groups] validation
                  value (get to-validate fieldname)
                  error-messages (error-messages-for value validation-check-groups)]
              (if (empty? error-messages)
                errors
                (assoc errors fieldname error-messages))))
          {}
          validations))

(validate order-details order-details-validations)
;; => {:email ["Your email address doesn't look like an email address"]}


(let [errors (validate order-details order-details-validations)]
  (if (empty? errors)
    (println :success)
    (println :failure errors)))


(defn if-valid
  [record validations success-code failure-code]
  (let [errors (validate record validations)]
    (if (empty? errors)
      success-code
      failure-code)))


(if-valid order-details order-details-validations errors
          (render :success)
          (render :failure errors))


(defmacro if-valid
  "Handle validation more concisely"
  [to-validate validations errors-name & then-else]
  `(let [~errors-name (validate ~to-validate ~validations)]
     (if (empty? ~errors-name)
       ~@then-else)))


(macroexpand
 '(if-valid order-details order-details-validations my-error-name
            (println :success)
            (println :failure my-error-name)))
(let*
    [my-error-name (user/validate order-details order-details-validations)]
  (if (clojure.core/empty? my-error-name)
    (println :success)
    (println :failure my-error-name)))
