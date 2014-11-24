;; Copyright (c) 2014 Engagor
;;
;; The use and distribution terms for this software are covered by the
;; BSD License (http://opensource.org/licenses/BSD-2-Clause)
;; which can be found in the file LICENSE at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns ^{:doc "Core functionality for interacting with vowpal wabbit in a generic way (input example
formatting, writing data files, passing options and calling vw, ...)."} 
  clj-vw.core
  (:require [clojure.java [shell :refer [sh]] [io :refer :all]]
            [clojure.pprint :refer (print-table)])
  (:import [java.io Writer]))


;;; ========================================================================
;;; Example Formatting
;;; ========================================================================

(defn- ^java.lang.String clean-string
  "replaces all occurrences of characters in #{\\:, \\space, \\newline, \\|} by a space."
  [^java.lang.String input]
  (clojure.string/replace input #"[\n|:]" " "))

(defn- ^java.lang.String format-namespace [x]
  (if (map? x)
    (if-not (empty? (:name x))
      (str (clean-string (:name x))
           (when (:weight x) (str ":" (:weight x))))
      "")
    (str x)))

(defn- ^java.lang.String format-feature [x]
  (if (map? x)
    (if-not (empty? (:name x))
      (str (clean-string (:name x)) (when (:value x) (str ":" (:value x))))
      "")
    (clean-string (str x))))

(defn- ^java.lang.String format-label [x]
  (if (map? x)
    (str (:value x) (when (:cost x) (str ":" (:cost x))))
    (str x)))

;;; ========================================================================
;;; Command Line Arguments
;;; ========================================================================

(def ^{:private true} +options+ (atom []))

(defmacro ^{:private true} defoption [key {:keys [short-name long-name] :as cfg}]
  (assert (or short-name long-name))
  `(swap! +options+ conj (assoc ~cfg 
                           :key ~key)))

(defn- option [key]
  (first (filter #(= key (:key %)) @+options+)))

(defn- command-line-args [options]
  (reduce (fn [ret [key val]]
            (if-let [option (option key)]
              (if (= true val)
                (conj ret (or (:long-name option) (:short-name option)))
                (conj ret (or (:long-name option) (:short-name option)) (str val)))
              ret))
          [] options))

(defmacro assert-options
  "Macro for performing multiple asserts of the form (assert (get-option settings key))
  simulataneously."
  [settings & option-keys]
  `(do ~@(map (fn [key]
                `(assert (get-option ~settings ~key)))
              option-keys)))

;;; VW options

(defoption :help
  {:long-name "--help"
   :short-name "-h"
   :doc "Look here: http://hunch.net/~vw/ and click on Tutorial."})

(defoption :version
  {:long-name "--version"
   :doc "Version information"})

(defoption :random-seed
  {:long-name "--random_seed"
   :doc "seed random number generator"})

(defoption :noop
  {:long-name "--noop"
   :doc "do no learning"})

;;; Input options

(defoption :data
  {:short-name "-d" 
   :long-name "--data" 
   :doc "Example Set"})

(defoption :ring-size
  {:long-name "--ring_size"
   :doc "size of example ring"})

(defoption :examples
  {:long-name "--examples"
   :doc "number of examples to parse"})

(defoption :daemon
  {:long-name "--daemon"
   :doc "read data from port 26542"})

(defoption :port 
  {:long-name "--port"
   :doc "port to listen on"})

(defoption :num-children
  {:long-name "--num_children"
   :doc "number of children for persistent daemon mode"})

(defoption :pid-file
  {:long-name "--pid_file"
   :doc "Write pid file in persistent daemon mode"})

(defoption :passes 
  {:long-name "--passes"
   :doc "Number of Training Passes"})

(defoption :cache 
  {:short-name "-c" 
   :long-name "--cache"
   :doc "Use a cache.  The default is <data>.cache"})

(defoption :cache-file
  {:long-name "--cache_file"
   :doc "The location(s) of cache_file."})

(defoption :compressed 
  {:long-name "--compressed"
   :doc "use gzip format whenever possible."})

(defoption :no-stdin
  {:long-name "--no_stdin"
   :doc "do not default to reading from stdin"})

(defoption :save-resume 
  {:long-name "--save_resume"
   :doc "save extra state so learning can be resumed later with new data"})

;;; Output options

(defoption :audit
  {:short-name "-a" 
   :long-name "--audit"
   :doc "print weights of features"})

(defoption :predictions
  {:short-name "-p" 
   :long-name "--predictions"
   :doc "File to output predictions to"})

(defoption :raw-preditions
  {:short-name "-r" 
   :long-name "--raw_predictions" 
   :doc "File to output unnormalized predictions to"})

(defoption :sendto
  {:long-name "--sendto"
   :doc "send compressed examples to <host>"})

(defoption :quiet
  {:long-name "--quiet"
   :doc "Don't output diagnostics"})

(defoption :progress
  {:short-name "-P" 
   :long-name "--progress"
   :doc "Progress update frequency. int: additive, float: multiplicative"})

(defoption :min-prediction
  {:long-name "--min_prediction"
   :doc "Smallest prediction to output"})

(defoption :max-prediction
  {:long-name "--max_prediction"
   :doc "Largest prediction to output"})


;;; Example manipulation options

(defoption :test-only
  {:short-name "-t" 
   :long-name "--testonly"
   :doc "Ignore label information and just test"})

(defoption :quadratic
  {:short-name "-q" 
   :long-name "--quadratic"
   :doc "Create and use quadratic features"})

(defoption :cubic 
  {:long-name "--cubic"
   :doc "Create and use cubic features"})

(defoption :ignore
  {:long-name "--ignore"
   :doc "ignore namespaces beginning with character <arg>"})

(defoption :keep
  {:long-name "--keep"
   :doc "keep namespaces beginning with character <arg>"})
                                 
(defoption :holdout-off
  {:long-name "--holdout_off"
   :doc "no holdout data in multiple passes"})

(defoption :holdout-period
  {:long-name "--holdout_period"
   :doc "holdout period for test only, default 10"})

(defoption :sort-features
  {:long-name "--sort_features"
   :doc "disregard order of features for smaller cache sizes"})

(defoption :no-constant
  {:long-name "--noconstant"
   :doc "Don't add a constant feature"})

(defoption :constant
  {:short-name "-C" 
   :long-name "--constant"
   :doc "Set initial value of the constant feature to arg"})

(defoption :ngram
  {:long-name "--ngram"
   :doc "Generate N grams"})

(defoption :skips
  {:long-name "--skips"
   :doc "Generate skips in N grams."})

(defoption :hash
  {:long-name "--hash"
   :doc "how to hash the features. Available options: strings, all"})

;;; Update rule options

(defoption :sgd
  {:long-name "--sgd"
   :doc "use regular/classic/simple stochastic gradient descent update"})

(defoption :adaptive
  {:long-name "--adaptive"
   :doc "use adaptive, individual learning rates (on by default)"})

(defoption :normalized
  {:long-name "--normalized"
   :doc "use per feature normalized updates. (on by default)"})

(defoption :invariant
  {:long-name "--invariant"
   :doc "use safe/importance aware updates (on by default)"})

(defoption :conjugate-gradient
  {:long-name "--conjugate_gradient"
   :doc "use conjugate gradient based optimization (option in bfgs)"})

(defoption :bfgs
  {:long-name "--bfgs"
   :doc "use bfgs optimization"})

(defoption :mem
  {:long-name "--mem"
   :doc "memory in bfgs"})

(defoption :termination
  {:long-name "--termination"
   :doc "Termination threshold"})

(defoption :hessian-on
  {:long-name "--hessian_on"
   :doc "use second derivative in line search"})

(defoption :initial-pass-length
  {:long-name "--initial_pass_length"
   :doc "initial number of examples per pass"})

(defoption :l1
  {:long-name "--l1" 
   :doc "l_1 lambda (L1 regularization)"})

(defoption :l2
  {:long-name "--l2"
   :doc "l_2 lambda (L2 regularization)"})

(defoption :decay-learning-rate
  {:long-name "--decay_learning_rate"
   :doc "Set Decay factor for learning_rate between passes"})

(defoption :initial-t
  {:long-name "--initial_t"
   :doc "initial t value"})

(defoption :power-t
  {:long-name "--power_t"
   :doc "t power value"})

(defoption :learning-rate 
  {:short-name "-l" 
   :long-name "--learning_rate" 
   :doc "Set Learning Rate"})

(defoption :loss-function
  {:long-name "--loss_function"
   :doc "Specify the loss function to be used (squared, hinge, logistic or quantile)."})

(defoption :quantile-tau
  {:long-name "--quantile_tau"
   :doc "Parameter \\tau associated with Quantile loss. Default 0.5"})

(defoption :minibatch
  {:long-name "--minibatch"
   :doc "Minibatch size"})

(defoption :feature-mask
  {:long-name "--feature_mask"
   :doc "Use existing regressor to determine the parameters to updated"})


;;; Weight options

(defoption :bit-precision
  {:short-name "-b" 
   :long-name "--bit_precision"                  
   :doc "number of bits in the feature table"})

(defoption :initial-regressor 
  {:short-name "-i" 
   :long-name "--initial_regressor"              
   :doc "Initial regressor(s) to load into memory ( is filename)"})

(defoption :final-regressor
  {:short-name "-f" 
   :long-name "--final_regressor"
   :doc "Final regressor to save ( is filename)"})

(defoption :random-weights
  {:long-name "--random_weights"
   :doc "make initial weights random"})

(defoption :initial-weight
  {:long-name "--initial_weight"
   :doc "Set all weights to an initial value of 1."})

(defoption :readable-model
  {:long-name "--readable_model"
   :doc "Output human-readable final regressor"})

(defoption :invert-hash
  {:long-name "--invert_hash"
   :doc "Output human-readable final regressor with feature names"})

(defoption :save-per-pass
  {:long-name "--save_per_pass"
   :doc "Save the model after every pass over data"})

(defoption :input-feature-regularizer
  {:long-name "--input_feature_regularizer"
   :doc "Per feature regularization input file"})

(defoption :output-feature-regularizer-binary
  {:long-name "--output_feature_regularizer_binary"
   :doc "Per feature regularization output file"})

(defoption :output-feature-regularizer-text
  {:long-name "--output_feature_regularizer_text"
   :doc "Per feature regularization output file, in text"})

	

;;; Holdout options 

(defoption :holdout-off
  {:long-name "--holdout_off"
   :doc "no holdout data in multiple passes"})

(defoption :holdout-period
  {:long-name "--holdout_period"
   :doc "holdout period for test only, default 10"})

(defoption :holdout-after
  {:long-name "--holdout_after"
   :doc "holdout after n training examples. Default off."})

(defoption :early-terminate
  {:long-name "--early_terminate"
   :doc "Specify the number of passes tolerated when holdout loss doesn't decrease."})


;;; Feature namespace options

(defoption :hash
  {:long-name "--hash"
   :doc "how to hash the features. Available options: strings, all"})

(defoption :ignore
  {:long-name "--ignore"
   :doc "ignore namespaces beginning with character <arg>"})

(defoption :keep
  {:long-name "--keep"
   :doc "keep namespaces beginning with character <arg>"})

(defoption :affix
  {:long-name "--affix"
   :doc "generate prefixes/suffixes of features."})

(defoption :spelling
  {:long-name "--spelling"
   :doc "compute spelling features for a give namespace."})

;;; Latent dirichlet options

(defoption :lda
  {:long-name "--lda"
   :doc "Run lda with <int> topics"})

(defoption :lda-alpha
  {:long-name "--lda_alpha"
   :doc "Prior on sparsity of per-document topic weights"})

(defoption :lda-rho
  {:long-name "--lda_rho"
   :doc "Prior on sparsity of topic distributions"})

(defoption :lda-D
  {:long-name "--lda_D"
   :doc "Number of documents"})

;;; Matrix Factorization options

(defoption :rank
  {:long-name "--rank"
   :doc "rank for matrix factorization."})


;;; Low rank quadratic options

(defoption :lrq
  {:long-name "--lrq"
   :doc "use low rank quadratic features"})

(defoption :lrq-dropout
  {:long-name "--lrqdropout"
   :doc "use dropout training for low rank quadratic features"})


;;; Binary

(defoption :binary
  {:long-name "--binary"
   :doc "Reports loss as binary classification with -1,1 labels"})


;;; Multiclass options

(defoption :oaa
  {:long-name "--oaa"
   :doc "Use one-against-all multiclass learning with <k> labels"})

(defoption :ect
  {:long-name "--ect"
   :doc "Use error correcting tournament with <k> labels"})

(defoption :csoaa
  {:long-name "--csoaa"
   :doc "Use one-against-all multiclass learning with <k> costs"})

(defoption :wap
  {:long-name "--wap"
   :doc "Use weighted all-pairs multiclass learning with <k> costs"})

(defoption :csoaa-ldf
  {:long-name "--csoaa_ldf"
   :doc "Use one-against-all multiclass learning with ldf (singleline or multiline.)"})


(defoption :wap-ldf
  {:long-name "--wap_ldf"
   :doc "Use weighted all-pairs multiclass learning with ldf (singleline or multiline.)"})

(defoption :log-multi
  {:long-name "--log_multi"
   :doc "Use online (decision) trees for <arg> classes. See http://arxiv.org/pdf/1406.1822"})


;;; Stagewise Polynomial options

(defoption :stage-poly
  {:long-name "--stage_poly"
   :doc "stagewise polynomial features"})

(defoption :sched-exponent
  {:long-name "--sched_exponent"
   :doc "exponent controlling quantity of included features"})

(defoption :batch-sz
  {:long-name "--batch_sz"
   :doc "multiplier on batch size before including more features"})

(defoption :batch-sz-no-doubling
  {:long-name "--batch_sz_no_doubling"
   :doc "batch_sz does not double"})


;;; Active Learning options

(defoption :active-learning
  {:long-name "--active_learning"
   :doc "active learning mode"})

(defoption :active-simulation
  {:long-name "--active_simulation"
   :doc "active learning simulation mode"})

(defoption :active-mellowness
  {:long-name "--active_mellowness"
   :doc "active learning mellowness parameter c_0. Default 8"})

;;; Parallelization options

(defoption :span-server
  {:long-name "--span_server"
   :doc "Location of server for setting up spanning tree"})

(defoption :unique-id
  {:long-name "--unique_id"
   :doc "unique id used for cluster parallel job"})

(defoption :total
  {:long-name "--total"
   :doc "total number of nodes used in cluster parallel job"})

(defoption :node
  {:long-name "--node"
   :doc "node number in cluster parallel job"})


;;; Label dependent features

(defoption :csoaa-ldf
  {:long-name "--csoaa_ldf"
   :doc "multiline|singleline"})

(defoption :wap-ldf
  {:long-name "--wap_ldf"
   :doc "multiline|singleline"})


;;; Learning algorithm / reduction options

(defoption :bootstrap
  {:short-name "-B" 
   :long-name "--bootstrap"  
   :doc "bootstrap mode with k rounds by online importance resampling"})

(defoption :top
  {:long-name "--top"
   :doc "top k recommendation"})

(defoption :bs-type
  {:long-name "--bs_type"
   :doc "bootstrap mode - currently 'mean' or 'vote'"})

(defoption :auto-link
  {:long-name "--autolink"
   :doc "create link function with polynomial d"})

(defoption :cb
  {:long-name "--cb"
   :doc "Use contextual bandit learning with <k> costs"})

(defoption :lda
  {:long-name "--lda"
   :doc "Run LDA with <int> topics"})

(defoption :nn
  {:long-name "--nn"
   :doc "Use sigmoidal feedforward network with <k> hidden units"})

(defoption :cbify
  {:long-name "--cbify"
   :doc "Convert multiclass on <k> classes into a cb problem and solve"})

(defoption :search
  {:long-name "--search"
   :doc "use search-based structured prediction (SEARN or DAgger)."})

;;; ========================================================================
;;; Public API
;;; ========================================================================

(defn format-example [{:keys [label, labels, importance, tag, features] :or {labels [], importance nil, tag "", features []}}]
  "Turns an example given as a map into vowpal wabbit's string format (see https://github.com/JohnLangford/vowpal_wabbit/wiki/Input-format).

Example:

   (format-example {:label -1.231
                    :features [\"f1\"
                               {:name \"f2\" :value 3.5}
                               {:name \"f3\" :namespace \"ns3\"}]})
   => \"-1.231 | f1 f2:3.5 |ns3:1.0 f3\"

  See test suite for more examples.
"
  (str (if label          
         (format-label label)
         (clojure.string/join " " (map format-label labels)))
       (when importance 
         (str " " importance))
       (when tag
         (clean-string (str " " tag)))
       (clojure.string/trim (reduce (fn [ret [ns features]]
                                      (str ret  
                                           (apply str "|"
                                                  (format-namespace ns)
                                                  " "
                                                  (clojure.string/join " " (map format-feature features)))
                                           " "))
                                    ""
                                    (group-by :namespace features)))))

(defn add-example 
  "Add one ore more examples to settings."
  ([settings example]
     (update-in settings [:data] (fnil conj []) example))
  ([settings example & more]
     (reduce add-example (add-example settings example) more)))

(defn available-options
  "Print and return the set of available options and their documentation."
  []
  (print-table [:key :doc] @+options+)
  @+options+)

(defn set-option 
  "Set one or more options. Can be chained, e.g.

  (def settings (-> {}
                    (set-option :data \"foo/bar.dat\")
                    (set-option :save-resume true)
                    (set-option :ngram 3)
                    (set-option :quadratic \"ab\")
                    (set-option :quadratic \"ac\")
                    (set-option :learning-rate 0.3)))
"
  ([key val]
     (set-option {} key val))
  ([settings key val]
     (update-in settings [:options] (fnil conj []) [key val]))
  ([settings key val & more]
     (reduce (fn [ret [key val]]
               (set-option ret key val))
             (set-option settings key val)
             (partition 2 more))))

(defn get-option
  "Return option for key in settings."
  [settings key]
  (second (first (filter #(= key (first %))
                         (:options settings)))))

(defn maybe-set-option 
  "Same as set-option but only when option is unset."
  ([settings key]
     (maybe-set-option settings key true))
  ([settings key val]
     (if (nil? (get-option settings key))
       (update-in settings [:options] (fnil conj []) [key val])
       settings))
  ([settings key val & more]
     (reduce (fn [ret [key val]]
               (maybe-set-option ret key val))
             (maybe-set-option settings key val)
             (partition 2 more))))

(defn remove-option 
  "Remove an option from settings."
  ([settings key]
     (update-in settings [:options]
                #(remove (fn [opt] (= key (first opt))) %)))
  ([settings key & more]
     (reduce remove-option settings (conj more key))))



(defn write-data-file 
  "Writes (:data settings), a collection of examples, to (get-option settings :data)."
  ([settings]
     (write-data-file settings {}))
  ([settings writer-settings]
     (assert (get-option settings :data))
     (assert (not (empty? (:data settings))))
     (with-open [wr ^Writer (apply writer (clojure.java.io/as-file (get-option settings :data))
                                   (flatten (seq writer-settings)))]
       (doseq [e (:data settings)] 
         (.write wr ^java.lang.String (format-example e))
         (.write wr "\n")))
     settings))

(defn vw-command 
  "Returns the vw command as a string as defined by settings. Useful for inspecting the command that
  wille be issued when calling vw on settings."
  [settings]
  (str "vw " (clojure.string/join " " (command-line-args (:options settings)))))

(defn vw
  "Calls vw as specified by settings. Puts output in settings under :output and returns
  updated settings."
  [settings]
  (print "\n" (vw-command settings))
  (let [ret (apply sh "vw" (command-line-args (:options settings)))]
    (print "\n" (:err ret))
    (if (= 0 (:exit ret))
      settings
      (throw (Exception. "vowpal wabbit segmentation fault")))))


(defn read-predictions 
  "Read vowpal wabbit prediction file as specified by (get-option settings :predictions)."
  [settings]
  (let [predictions (with-open [rdr (clojure.java.io/reader (get-option settings :predictions))]
                      (doall (mapv #(hash-map :label (read-string %)
                                              :tag (second (clojure.string/split % #" ")))
                                   (line-seq rdr))))]
    (assoc settings :predictions predictions)))
