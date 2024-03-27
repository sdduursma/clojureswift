# ClojureSwift

A prototype of a compiler from Clojure to Swift.

## Why ClojureSwift?
I enjoy programming in Clojure. Its value orientation, dynamism, and simplicity enable a level of power and flexibility that many other programming languages, including Swift, cannot provide. This got me thinking, 'Can I use Clojure to program iOS apps?.' There is ClojureScript, which, in combination with React Native, can be used to build iOS apps.[^1] However, with React Native, you still don't have direct access to the platform's native APIs, and, as a result, you still need to use Swift sometimes. So, I decided to see if I could compile Clojure to Swift. It would also be a great way to learn more about Clojure and compilers, and to have some fun :).

## How does ClojureSwift work?

At a high level, the Clojure compiler can be broken down into the following components:

```
reader -> analyzer -> emitter
```

* The reader reads Clojure text and transforms it into Clojure data structures.
* The analyzer analyzes the Clojure data and enriches it, outputting an AST.
* The emitter walks the AST and emits the target code. In the case of Clojure, the emitter emits JVM bytecode; in the case of ClojureScript, JavaScript code; and in the case of ClojureSwift, Swift code.

Fortunately, the Clojure compiler is very modular. ClojureSwift reuses the reader and [tools.analyzer](https://github.com/clojure/tools.analyzer), which is a host-agnostic analyzer for Clojure code. On top of it, it provides [tools.analyzer.swift](https://github.com/sdduursma/clojureswift/tree/master/src/main/clojure/clojureswift/tools/analyzer) for Swift-specific analysis, and a Swift-specific emitter.

Ultimately, the idea is that the emitter would automatically feed into the Swift compiler, which would then compile the Swift code down to native code.

## Status

As said above, this is a prototype. It's nowhere near production-ready; however, it demonstrates the ability to compile certain Clojure forms for Swift. For instance:

```clojure
(def id #uuid "1369709c-2bdc-4e35-9ae1-1cde9068f672")
```

is compiled to:

```swift
var id = Foundation.UUID(uuidString: "1369709c-2bdc-4e35-9ae1-1cde9068f672")!
```

## Development

To run the tests:
```bash
clj -X:test:test/run
```

[^1]: At the time of this writing, there is also ClojureDart, but this didn't exist when I decided to prototype ClojureSwift.
