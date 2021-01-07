
# Toto

### Simple but powerful static site generation in Clojure

[![Clojars Project](https://img.shields.io/clojars/v/metasoarous/toto.svg)](https://clojars.org/metasoarous/toto)
[![cljdoc badge](https://cljdoc.org/badge/metasoarous/toto)](https://cljdoc.org/d/metasoarous/toto)


![Toto](/toto.svg)

> The *real* man behind the curtain...


<br/>


## Overview

Toto is a static site generation toolkit build in and (predominantly) for Clojure.


### Features

* **Live code reloading** experience (update browser preview as your edit)!
* Exceptionally simple yet flexible API
* Ingest markdown or hiccup (as `.edn`, `.json` or even `.clj`)
* Embed hiccup in your markdown files using specially annotated code blocks
* Templating with simple hiccup functions
* Outputs static html files

To my knowledge, Toto is the only static site generation toolkit oriented towards Clojure which features live code reloading.


### Relationship with Oz

Toto is an experimental/alpha fork of [Oz](https://github.com/metasoarous/oz) which strips out all of the data visualization functionality for a more stripped down static site generation experience.
Over time the scope of Oz grew and evolved into something generally useful outside of the data science workflows it was initially intended for.
However, it's been difficult to properly highlight this functionality in light of everything _else_ Oz provides.
Toto is being released separately from Oz in hopes of making this functionality more discoverable.

Eventually, my goal is to modularize Toto to the point that it can serve as the underpinnings for much of Oz's core functionality, simplifying that code base.
This will take some configuration work, and may still result in a bit of code duplication, but ultimately has the potential to make both more capable.

In any case, if all you need is a basic static html site (sans data science goodies), you've come to the right place.


### API

Toto itself provides:

* [`build!`](#basic-usage) (main entry point): generate a static website from directories of markdown &/or hiccup, complete with live code reloading (as with `live-view!`)
* [`view!`](#repl-usage): Clojure REPL API for for pushing hiccup data to a browser window over a websocket
* [`load`](#loading-specs): load markdown or hiccup (as `edn`, `json`, or or `clj`) from disk
* [`export!`](#export): write out documents as html
* [`live-view!`](#live-code-reloading): watch individual files for changes and update a live `view!` of them (without compiling to disk as html like `build!`)


## Basic Usage

Say you have a few directories of markdown that you'd like to turn into a static site.

```
site-src
├── index.md
├── about.md
└── blog
    ├── first-post.md
    ├── why-clojure-is-awesome.md
    ├── sourdough-recipe.md
    └── apologies-for-not-posting-more.md
```

With `toto`, this is as simple as:

```clojure
(require '[toto.core :as toto])

(toto/build!
  {:from "site-src/"
   :to "build/"})
```

This command immediately compiles the markdown files in `site-src` to html files in the `build` directory, like so:


```
build
├── index.html
├── about.html
└── blog
    ├── first-post.html
    ├── why-clojure-is-awesome.html
    ├── sourdough-recipe.html
    └── apologies-for-not-posting-more.html
```

The `build!` command will also (on Mac and Linux) open a web page in your browser.
As you edit and save changes to the input markdown files, said web page will display the most recently edited file.
This provides a really wonderful editing experience al. a Gatsby or Figwheel (only better, IMHO, as these tools don't automatically switch context to the most recently edited file, helpful when working on multiple files concurrently).


### Template functions

Once you've gotten going, you may wish to customize layout of your site a bit.
Perhaps you would like to restrict the width of your main content.

This can be accomplished with the `:template-fn` entry in your build spec:

```clojure
(defn site-template [doc]
  [:div {:max-width 800}
   doc]

(toto/build!
  {:from "site-src/"
   :to "build/"
   :template-fn site-template})
```


### Markdown metadata

Toto automatically parses out [markdown metadata](https://jekyllrb.com/docs/front-matter/) as yaml from the top of your documents.
This is in following with other popular static site generation frameworks such as Jekyll, and now supported by markdown-clj.

As with Jekyll, Toto will treat some of this data specially.
For example, if you set a `title` on your doc, as above, Toto will use this to set the `Title` metadata tag of your output HTML document, making it visible at the top of your browser window when loaded as HTML.
(Toto also handles `keywords`, `author`, and `description` similarly).

```
---
title: 'My awesome blog'
---

# Welcome to my site.

Thanks for visiting.
```

Toto goes a step further by attaching this document metadata as proper Clojure metadata on the loaded document.
This means that you can use this data in the [template functions](#template-functions) described above (see [below](#multiple-build-specs) for examples).


### Multiple build specs

As time goes on, you might decide that certain parts of your site should get rendered differently than the rest.
For example, you might wish to specifically change how your blog posts are displayed.
Perhaps you'd like to automatically render the title and publish date from your markdown metadata?

Given markdown like this::

```
---
title: 'Why Clojure is so Awesome'
published-at: '2020-12-20 11:24'
tags: [clojure, data]
---

Three simple (TM) words: **ITS JUST DATA**
```

We can write a simple template function like so:

```clojure
(defn blog-template
  [doc]
  ;; Here's where we get our metadata
  (let [{:as doc-meta :keys [title published-at tags]} (meta doc)]
    [site-template
      [:div
        [:h1 title]
        [:p "Published at: " published-at]
        [:p "Tags: " (string/join ", " tags)]
        [:br]
        doc]]))
```

The easiest way to apply this template to just the blog posts is to separate them out from the rest of the content so that we can easily apply one template to one set of files, and another to the rest.

Imagine we reorganize the files like so:

```
site-src
├── pages
│   ├── index.md
│   └── about.md
└── blog-posts
    ├── first-post.md
    ├── why-clojure-is-awesome.md
    ├── sourdough-recipe.md
    └── apologies-for-not-posting-more.md
```

Then we can easily modify our build spec to take a vector of build specifications, like so:

```clojure
(toto/build!
  [{:from "site-src/pages"
    :to "build/"
    :template-fn site-template}
   {:from "site-src/blog-posts"
    :to "build/blog"
    :template-fn blog-template}])
```

Note however that even though we had to separate these files, they will still have the same relationship to each other in the build directory as before, due to our `:to` specifications.

This ability to specify multiple build specs gives us very granular control over how we process different parts of our site.

### Assets

If you ever need to pass through static assets uncompiled you can use the `:as-assets?` option on a build spec, and add that to the rest.

```clojure
(toto/build!
  [...
   ;; If you have static assets, like datasets or imagines which need to be simply copied over
   {:from "site-src/assets/"
    :to "build/"
    :as-assets? true}])
```

In general, Toto will pass through files it doesn't know how to handle, but this directive can be helpful for (e.g.) json files which might otherwise be interpreted as hiccup.

### More...

Other options passed through to the build specs will be passed through to the `compile` function.
In particular, you can specify options like `:omit-styles?` to leave out the default styles, or `:header-extras` to add your own content to the head of the resulting HTML documents.



## More API functions

### Loading specs

While it's generally most convenient to use the `build!` function, you can also load individual files using `toto/load`.


### Export

We can also export static HTML files which use `Vega-Embed` to render interactive Vega/Vega-Lite visualizations using the `toto/export!` function.

```clojure
(toto/export! doc "test.html")
```

### Live view

You can run live-view on a single file with

```clojure
(toto/live-view! "test.md")
```

## Deploy

When you're done, one of the easiest ways to deploy is with the excellent `surge.sh` toolkit, which makes static site deployment a breeze.
You can also use GitHub Pages or S3 or really whatever if you prefer.
The great thing about static sites is that they are easy and cheap to deploy and scale, so you have plenty of options at your disposal.


## Thanks for checking out Toto!

I'm looking forward to hearing what you think.


<br/>

## Local CLJS development

Toto is now compiled (on the cljs side) with [Shadow-CLJS](http://shadow-cljs.org/), together with the Clojure CLI tooling.
A typical workflow involves running `clj -M:shadow-cljs watch devcards app` (note, older versions of `clj` use `-A` instead of `-M`; consider updating).
This will watch your cljs files for changes, and immediately compile both the `app.js` and `devcards.js` targets (to `resources/toto/public/js/`).

In general, the best way to develop is to visit <https://localhost:7125/devcards.html>, which will pull up a live view of a set of example Reagent components defined at `src/cljs/toto/core_devcards.cljs`.
This is the easiest way to tweak functionality and test new features, as editing `src/cljs/toto/core.cljs` will trigger updates to the devcards views.

If it's necessary or desirable to test the app (live-view, etc) functionality  "in-situ", you can also use the normal Clj REPL utilities to feed plots to the `app.js` target using `toto/view!`, etc.
Note that if you do this, you will need to use whatever port is passed to `toto/view!` (by default, 10666) and not the one printed out when you start `clj -M:shadow-cljs`.

See documentation for your specific editing environment if you'd like your editor to be able to connect to the Shadow-CLJS repl.
For `vim-fireplace`, the initial Clj connection should establish itself automatically when you attempt to evaluate your first form.
From there simply execute the vim command `:CljEval (shadow/repl :app)`, and you should be able to evaluate code in the `*.cljs` files from vim.
Code in `*.clj` files should also continue to evaluate as before as well.

IMPORTANT NOTE: If you end up deploying a version of Toto to Clojars or elsewhere, make sure you stop your `clj -M:shadow-cljs watch` process before running `make release`.
If you don't, shadow will continue watching files and rebuild js compilation targets with dev time configuration (shadow, less minification, etc), that shouldn't be in the final release build.
If however you are simply making changes and pushing up for me to release, please just leave any compiled changes to the js targets out of your commits.



## License

Copyright © 2020 Christopher Small

Forked from Vizard (with thanks) - Copyright © 2017 Yieldbot, Inc.

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.

