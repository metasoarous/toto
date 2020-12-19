(ns user
  (:require [toto.server :as server]
            [toto.core :as toto]
            [cheshire.core :as json]
            [clojure.pprint :as pp]))

;; Let Clojure warn you when it needs to reflect on types, or when it does math
;; on unboxed numbers. In both cases you should add type annotations to prevent
;; degraded performance.
;(set! *warn-on-reflection* true)
;(set! *unchecked-math* :warn-on-boxed)


;; Here is some example usage you can play with at the repl
(comment

  ;; Start the plot server
  (toto/start-server! 10666)


  (toto/view! [:div [:h1 "yo dawg"]])

  ;; Construct a composite document using hiccup
  (def viz
    [:div
      [:h1 "Look ye and behold"]
      [:p "All this time, it was the little dog!"]])

  (toto/live-view! "resources/toto/examples/test.md")

  ;; Then edit the file at `resources/toto/examples/test.md` and watch

  ;; Can live reload code as well

  (toto/kill-watchers!)
  (toto/live-view! "resources/toto/examples/clj/clj_test.clj")

  :end-examples)




