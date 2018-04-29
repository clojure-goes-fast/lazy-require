(task-options!
 pom  {:project     'com.clojure-goes-fast/lazy-require
       :version     "0.1.1"
       :description "Clojure library for deferred namespace loading."
       :url         "https://github.com/clojure-goes-fast/lazy-require"
       :scm         {:url "https://github.com/clojure-goes-fast/lazy-require"}
       :license     {"Eclipse Public License"
                     "http://www.eclipse.org/legal/epl-v10.html"}}
 push {:repo "clojars"})

(set-env! :dependencies '[[org.clojure/clojure "1.9.0" :scope "provided"]]
          :source-paths #{"src/"}
          :test-paths #{"test/"})

(ns-unmap 'boot.user 'test)
(deftask test []
  (set-env! :source-paths #(into % (get-env :test-paths))
            :dependencies #(conj % '[adzerk/boot-test "1.2.0"]))
  (require 'adzerk.boot-test)
  ((resolve 'adzerk.boot-test/test)))

(deftask build []
  (set-env! :resource-paths (get-env :source-paths))
  (comp (pom) (jar)))
