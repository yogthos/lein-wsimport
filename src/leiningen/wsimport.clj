(ns leiningen.wsimport
  "Generate Java code from SOAP .wsdls using the JDK's wsimport task"
  (:require [clojure.java.io :as io]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.core :as appenders])
  (:import (com.sun.tools.ws WsImport)))

(System/setProperty "javax.xml.accessExternalSchema" "all")

(timbre/refer-timbre)
(timbre/merge-config! {:appenders {:spit (appenders/spit-appender {:fname "lein-wsimport.log"})}})
(timbre/merge-config! {:appenders {:println { :enabled? false } }})
(timbre/set-level! :debug)

(def opts {:compile-java-sources false
           :java-output-directory "target/generated/java"
           :keep-java-sources true
           :quiet-output false})

;; This is the meat of the plugin.  `WsImport#doMain` only takes two sets of parameters:
;;
;; - a String for the WSDL URI to import
;; - an array of String arguments to pass, configuring the generation strategy.
;;
;; If you have a JDK installed and in your path, type `wsimport` and read the output
;; to find out what all of the options available are.  Many are handled through
;; various map options in `:wsimport` on your `project.clj`, but any that aren't
;; can be passed in through `:extra-options`.

(defn compose-options-array
  "Create an array of options to pass in to WsImport#doMain out of
   a map of settings (usually gathered from the project settings)"
  [wsdl-file wsimport-opts]
  (let [ws-ary   (transient [])    ;; this feels wrong
        all-opts (conj opts wsimport-opts)]
    (debug "all-opts:" all-opts)
    (if-not (:compile-java-sources all-opts)
      (conj! ws-ary "-Xnocompile"))
    (if-let [out-dir (:java-output-directory all-opts)]
      (doseq [val ["-s" out-dir]]
        (conj! ws-ary val)))
    (if-let [out-dir (:class-output-directory all-opts)]
      (do
        (info "output directory:" out-dir)
        (conj! ws-ary "-d")
        (conj! ws-ary out-dir)
      )
      (info "Output directory not specified"))
    (if (:keep-java-sources all-opts)
      (conj! ws-ary "-keep"))
    (if-let [pkg (:java-package-name all-opts)]
      (do
        (conj! ws-ary "-p")
        (conj! ws-ary pkg)))
    (if (:quiet-output all-opts)
      (conj! ws-ary "-quiet"))
    (if-let [xtra-opts (:extra-options all-opts)]
      (reduce conj! ws-ary xtra-opts))
    (if-let [jaxb-binding-files (:jaxb-binding-files all-opts)]
      (doseq [jbinding jaxb-binding-files]  ;; seems kind of ugly, not sure this is a great solution
        (conj! ws-ary "-b")
        (conj! ws-ary jbinding)))
    (conj! ws-ary wsdl-file)
    (persistent! ws-ary)))  ;; this seems like a confession washing away sins...

(defn import-wsdls
  "Call WsImport#doMain from Sun's JDK using an array of WSDL's to import
   and a set of user and default-specified options"
  [wsdl-list wsdl-options]
  (let [all-opts (conj opts wsdl-options)
        f (clojure.java.io/file (all-opts :java-output-directory))]
    (if-not (.exists f)
      (.mkdirs f)))
  (doseq [wsdl wsdl-list]
    (let [options (compose-options-array wsdl wsdl-options)]
      (debug "options: " options)
      (WsImport/main (into-array options))
    )
  )
)

(defn wsimport
  "Generate Java code from SOAP .wsdls using the JDK's wsimport task

To use this task, you need to add two pieces to your 'project.clj' file:

    :wsimport {:wsdl-list [ \"put.wsdl\" \"your.wsdl\"
                            \"files.wsdl\" \"here.wsdl\" ]]}
    ;; can also use URIs like \"http://somewhere.com/remote-wsdl.wsdl\"

    ;; add java paths to compile in your project, too
    :java-source-paths [\"target/generated/java\"]

There are alot of other options that are provided by the 'wsimport' task to get
you through if you find your wsdls require extra options (such as Amazon's
need for '-extension' in some wsdls).  They are:

    :wsimport { :wsdl-list [ \"Sample.wsdl\" \"ec2.wsdl\" … ]
                :compile-java-sources true ;; default is false
                :java-output-directory \"target/generated/java\" ;; default
                :keep-java-sources true ;; default
                :java-package-name \"com.corporate.prefix.package\"
                :quiet-output true ;; don't show Sun's 'wsimport' output
                :jaxb-binding-files [ \"binding1\" \"binding2\" ]
                :extra-options [\"-extension\" \"-catalog\" ] ;; catch-all for
                                                          ;; any cmd-line opts
                                                          ;; missed
              }

The catch-all for options is the ':extra-options' vector, which will let you
pass in any options that the standard `wsimport` task takes, passing those
along.

For more information on this plugin see the homepage:

https://github.com/klauern/lein-wsimport"
  ([project]
   (debug "project: " project)
   (import-wsdls (-> project :wsimport :wsdl-list) (project :wsimport))))
