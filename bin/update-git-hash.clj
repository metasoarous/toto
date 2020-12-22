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

(def pom-reader (io/reader "pom.xml"))

(def pom-xml
  (xml/parse pom-reader))

(def pom-ns (namespace (:tag (second (:content pom-xml)))))

(defn nsify
  [key]
  (keyword pom-ns (name key)))

(def git-sha
  (-> ($ git rev-parse HEAD) :out slurp string/trim))

(Thread/sleep 1000)

(defn update-git-sha []
  (-> (zip/xml-zip pom-xml)
      (zx/xml1->
        (nsify :scm)
        (nsify :tag))
      (zip/edit
        (fn [node]
          (assoc node :content [git-sha])))
      (zip/root)
      (xml/emit-str)))

(def new-xml
  ;; This is fucking stupid, but is the only way I can get around this weird error where occasionally I get
  ;; com.sun.org.apache.xerces.internal.impl.msg.SAXMessages
  (loop [i 0]
    (when (< i 10)
      (try
        (update-git-sha)
        (catch Throwable t
          (recur (inc i)))))))

(spit "pom.xml" new-xml)
