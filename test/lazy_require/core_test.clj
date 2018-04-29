(ns lazy-require.core-test
  (:require [lazy-require.core :as sut]
            [clojure.test :refer :all]))

(deftest with-lazy-require-test
  (is (= #{"ab" 1 2 3}
         (sut/with-lazy-require [clojure.string
                                 [clojure.set :as set]]
           (set/union #{1 2} #{3 (clojure.string/join ["a" "b"])})))))

(deftest load-in-background-test
  ;; clojure.core.reducers takes noticable amount of time to load. If we load it
  ;; in background, then the later require should be instant.

  ;; First, ensure that it really takes awhile to load.
  (let [start (System/currentTimeMillis)]
    (require 'clojure.core.reducers)
    (is (> (- (System/currentTimeMillis) start) 50)
        "Loading c.c.reducers should take some time."))

  ;; Flush *loaded-libs* so that c.c.reducers is not "loaded" now
  (dosync (ref-set @#'clojure.core/*loaded-libs* (sorted-set)))

  (let [start (System/currentTimeMillis)]
    ;; Start background loading. The lazy require is done below, but since it
    ;; has side effets in macroexpansion time, we already know about it here.
    (sut/load-in-background)
    (is (< (- (System/currentTimeMillis) start) 5)
        "Loading is asynchronous - no time should have passed"))

  (Thread/sleep 1000) ;; To let the background loading complete

  (let [start (System/currentTimeMillis)]
    ;; Now, require the namespace and use it.
    (sut/with-lazy-require [[clojure.core.reducers :as r]]
      (is (= 10 (r/reduce + [1 2 3 4]))))
    (is (< (- (System/currentTimeMillis) start) 5)
        "c.c.reducers should be already loaded by now - no delay here")))
