(defproject CoursePrediction "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [cc.artifice/clj-ml "0.5.0-SNAPSHOT"]
                 [clj-ml "0.0.3-SNAPSHOT"]
                 [ring "1.2.1"] ;"1.0.0"?
                 [compojure "1.1.6"]
                 [javax.servlet/servlet-api "2.5"]  ;recommended in ring github
                 [org.clojure/java.jdbc "0.2.3"]
                 [org.xerial/sqlite-jdbc "3.7.2"]
                 [korma "0.3.0-RC5"]
                 [enlive/enlive "1.0.0"]
                 [lib-noir "0.8.1"]
                 [org.clojure/tools.trace "0.7.5"]
                 [org.clojure/data.json "0.2.4"]
                 [ring/ring-json "0.2.0"]
                 ]
    :plugins [[lein-ring "0.8.10"][lein-beanstalk "0.2.7"]]
  :ring {:handler CoursePrediction.core/app2}
  
  )
