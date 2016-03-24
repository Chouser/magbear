(ns cljs-repl-web.replumb-proxy
  (:require [replumb.core :as replumb]
            [replumb.repl :as replumb-repl]
            [cljs-repl-web.io :as io]
            [cljs-repl-web.config :as config]
            [adzerk.cljs-console :as log :include-macros true]))

(defn repl-options
  "Options for replumb.core/read-eval-call.

  Read the docs at https://github.com/ScalaConsultants/replumb"
  [verbose? src-paths]
  (merge (replumb/options :browser src-paths io/fetch-file!)
         {:warning-as-error true
          :verbose verbose?}))

(defn read-eval-call [opts cb source]
  (let [ns (replumb-repl/current-ns)]

    (replumb/read-eval-call opts
                            (fn [result]
                              (log/debug "Top of read-eval-call cb: ~{result}")
                              (cb {:success? (replumb/success? result)
                                   :result   (replumb/unwrap-result result)
                                   :prev-ns  ns
                                   :source   source}))
                            source)))

(defn multiline?
  [input]
  (try
    (replumb-repl/read-string {:features #{:cljs}} input)
    false
    (catch :default e
      (log/warn "multiline? caught @{e}")
      true)))

(def eval-opts {:get-prompt  replumb/get-prompt
                :should-eval (complement multiline?)
                :evaluate    (partial read-eval-call
                                      (repl-options (:verbose-repl? config/defaults)
                                                    (:src-paths config/defaults)))})
