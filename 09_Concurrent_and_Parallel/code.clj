;; Chapter 9 Concurrent and Parallel Programming

;; Table of Contents:
;; ******************

;; *****************************************************************************
;; Summary
;; *****************************************************************************

;; - Concurrency: Managing multiple tasks
;; - Parallelism: execute taks simultaneously
;; - Three concurrency risks:
;;   - Reference Cell
;;   - Mutual Exclusion
;;   - Deadlock
;; - Three Tools:
;;   - Future: Define task, execute, but get results later.
;;   - Delays: Define task, but execute and get results later
;;   - Promises: Express you require a result without knowing about the task that
;;               produces the result.




;; *****************************************************************************
;; Concurrency and Parallelism Concepts
;; *****************************************************************************

;;; Managing Multiple Tasks vs Executing Tasks Simultaneously
;; *****************************************************************************

;; Concurrency: (managing multiple tasks)
;; - Concurrency: the managment (interleaving) of tasks
;; - Task do not need to be fully completed before switching

;; Parallelism: (execute tasks simultaneously)
;; - is a subclass of concurrency



;;; Blocking and Asynchronous Task
;; *****************************************************************************

;; - Blocking: Halt program, wait for operation to finish
;; - Asynchronous: proccess operations as they come, do not wait for previous ones
;;               to finish.



;; *****************************************************************************
;; Clojure Implementation: JVM Threads
;; *****************************************************************************





;;;; What's Thread
;; *****************************************************************************

;; - a thread is a subprogram
;; - a program can have many threads
;; - thread share program state
;; - threads can spawn threads
;; - Processing is Non-deterministic, can't know the execution order





;;;; The Three Goblins: Reference cells, Mutual Exclusion, and Deadlock
;; *****************************************************************************

;; Reference Cell:
;; Occurs when two threads can read and write to the same location, and the value
;;at the location depends on the order of the reads and writes.

;; Mutial Exclusion:
;; Occurs when multiple threads try to write to a file. Without a way to claim
;; exclusive write access to a file stuff written to the file end up mixed up.

;; Deadlock:
;; A state when each member of a group is waiting for another member to take action.




;; *****************************************************************************
;; Futures Delays, and Promises
;; *****************************************************************************





;; Futures
;; *****************************************************************************

;; (future & body)

;; Takes a body of expressions and yields a future object that will
;; invoke the body in another thread, and will cache the result and
;; return it on all subsequent calls to deref/@. If the computation has
;; not yet finished, calls to deref/@ will block, unless the variant of
;; deref with timeout is used. See also - realized?.

(future (Thread/sleep 4000)
        (println "I'll print after 4 seconds"))
(println "I'll print immediately")


;; Future returns reference value
;; To get result, use `deref` or `@`
(let [result (future (println "this prints once")
                     (+ 1 1))]
  (println "deref: " (deref result))
  (println "@: " @result))
                                        ; => "this prints once"
                                        ; => deref: 2
                                        ; => @: 2


(let [result (future (Thread/sleep 3000)
                     (+ 1 1))]
  (println "The result is: " @result)
  (println "It will be at least 3 seconds before I print"))
                                        ; => The result is: 2
                                        ; => It will be at least 3 seconds before I print


;; To place a limit on how long to wait for future:
;; supply deref with time limit and default return
(deref (future (Thread/sleep 1000) "result") 10 "default Return")
                                        ; => "default Return"

;; To interogate future and see if it's done running
(realized? (future (Thread/sleep 1000)))
                                        ; => false

(let [f (future)]
  @f
  (realized? f))
;; => true

;; Futures Summary:
;; - chuck tasks to another thread
;; - More flexibility by giving control when a task result is required
;; - When deref, evaluation is stopped until result obtained
;; - Allows you to treat task definition and requiring the result





;; Delays
;; *****************************************************************************

;; Dealays allow you to define a task without having to execute or require a
;; result immediately.

(def jackson-5-delay
  (delay (let [message "Just call my name and I'll be there"]
           (println "First deref:" message)
           message)))

