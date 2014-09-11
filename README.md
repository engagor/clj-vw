# clj-vw

A Clojure client and wrapper for [vowpal
wabbit](https://github.com/JohnLangford/vowpal_wabbit/wiki), a fast out-of-core learning system
sponsored by [Microsoft Research](http://research.microsoft.com/en-us/) and (previously) [Yahoo!
Research](http://research.yahoo.com/node/1914).

## Artifacts

clj-vw artifacts are released to Clojars.

If you are using Maven, add the following repository definition to your pom.xml:

```
<repository>
  <id>clojars.org</id>
  <url>http://clojars.org/repo</url>
</repository>
```

## The Most Recent Release

With Leiningen:

```
[engagor/clj-vw "1.0.0-RC1"]
```

With Maven:

```
<dependency>
  <groupId>engagor</groupId>
  <artifactId>clj-vw</artifactId>
  <version>1.0.0-RC1</version>
</dependency>
```

## Usage and documentation

Except when only using client related code (see [online.clj](doc/clj-vw/clj-vw.online.html)), this
library requires that vowpal wabbit is installed in the usual way.  Basic knowledge of vowpal
wabbit, its [command line
options](https://github.com/JohnLangford/vowpal_wabbit/wiki/Command-line-arguments) and [input
format](https://github.com/JohnLangford/vowpal_wabbit/wiki/Input-format) are recommended. See the
[vowpal wabbit tutorial](https://github.com/JohnLangford/vowpal_wabbit/wiki/Tutorial) for more
information.

Codox documentation is available under [doc](doc/index.html) (or can be generated by issuing the
command `lein doc` in the repository's root directory). There are three namespaces.

* [core.clj](doc/clj-vw/clj-vw.core.html)
  Core functionality for interacting with vowpal wabbit in a generic way (input example formatting,
  writing data files, passing options and calling vw, ...)

* [offline.clj](doc/clj-vw/clj-vw.offline.html)
  Higher level helper functions for interfacing to a local vowpal wabbit installation.

* [online.clj](doc/clj-vw/clj-vw.online.html)
  Higher level helper functions for launching and connecting to a (local or remote) vowpal wabbit
  running in daemon mode.

Besides the codox documentation, example use cases are provided in the tests.

## Testing

On the command line, issue `lein test`. Requires that vowpal wabbit is installed and that the
directory `"/tmp/"` is read/writable.

## License

Copyright © 2014 Engagor

Distributed under the BSD Clause-2 License as distributed in the file [LICENSE.md](LICENSE.md) at
the root of this repository.
