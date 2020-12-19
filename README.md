
# Toto

### Simple but powerful static site generation in Clojure

[![Clojars Project](https://img.shields.io/clojars/v/metasoarous/toto.svg)](https://clojars.org/metasoarous/toto)
[![cljdoc badge](https://cljdoc.org/badge/metasoarous/toto)](https://cljdoc.org/d/metasoarous/toto)


![Toto](/toto.svg)

> The **real** man behind the curtain...


<br/>


## Overview

Toto is a static site generation toolkit build in and for (predominantly, for now) Clojure.


### Features

* **Live code reloading** experience (update browser preview as your edit)
* Simple but flexible API for differentiating output
* Ingest markdown or hiccup (as `.edn`, `.json` or even `.clj`)
* Embed hiccup in your markdown files using specially annotated code blocks
* Templating with simple hiccup functions
* Outputs static html files

To my knowledge, Toto is the only static site generation toolkit oriented towards Clojure which features live code reloading.
It's a fantastic choice for for putting together a personal website or blog.


### Relationship with Oz

Toto is an experimental/alpha fork of [Oz](https://github.com/metasoarous/oz) which strips out all of the data visualization functionality for a more stripped down static site generation experience.
Over time the scope of Oz grew and evolved into something generally useful outside of the data science workflows it was initially intended for.
However, it's been difficult to properly highlight this functionality in light of everything _else_ Oz provides.
Toto is being released separately from Oz in hopes of making this functionality more discoverable.

Eventually, my goal is to modularize Toto to the point that it can serve as the underpinnings for much of Oz's core functionality, simplifying that code base.
This will take some configuration work, and may still result in a bit of code duplication, but ultimately has the potential to make both more capable.

In any case, if you want static site generation 


## API

Oz itself provides:

* [`view!`](#repl-usage): Clojure REPL API for for pushing hiccup data to a browser window over a websocket
* [`load`](#loading-specs): load markdown or hiccup (as `edn`, `json`, or or `clj`) from disk
* [`export!`](#export): write out self-contained html files
* [`live-view!`](#live-code-reloading): Figwheel-inspired `live-view!` function for watching and `view!`ing `.md`, `.edn` and `.json` files with Vega-Lite & Vega (+ (or markdown hiccup))
* [`build!`](#static-site-generation): generate a static website from directories of markdown &/or hiccup, complete with live code reloading (as with `live-view!`)


## REPL Usage

If you clone this repository and open up the `dev/user.clj` file, you can follow along by executing the
commented out code block at the end of the file.

Assuming you're starting from scratch, first add oz to your leiningen project dependencies

[![Clojars Project](https://img.shields.io/clojars/v/metasoarous/oz.svg)](https://clojars.org/metasoarous/oz)

Next, require oz and start the plot server as follows:

``` clojure
(require '[oz.core :as oz])

(oz/start-server!)
```

This will fire up a browser window with a websocket connection for funneling view data back and forth.
If you forget to call this function, it will be called for you when you create your first plot, but be aware
that it will delay the first display, and it's possible you'll have to resend the plot on a slower
computer.

Next we'll define a function for generating some dummy data

```clojure
(defn play-data [& names]
  (for [n names
        i (range 20)]
    {:time i :item n :quantity (+ (Math/pow (* i (count n)) 0.8) (rand-int (count n)))}))
```


### Hiccup

We can also embed Vega-Lite & Vega visualizations within hiccup documents:

```clojure
(def viz
  [:div
    [:h1 "Look ye and behold"]
    [:p "A couple of small charts"]
    [:div {:style {:display "flex" :flex-direction "row"}}
      [:vega-lite line-plot]
      [:vega-lite stacked-bar]]
    [:p "A wider, more expansive chart"]
    [:vega contour-plot]
    [:h2 "If ever, oh ever a viz there was, the vizard of oz is one because, because, because..."]
    [:p "Because of the wonderful things it does"]])

(oz/view! viz)
```

Note that the Vega-Lite & Vega specs are described in the output vega as using the `:vega` and `:vega-lite` keys.

You should now see something like this:

![composite view](doc/composite-view.png)

Note that vega/vega-lite already have very powerful and impressive [plot concatenation](https://vega.github.io/vega-lite/docs/concat.html) features which allow for coupling of interactivity between plots in a viz.
However, combing things through hiccup like this is nice for expedience, gives one the ability to combine such visualizations in the context of HTML documents.

Also note that while not illustrated above, you can specify multiple maps in these vectors, and they will be merged into one.
So for example, you can do `[:vega-lite stacked-bar {:width 100}]` to override the width.


## Loading specs

Oz now features a `load` function which accepts the following formats:

* `edn`, `json`, `yaml`: directly parse into hiccup &/or Vega/Vega-Lite representations
* `md`: loads a markdown file, with a notation for specifying Vega/Vega-Lite in code blocks tagged with the `vega`, `vega-lite` or `oz` class

As example of the markdown syntax:

    # An example markdown file

    ```edn vega-lite
    {:data {:url "data/cars.json"}
     :mark "point"
     :encoding {
       :x {:field "Horsepower", :type "quantitative"}
       :y {:field "Miles_per_Gallon", :type "quantitative"}
       :color {:field "Origin", :type "nominal"}}}
    ```

The real magic here is in the code class specification `edn vega-lite`.
It's possible to replace `edn` with `json` or `yaml`, and `vega` with `vega-lite` as appropriate.
Additionally, these classes can be hyphenated for compatibility with editors/parsers that have problems with multiple class specifications (e.g. `edn-vega-lite`)

Note that embedding all of your data into a vega/vega-lite spec directly as `:values` may be untenable for larger data sets.
In these cases, the recommended solution is to post your data to a GitHub gist, or elsewhere online where you can refer to it using the `:url` syntax (e.g. `{:data {:url "https://your.data.url/path"} ...}`).

One final note: in lieue of `vega` or `vega-lite` you can specify `hiccup` in order to embed oz-style hiccup forms which may or may not contain `[:vega ...]` or `[:vega-lite ...]` blocks.
This allows you to embed nontrivial html in your markdown files as hiccup, when basic markdown just doesn't cut it, without having to resort to manually writing html.


## Export

We can also export static HTML files which use `Vega-Embed` to render interactive Vega/Vega-Lite visualizations using the `oz/export!` function.

```clojure
(oz/export! spec "test.html")
```


## Live code reloading

Oz now features Figwheel-like hot code reloading for Clojure-based data science workflows.
To start this functionality, you specify from the REPL a file you would like to watch for changes, like so:

```clojure
(oz/live-reload! "live-reload-test.clj")
```

As soon as you run this, the code in the file will be executed in its entirety.
Thereafter, if you save changes to the file, all forms starting from the first form with material changes will be re-evaluated.
Additionally, whitespace changes are ignored, and namespace changes only trigger a recompile if there were other code changes in flight, or if there was an error during the last execution.
We also try to do a good job of logging notifications as things are running so that you know what is running and how long things are taking for to execute long-running forms.

Collectively all of these features give you the same magic of Figwheel's hot-code reloading experience, but geared towards the specific demands of a data scientist, or really anyone who needs to quickly hack together potentially long running jobs.

Here's a quick video of this in action: <https://www.youtube.com/watch?v=yUTxm29fjT4>

Of import: Because the code evaluated with `live-reload!` is evaluated in a separate thread, you can't include any code which might try to set root bindings of a dynamic var.
Fortunately, setting root var bindings isn't something I've ever needed to do in my data science workflow (nor should you), but of course, it's possible there are libraries out there that do this.
Just be aware that it might come up.
This seems to be a pretty fundamental Clojure limitation, but I'd be interested to hear from the oracles whether there's any chance of this being supported in a future version of Clojure.

There's also a related function, `oz/live-view!` which will similarly watch a file for changes, `oz/load!` it, then `oz/view!` it.



## Static site generation

If you've ever thought "man, I wish there was a static site generation toolkit which had live code reloading of whatever page you're currently editing, and it would be great if it was in Clojure and let me embed data visualizations and math formulas via LaTeX in Markdown & Hiccup documents", boy, are you in for a treat!

Oz now features exectly such features in the form of the `oz/build!`.
A very simple site might be generated with:

```clojure
(build!
  [{:from "examples/static-site/src/"
    :to "examples/static-site/build/"}])
```

The input formats currently supported by `oz/build!` are

* `md`: As described above, markdown with embedded Vega-Lite or Vega visualizations, Latex, and hiccup
* `json`, `edn`: You can directly supply hiccup data for more control over layout and content
* `clj`: Will `live-reload!` Clojure files (as described above), and render the last form evaluated as hiccup

Oz should handle image and css files it comes across by simply copying them over.
However, if you have any `json` or `edn` assets (datasets perhaps) which need to pass through unchanged, you can separate these into their own build specification, like so:

```clojure
(defn site-template
  [spec]
  [:div {:style {:max-width 900 :margin-left "auto" :margin-right "auto"}}
   spec])

(build!
  [{:from "examples/static-site/src/site/"
    :to "examples/static-site/build/"
    :template-fn site-template}
   ;; If you have static assets, like datasets or imagines which need to be simply copied over
   {:from "examples/static-site/src/assets/"
    :to "examples/static-site/build/"
    :as-assets? true}])
```

This can be a good way to separate document code from other static assets.

Specifying multiple builds like this can be used to do other things as well.
For example, if you wanted to render a particular set of pages using a different template function (for example, so that your blog posts style differently than the main pages), you can do that easily

```clojure
(defn blog-template
  [spec]
  (site-template
    (let [{:as spec-meta :keys [title published-at tags]} (meta spec)]
      [:div
       [:h1 {:style {:line-height 1.35}} title]
       [:p "Published on: " published-at]
       [:p "Tags: " (string/join ", " tags)]
       spec])))

(build!
  [{:from "examples/static-site/src/site/"
    :to "examples/static-site/build/"
    :template-fn site-template}
   {:from "examples/static-site/src/blog/"
    :to "examples/static-site/build/blog/"
    :template-fn blog-template}
   ;; If you have static assets, like datasets or imagines which need to be simply copied over
   {:from "examples/static-site/src/assets/"
    :to "examples/static-site/build/"
    :as-assets? true}])
```

Note that the `blog-template` above is using metadata about the spec to inform how it renders.
This metadata can be written into Markdown files using a yaml markdown metadata header (see `/examples/static-site/src/`)

```
---
title: Oz static websites rock
tags: oz, dataviz
---

# Oz static websites!

Some markdown content...
```

The title in particular here will wind it's way into the `Title` metadata tag of your output HTML document, and thus will be visible at the top of your browser window when you view the file.
This is a pattern that Jekyll and some other blogging engines use, and `markdown-clj` now supports extracting this data.

Again, as you edit and save these files, the outputs just automatically update for you, both as compiled HTML files, and in the live-view window which lets you see your changes as you make em.
If you need to change a template, or some other detail of the specs, you can simply rerun `build!` with the modified arguments, and the most recently edited page will updated before your eyes.
This provides for a lovely live-view editing experience from the comfort of your favorite editor.

When you're done, one of the easiest ways to deploy is with the excellent `surge.sh` toolkit, which makes static site deployment a breeze.
You can also use GitHub Pages or S3 or really whatever if you prefer.
The great thing about static sites is that they are easy and cheap to deploy and scale, so you have plenty of options at your disposal.


## Local CLJS development

Oz is now compiled (on the cljs side) with [Shadow-CLJS](http://shadow-cljs.org/), together with the Clojure CLI tooling.
A typical workflow involves running `clj -M:shadow-cljs watch devcards app` (note, older versions of `clj` use `-A` instead of `-M`; consider updating).
This will watch your cljs files for changes, and immediately compile both the `app.js` and `devcards.js` targets (to `resources/oz/public/js/`).

In general, the best way to develop is to visit <https://localhost:7125/devcards.html>, which will pull up a live view of a set of example Reagent components defined at `src/cljs/oz/core_devcards.cljs`.
This is the easiest way to tweak functionality and test new features, as editing `src/cljs/oz/core.cljs` will trigger updates to the devcards views.

If it's necessary or desirable to test the app (live-view, etc) functionality  "in-situ", you can also use the normal Clj REPL utilities to feed plots to the `app.js` target using `oz/view!`, etc.
Note that if you do this, you will need to use whatever port is passed to `oz/view!` (by default, 10666) and not the one printed out when you start `clj -M:shadow-cljs`.

See documentation for your specific editing environment if you'd like your editor to be able to connect to the Shadow-CLJS repl.
For `vim-fireplace`, the initial Clj connection should establish itself automatically when you attempt to evaluate your first form.
From there simply execute the vim command `:CljEval (shadow/repl :app)`, and you should be able to evaluate code in the `*.cljs` files from vim.
Code in `*.clj` files should also continue to evaluate as before as well.

IMPORTANT NOTE: If you end up deploying a version of Oz to Clojars or elsewhere, make sure you stop your `clj -M:shadow-cljs watch` process before running `make release`.
If you don't, shadow will continue watching files and rebuild js compilation targets with dev time configuration (shadow, less minification, etc), that shouldn't be in the final release build.
If however you are simply making changes and pushing up for me to release, please just leave any compiled changes to the js targets out of your commits.



## License

Copyright © 2020 Christopher Small

Forked from Vizard (with thanks) - Copyright © 2017 Yieldbot, Inc.

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.