;; to dereference, use `force` generally.
(force jackson-5-delay)
;; => First deref: Just call my name and I'll be there
;; => "Just call my name and I'll be there"

;; but `deref` and `@` works too.
(deref jackson-5-delay)
;; => "Just call my name and I'll be there"

@jackson-5-delay
;; => "Just call my name and I'll be there"

;; Notice delayis only run once, result is cached. Just like futures.


;; Example Use case:
;; Delay email to user until the first upload-document executes.
(def gimli-headshots ["serious.jpg" "fun.jpg" "playful.jpg"])
(defn email-user
  [email-address]
  (println "Sending headshot notification to" email-address))
(defn upload-document
  "Needs to be implemented"
  [headshot]
  true)
(let [notify (delay (email-user "and-my-axe@gmail.com"))]
  (doseq [headshot gimli-headshots] ;; see definition doseq
    (future (upload-document headshot)
            (force notify))));; even though execute 3X, delay returns cached
;; Note future spawns other threads to upload-document headshot.


;; (doseq seq-exprs & body)
;; Repeatedly executes body (presumably for side-effects) with
;; bindings and filtering as provided by "for".  Does not retain
;; the head of the sequence. Returns nil.


;; Promises
;; *****************************************************************************

;; Promises allow you to express that you expect a result without haing to define
;; a task or when that task should produce it. Create a promise using `promise` and
;; delicer result using `deliver`. Deref to get result

(def my-promise (promise))
(deliver my-promise (+ 1 2))
@my-promise
;; => 3

;; If you deref before deliver, program will block until promise is delivered


;; Example Set
(def yak-butter-international
  {:store "Yak Butter International"
   :price 90
   :smoothness 90})
(def butter-than-nothing
  {:store "Butter Than Nothing"
   :price 150
   :smoothness 83})
;;;; This is the butter that meets our requirements
(def baby-got-yak
  {:store "Baby Got Yak"
   :price 94
   :smoothness 99})

(defn mock-api-call
  [result]
  (Thread/sleep 1000)
  result)

(defn satisfactory?
  "If the butter meets our criteria, return the butter, else return false"
  [butter]
  (and (<= (:price butter) 100)
       (>= (:smoothness butter) 97)
       butter))

