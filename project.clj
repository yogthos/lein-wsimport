(defproject com.madeye.clojure/lein-wsimport "1.1.2"
  :description "JAX-WS import plugin for Clojure projects"
  :url "https://github.com/klauern/lein-wsimport"
  :dependencies [
                 [com.taoensso/timbre "4.1.4"]
                 [com.sun.xml.ws/jaxws-tools "2.2.8"]
                ]
  :profiles { :dev { :dependencies [[midje "1.6-beta1"]]}}
  :plugins [[lein-midje "3.1.2"]
            [lein-clojars "0.9.1"]]
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :eval-in-leiningen true)
