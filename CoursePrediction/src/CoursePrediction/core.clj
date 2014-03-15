(ns CoursePrediction.core)
(use 'clj-ml.io)
(use '[ring.adapter.jetty :only (run-jetty)])
(use '[ring.util.response :as response])
(use '[ring.middleware.params :only (wrap-params)])    ;optional
(use '[compojure.core])                                ;optional
(require 'compojure.route)                             ;optional
(use '[korma.db])
(use '[clojure.core])
(use '[korma.core])
(use '[ring.middleware.json])
(require '[net.cgrand.enlive-html :as en])
(import java.net.URL)
(require '[clojure.java.jdbc :as jdbc] )
(require '[clojure.string :as str]
      ;   '[clojure.data.json :as json]
      ;   '[cheshire.core :as json]
         )

(def db {:classname "org.sqlite.JDBC",
:subprotocol "sqlite",
:subname "db.sq3"})

(defdb korma-db db)

(declare course prereq)

(defentity course
  (pk :num)

  (table :course )

  (database db)

  (entity-fields :num :name :credits)
  (many-to-many course :prereq
                {:lfk :num
                 :rfk :prereq_num})

 )

(defentity prereq
  (pk [:num :prereq_num])

  (table :prereq )

  (database db)

  (entity-fields :num :prereq_num)

 )

(defn create-course-table []
  (jdbc/with-connection
    db
    (jdbc/create-table
     :course
      [:num :integer "PRIMARY KEY"]
      [:name "varchar(32)"]
      [:credits "tinyint"])
   ))

(defn create-prereq-table []
  (jdbc/with-connection
    db
    (jdbc/create-table
     :prereq
      [:num :integer]
      [:prereq_num]
      ["PRIMARY KEY" "(num, prereq_num)"])
   ))

(defn add-course [num name credits] ()
  (insert course
      (values {:num num :name name :credits credits})))

(defn add-prereq [num prenum]
  (insert :prereq (values {:num num :prereq_num prenum}))
 )

(defn table-exists [table-name]
  (not (nil? (jdbc/with-connection db (jdbc/with-query-results rs
             [(str "SELECT name FROM sqlite_master WHERE type = \"table\" and name LIKE \"" table-name "\"")] (first rs))))
))



(defn preload-courses []
  (add-course 210 "CIS-210" 4 )
  (add-course 110 "CIS-110" 4 )
  (add-course 105 "CIS-105" 4 )
  (add-course 310 "CIS-310" 4 )
  (add-course 315 "CIS-315" 4)
  (add-course 313 "CIS-313" 4)
  (add-course 432 "CIS-432" 4)

)

(defn preload-prereqs []

  (add-prereq 315 310)
  (add-prereq 315 313)
  (add-prereq 310 210)
  (add-prereq 310 110)
  (add-prereq 313 310)
  (add-prereq 432 315)
  (add-prereq 110 105)
)

(defn init_database[]
  (do (if-not (table-exists "course") (do (create-course-table)(preload-courses)))
      (if-not (table-exists "prereq") (do (create-prereq-table)(preload-prereqs)))
   )
)

(init_database)
(defn get-course-pre [num]
  (select course
          (with course)        ;appears to be including the many-to-many field in course entity def
          (where {:num num}) ))

(defn app2 [request] (println request){:body (str request)})

;(def server (run-jetty #'app2 {:port 8080 :join? false}))

(defn q2r [query-string]
  (second (clojure.string/split query-string #"="))
  )


(defn app2
  [{:keys [uri query-string]}]
  {:body (format "%s" (apply str (get-course-pre (q2r query-string))))}
  )

(defn app2*
    [{:keys [uri params]}]
    {:body (format "Hello world %s with %s" uri params)})


#_(en/deftemplate homepage
   (en/xml-resource "homepage.html")
   [request]
)


(def totalStep (atom 0))
(def currentBoard (atom {}))

(def boardSize (atom 3))

(defn resetBoard [size] 
  (loop [i 0 board []]
    (if (>= i size)  
      (reset! currentBoard board)
      (recur (inc i) (conj board (vec (range 0 size))))
 
   )
  )
  
)

(defn find-ox [r c board]
  (get (get board r) c)
  )

(defn win-detect [r c board OorX]
  (cond 
    (= (find-ox r c board) (find-ox (+ r 1) c board) (find-ox (+ r 2) c board) OorX) true
    (= (find-ox r c board) (find-ox (+ r 1) c board) (find-ox (- r 1) c board) OorX) true
    (= (find-ox r c board) (find-ox (- r 1) c board) (find-ox (- r 2) c board) OorX) true
    (= (find-ox r c board) (find-ox (+ r 1) (+ c 1) board) (find-ox (+ r 2) (+ c 2) board) OorX) true
    (= (find-ox r c board) (find-ox (+ r 1) (+ c 1) board) (find-ox (- r 1) (- c 1) board) OorX) true
    (= (find-ox r c board) (find-ox (- r 2) (- c 2) board) (find-ox (- r 1) (- c 1) board) OorX) true
    (= (find-ox r c board) (find-ox r (+ c 1) board) (find-ox r (+ c 2) board) OorX) true
    (= (find-ox r c board) (find-ox r (+ c 1) board) (find-ox r (- c 1) board) OorX) true
    (= (find-ox r c board) (find-ox r (- c 1) board) (find-ox r (- c 2) board) OorX) true
    (= (find-ox r c board) (find-ox (- r 1) (+ c 1) board) (find-ox (- r 2) (+ c 2) board) OorX) true
    (= (find-ox r c board) (find-ox (- r 1) (+ c 1) board) (find-ox (+ r 1) (- c 1) board) OorX) true
    (= (find-ox r c board) (find-ox (+ r 1) (- c 1) board) (find-ox (+ r 2) (- c 2) board) OorX) true
    :else false
    )
  
  
  )

(defn placeRC [r c OorX]
  (let [tempBoard (assoc-in @currentBoard [r c] OorX)]
       (reset! currentBoard tempBoard)
    )
)


(defn helper [i]
  (loop [j (dec @boardSize)]
      (if (or (< j 0) (number? (find-ox i j @currentBoard)))
              j
           (recur (dec j)) 
    )
  )
  )

(defn computerMove [] 
  (loop [i (dec @boardSize), result []]
    (if (and (< i 0) (> (count result) 0))
    (get  (vec result) 0)
   ; (vec result)
     (recur (dec i)(if (>= (helper i) 0)
                     (conj result (vector i (helper i)))
                 #_   (do 
                       (println i)
                       (println (helper i))
                      )))
  )
  ))

(defn computerMove2 [] 
  (loop [i (dec @boardSize), result []]
    (if (and (< i 0) (> (count result) 0))
  #_  (get  (vec result) 0)
    (vec result)
     (recur (dec i)(if (>= (helper i) 0)
                     (conj result (vector i (helper i)))
                 #_   (do 
                       (println i)
                       (println (helper i))
                      )))
  )
  ))

(defn computerTurn [computer]
  
    (do
      (placeRC (get computer 0) (get computer 1) "o")
    (cond 
      (win-detect (get computer 0) (get computer 1) @currentBoard "o") {:type "comwon"}
      :else (do 
             (swap! totalStep inc)
             (if (>= @totalStep (* @boardSize @boardSize)) 
               {:type "tie"}
               (do
               ;  (placeRC (get computer 0) (get computer 1) "o")
               {:type "commove" :r (get computer 0) :c (get computer 1)}
               ))           
            )  
      )
)
  
  )


(defroutes app2*
  
     (compojure.route/resources "/")
     (HEAD "/" [] "")
     (GET "/" [] (ring.util.response/resource-response "homepage.html"))
     
   (POST  "/" request 
          (let [newSize (read-string ((request :params) "size"))]
          (do
        ;   (prn request)
           (resetBoard newSize)
           (reset! boardSize newSize)
           (reset! totalStep 0)
           (response {:type ((request :params) "type")})
        )
          )
   )
    (POST  "/size" request 
            
              (let [newSize (read-string ((request :params) "size"))]
              
            (do
          ;    (prn  newSize)
              (reset! boardSize  newSize)
              (reset! currentBoard (resetBoard newSize))
            (response  {:type newSize} )        
         )
         )
            )
   (POST  "/move" request 
            (do
            (swap! totalStep inc)
            (cond 
              
              (win-detect (Integer. ((request :params) "r")) (Integer. ((request :params) "c")) (placeRC (Integer. ((request :params) "r")) (Integer. ((request :params) "c")) "x") "x") (response {:type "userwon"})
              (>= @totalStep (* @boardSize @boardSize)) (response {:type "tie"})
              
              :else (response (computerTurn (computerMove)))
              )
            
            )
    )
)
(def app2
  (-> (wrap-params app2*)
      (wrap-json-params )
      (wrap-json-response)))