;; (some pred coll)
;; Returns the first logical true value of (pred x) for any x in coll,
;; else nil.  One common idiom is to use a set as pred, for example
;; this will return :fred if :fred is in the sequence, otherwise nil:
;; (some #{:fred} coll)

(time (some (comp satisfactory? mock-api-call)
            [yak-butter-international butter-than-nothing baby-got-yak]))
;; => "Elapsed time: 3002.132 msecs"
;; => {:store "Baby Got Yak", :smoothness 99, :price 94}

;; We use future and promises to be more timely.
(time
 (let [butter-promise (promise)]
   (doseq [butter-url [yak-butter-international butter-than-nothing baby-got-yak]]
     (future (if-let [satisfactory-butter (satisfactory? (mock-api-call butter-url))]
               (deliver butter-promise satisfactory-butter))))
   (println "And the winner is:" @butter-promise)
   @butter-promise))
;; => "Elapsed time: 1002.652 msecs"
;; => And the winner is: {:store Baby Got Yak, :smoothness 99, :price 94}
;; => {:store "Baby Got Yak", :price 94, :smoothness 99}


;; You can set timeout if you don't want a deref to hang indefinitely
(let [p (promise)]
  (deref p 100 "timed out"))


;; Callback just like in javascript
(let [ferengi-wisdom-promise (promise)]
  (future (println "Here's some Ferengi wisdom:" @ferengi-wisdom-promise)) ;#1
  (Thread/sleep 100)
  (deliver ferengi-wisdom-promise "Whisper your way to success.")) ;#2
;; => Here's some Ferengi wisdom: Whisper your way to success.

;; This spawns a new thread and blocks until the promise gets delivered from #2

;; Rolling Your Own Queue
;; *****************************************************************************

(defmacro wait
  "Sleep `timeout` seconds before evaluating body"
  [timeout & body]
  `(do (Thread/sleep ~timeout) ~@body))


(let [saying3 (promise)]
  (future (deliver saying3 (wait 100 "Cheerio!")))
  @(let [saying2 (promise)];dereference saying2
     (future (deliver saying2 (wait 400 "Pip pip!")))
     @(let [saying1 (promise)] ;dereference saying1
        (future (deliver saying1 (wait 200 "'Ello, gov'na!")))
        (println @saying1)
        saying1)
     (println @saying2)
     saying2)
  (println @saying3)
  saying3)
;; notice nested let functions. Good chance to use the `->` macro
;; passing a dereference future


;; Use thread-first macro `->`
(-> (enqueue saying (wait 200 "'Ello, gov'na!") (println @saying))
    (enqueue saying (wait 400 "Pip pip!") (println @saying))
    (enqueue saying (wait 100 "Cheerio!") (println @saying)))

(defmacro enqueue
  ([q concurrent-promise-name concurrent serialized]
   ())
  ([concurrent-promise-name concurrent serialized]
   ()))

;; Macro prototype could look like this:
;; (enqueue [concurrent-promise-name concurrent serialized])
(defmacro enqueue
  ([q concurrent-promise-name concurrent serialized] ;#1
   `(let [~concurrent-promise-name (promise)] ;#2
      (future (deliver ~concurrent-promise-name ~concurrent))
      (deref ~q) ;#3
      ~serialized
      ~concurrent-promise-name))
  ([concurrent-promise-name concurrent serialized] ;#4
   `(enqueue (future) ~concurrent-promise-name ~concurrent ~serialized)))

;; #1 The first-airity is where the work begins. Contains arg `q`.
;; #4 Second-airity calls the first and supplies (future) into `q`. This
;; #2 - Returns a form that creates a promise.
;;    - Delivers its value in a future.
;;    - Returns a promise.
;; #3 deref q, if `q` will usually be a nested let expression returned by
;;    another enqueue. If no value is supplied for `q` the macro supplies a
;;    future so deref does not cause an exception.

;; How `->` pass values
;; Takes an initial value as its first argument, -> threads it through one or more expressions.
;; Macro then inserts the first value returned to the next function as the first argument.

(time @(-> (enqueue saying (wait 200 "'Ello, gov'na!") (println @saying))
           (enqueue saying (wait 400 "Pip pip!") (println @saying))
           (enqueue saying (wait 100 "Cheerio!") (println @saying))))
;; => 'Ello, gov'na!
;; => Pip pip!
;; => Cheerio!
;; => "Elapsed time: 401.635 msecs"




;; *****************************************************************************
;; Excercises
;; *****************************************************************************

;; 1. Write a function that takes a string as an argument and searches for it
;; on Bing and Google using the slurp function. Your function should return the
;; HTML of the first page returned by the search.

(defn goog-search-url
  [search-term]
  (str "https://www.google.com/search?hl=en&q=" search-term))

(defn bing-search-url [search-term]
  (str "https://www.bing.com/search?q=" search-term))

(defn search
  [search-string]
  (let [result (promise)]
    (doseq [url [goog-search-url bing-search-url]]
      (future (if-let [first-html (slurp (url search-string))]
                (deliver result first-html))))
    @result))

(search "cat")



;; 2. Update your function so it takes a second argument consisting of the
;; search engines to use.

;; Asummption the second argument is a list of keys
(defn search-from-engines
  "ToDo: Docstring"
  [search-string engine-names]
  (let [result (promise)
        engines {:google goog-search-url
                 :bing bing-search-url}]
    (doseq [engine-name engine-names]
      (future (if-let [first-html (slurp ((get engines engine-name) search-string))]
                (deliver result first-html))))
    @result))

(search-from-engines "cat" (list :google :bing))


;; 3. Create a new function that takes a search term and search engines as
;; arguments, and returns a vector of the URLs from the first page of search
;; results from each search engine.cat
