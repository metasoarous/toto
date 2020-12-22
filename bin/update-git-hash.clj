#!/usr/bin/env bb

(require '[clojure.data.xml :as xml]
         '[clojure.string :as string]
         '[clojure.walk :as walk]
         '[clojure.pprint :as pp]
         '[babashka.process :as p :refer [process $]]
         '[babashka.classpath :as cp])

;; Add data.zip
(->> '{:deps {org.clojure/data.zip {:mvn/version "1.0.0"}}}
  str ($ clojure -Spath -Sdeps) :out slurp str/trim
  cp/add-classpath)

(require '[clojure.zip :as zip]
         '[clojure.data.zip.xml :as zx])

(def pom-xml
  (xml/parse (io/reader "pom.xml")))

(def pom-ns (namespace (:tag (second (:content pom-xml)))))

(defn nsify
  [key]
  (keyword pom-ns (name key)))

(def git-sha
  (-> ($ git rev-parse HEAD) :out slurp clojure.string/trim))

(-> (zip/xml-zip pom-xml)
    (zx/xml1->
      (nsify :scm)
      (nsify :tag))
    (zip/edit
      (fn [node]
        (assoc node :content [git-sha])))
    (zip/root)
    (xml/emit-str)
    (->> (spit "pom.xml")))

