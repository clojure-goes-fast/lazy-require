(ns lazy-require.core)

;; Private API

(def ^:private lock (Object.))

(def ^:private deferred-nses (atom #{}))

(defn- postwalk
  "Copied from `clojure.walk/walk` to avoid loading the functions we don't need,
  since the whole premise of this lib is to shave loading time."
  [f form]
  (letfn [(walk [form]
            (f
             (cond
               (list? form) (apply list (map walk form))
               (instance? clojure.lang.IMapEntry form) (into [] (map walk) form)
               (seq? form) (doall (map walk form))
               (instance? clojure.lang.IRecord form) (into form (map walk) form)
               (coll? form) (into (empty form) (map walk) form)
               :else form)))]
    (walk form)))

(defn- synchronized-require [require-forms]
  (locking lock
    (apply require require-forms)))

;; Public API

(defmacro with-lazy-require
  "Load namespaces listed in `require-forms` and execute `body` in the context
  where those namespaces are already required. `require-forms` is a vector of
  require declarations, each can be either a symbol or a vector of the form
  `[name.space :as alias]`. Prefixed and nested require declarations, and
  `:refer` keyword are not supported.

  Example:

  (with-lazy-require [clojure.string
                      [clojure.set :as set]]
     (set/union #{1 2} #{3 (clojure.string/join [\"a\" \"b\"])}))"
  [require-forms & body]
  (let [nses (map #(if (symbol? %) % (first %)) require-forms)
        aliases (keep #(when (vector? %) (nth % 2)) require-forms)
        ns-set (->> (concat nses aliases)
                    (map str)
                    set)]
    (swap! deferred-nses into nses)
    `(do (#'synchronized-require (quote ~require-forms))
         ~@(postwalk (fn [el]
                       (if (and (symbol? el) (ns-set (namespace el)))
                         `(resolve (quote ~el))
                         el))
                     body))))

(defn load-in-background
  "Start loading all namespaces that were declared in `with-lazy-require`
  asynchronously in a background thread."
  []
  (doto (Thread. #(synchronized-require @deferred-nses))
    (.setDaemon true)
    .start))
