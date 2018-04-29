# lazy-require

lazy-require is a minimalistic library that allows you to postpone loading
namespaces until the code which needs them runs for the first time. This allows
to reduce the perceived startup time of the application.

## Why?

By design, Clojure is eager about loading namespaces. It certainly doesn't load
every namespaces on the classpath — that would be wasteful. Instead, Clojure
walks the dependency tree of namespaces as it loads them, starting with the app
entrypoint (the namespace with `-main`) and eagerly loads all namespaces it
reaches via `require` links.

Sometimes, however, your application might have parts that would never run, and
those parts depend on namespaces that never had to be loaded. This could happen,
for example, if the program can be started with different configuration that
will determine which code is run. Imagine an encryption program that can either
encrypt or decrypt data based on a launch flag, and those two functions require
disparate sets of namespaces to be loaded.

Another case is when some functionality is not needed immediately when the
program starts, but may become necessary after some external event — user input
or a network call. In that case, the namespaces still have to be loaded
eventually, but postponing their loading may reduce the initial time the user
has to wait.

## How does it work?

First, you'll need to add `com.clojure-goes-fast/lazy-require` to dependencies:

[![](https://clojars.org/com.clojure-goes-fast/lazy-require/latest-version.svg)](https://clojars.org/com.clojure-goes-fast/lazy-require)

The main macro to use is called `with-lazy-require`:

```clojure
(require '[lazy-require.core :as lreq])

(defn some-function [args]
  ...
  (lreq/with-lazy-require [clojure.string
                           [clojure.set :as set]]
    (set/union #{1 2} #{3 (clojure.string/join ["a" "b"])}))
  ...)
```

First argument to `with-lazy-require` is a vector of `require` forms. They can
be either symbols or vectors like `[name.space :as alias]` — these are the only
two variants that lazy-require supports. The rest of the arguments is the body
that will be executed once the declared namespaces are loaded. Notice how you
can use both full namespace names and aliases in the body.

Of course, using `with-lazy-require` means you can remove those namespaces from
`:require` in the `ns` definition — otherwise Clojure will load them immediately
and the point of lazy-require is lost.

If you want to postpone loading some namespaces, but don't want the user to
experience the delay later when the loading actually happens, you can call
`(load-in-background)` at some point during program initialization. This will
start loading all of the namespaces postponed by `with-lazy-require` in a
background thread. Chances are, by the time the user will need the functionality
provided by a postponed namespace, it will already be loaded.

## Warnings and gotchas

Using this library slightly circumvents the regular Clojure loading process.
For example, when doing AOT, Clojure will not infer that it has to compile the
postponed namespaces — you'll have to mention them explicitly.

Another problem might happen if you mix lazy-require with some other code that
does dynamic namespace loading. lazy-require itself makes sure to serialize the
loading of namespaces so that nothing breaks because of concurrent loading
(e.g., see
[clojure-emacs/cider#2092](https://github.com/clojure-emacs/cider/issues/2092)).
However, if other code also loads namespaces in the background, something might
happen. If you ever experience such issues, please file a ticket.

## License

lazy-require is distributed under the Eclipse Public License. See
[LICENSE](LICENSE).

Copyright 2018 Alexander Yakushev
