(ns CoursePrediction.core)
(use 'clj-ml.classifiers 'clj-ml.data 'clj-ml.utils 'clj-ml.io)
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
(use 'clojure.java.io)
(require '[clojure.java.io :as io])
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



;(def titanicds (load-instances :arff "data/trainingData.arff"))
;(def titanicds (dataset-set-class titanicds :425))
;(dataset-class-index titanicds)
#_(def evaluation (classifier-evaluate (make-classifier :decision-tree :c45)
                                           :cross-validation titanicds 4))
;(println (:summary evaluation))
;(println (:confusion-matrix evaluation))
;evaluation
;;
;(def titanic-testds (load-instances :arff "data/testData.arff"))
;(def titanic-testds (dataset-set-class titanic-testds :425))
;(def classifier (classifier-train (make-classifier :decision-tree :c45) titanicds))
#_(def preds (for [instance (dataset-seq titanic-testds)]
                      (name (classifier-classify classifier instance))))

(defn makePrediction [input]
  (do
    (updateTestData input)
    (let [test (load-instances :arff "data/testData.arff")
          testClass (dataset-set-class test :425)
          training (load-instances :arff "data/trainingData.arff")
          trainingClass (dataset-set-class training :425)
          classifier (classifier-train (make-classifier :decision-tree :c45) trainingClass)
          preds (for [instance (dataset-seq testClass)]
                       (name (classifier-classify classifier instance)))
          ]
      preds
      )
    )
  )

;(makePrediction "B,B,B,B,B,B,B,B,B,B,B,B,B,C,?")


(defn updateTestData [input]
  (let [file-content-str (slurp  "data/prefix.txt")]
  (with-open [wrtr (writer "data/testData.arff")]
  (.write wrtr (str  file-content-str "\n\r" input)))
  )
  )


(defn updateClassifier [input]
  (let [inputString (str "\n\r" input)]
  (with-open [wrtr (writer "data/trainingData.arff" :append true)]
  (.write wrtr inputString))))

;(updateClassifier "B,B,B,B,B,B,B,B,B,B,B,B,B,B,C")


(defn getSummary [input]
  (do
    (let [ds (load-instances :arff "data/trainingData.arff")
         dsClass (dataset-set-class ds :425)
         evaluation (classifier-evaluate (make-classifier :decision-tree :c45)
                                          :cross-validation dsClass 4)]
      (:summary evaluation)
         )
    ))

(defn getMatrix [input]
  (do
  ;  (updateTestData input)
    (let [ds (load-instances :arff "data/trainingData.arff")
         dsClass (dataset-set-class ds :425)
         evaluation (classifier-evaluate (make-classifier :decision-tree :c45)
                                          :cross-validation dsClass 4)]
      (:confusion-matrix evaluation)
         )
    ))


;(println (getSummary "B,B,B,B,B,B,B,B,B,B,B,B,B,B,?"))




(defroutes app2*
  
     (compojure.route/resources "/")
     (HEAD "/" [] "")
     (GET "/" [] (ring.util.response/resource-response "index.html"))
     
   (POST  "/" request 
          (let [newSize (read-string ((request :params) "size"))]
          (do
           (response {:type ((request :params) "type")})
        )
          )
   )
    (POST  "/size" request 
            
              (let [newSize (read-string ((request :params) "size"))]
              
            (do
        
            (response  {:type newSize} )        
         )
         )
            )
)
(def app2
  (-> (wrap-params app2*)
      (wrap-json-params )
      (wrap-json-response)))











