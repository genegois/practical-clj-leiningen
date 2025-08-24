(defproject practical_clj "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [;; Backend library
                 [org.clojure/clojure "1.11.1"]
                 [ring/ring-core "1.11.0"]
                 [ring/ring-jetty-adapter "1.11.0"]
                 [metosin/reitit "0.9.1"]
                 [metosin/muuntaja "0.6.8"]
                 [metosin/ring-http-response "0.9.4"]
                 [cheshire/cheshire "5.11.0"] 
                 [buddy/buddy-core "1.12.0-430"]
                 [buddy/buddy-hashers "1.8.158"]
                 [buddy/buddy-sign "3.4.333"]
                 [clj-http/clj-http "3.13.1"]
                 [com.github.seancorfield/next.jdbc "1.3.1048"]
                 [org.postgresql/postgresql "42.7.7"]
                 [org.mongodb/mongodb-driver-sync "5.2.0"]
                 [org.clojure/core.cache "1.1.234"]
                 ;; Frontend library
                 [org.clojure/clojurescript "1.12.42"]
                 [reagent/reagent "1.3.0"]
                 [cljs-ajax/cljs-ajax "0.8.4"]
                 [org.clojure/tools.namespace "1.5.0"]]
  :source-paths ["src" "dev"]
  :main ^:skip-aot practical-clj.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
