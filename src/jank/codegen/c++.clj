(ns jank.codegen.c++
  (:require [jank.parse.fabricate :as fabricate]
            [jank.codegen.sanitize :as sanitize]
            [jank.codegen.util :as util]
            [jank.codegen.mangle :as mangle])
  (:use clojure.pprint
        jank.assert
        jank.debug.log))

(defmulti codegen-impl
  (fn [current]
    (:kind current)))

(defmethod codegen-impl :type-declaration
  [current]
  "")

(defmethod codegen-impl :binding-declaration
  [current]
  "")

; Only used for the main functions; all other functions
; are just local lambdas within main
(defmethod codegen-impl :function-definition
  [current]
  (let [lambda (:value current)]
    (str (codegen-impl (:return lambda))
         " "
         (codegen-impl (:name current))
         (codegen-impl (:arguments lambda))
         "{"
         (util/reduce-spaced-map (comp util/end-statement codegen-impl)
                                 (:body lambda))
         "}")))

(defmethod codegen-impl :lambda-definition
  [current]
  (str "[=]"
       (codegen-impl (:arguments current))
       "->"
       (codegen-impl (:return current))
       "{"
       (util/reduce-spaced-map (comp util/end-statement codegen-impl)
                               (:body current))
       "}"))

(defmethod codegen-impl :binding-type
  [current]
  (let [value (:value current)]
    (cond
      ; Lambdas can be recursive, so their type needs to be specified
      (= (:kind value) :lambda-definition)
      (str "std::function<"
           (codegen-impl (:return value))
           (codegen-impl (:arguments value))
           "> const ")

      ; Typically, we just want auto
      :else
      "auto const ")))

(defmethod codegen-impl :struct-definition
  [current]
  ; TODO: member functions
  (str "struct "
       (codegen-impl (:name current))
       " final {"
       (apply str (map codegen-impl (:members current)))
       "};"
       (apply str (map (comp codegen-impl
                             #(assoc %
                                     :kind :struct-member-function
                                     :struct current))
                       (:members current)))))

(defmethod codegen-impl :struct-member
  [current]
  (util/end-statement
    (str (codegen-impl (:type current))
         " "
         (codegen-impl (:name current)))))

(defmethod codegen-impl :new-expression
  [current]
  "")

(defmethod codegen-impl :struct-member-function
  [current]
  (str "auto const "
       ; We need type info for mangling, so we'll cheat
       (mangle/mangle
         (assoc
           (fabricate/function-declaration (str "." (:name (:name current)))
                                           ["person"]
                                           (-> current :type :value :name))
           :kind :binding-name))
       "=[=]("
       (codegen-impl (:name (:struct current)))
       " const obj){return obj."
       (codegen-impl (:name current))
       ";};"))

(defmethod codegen-impl :binding-name
  [current]
  (let [value (:value current)]
    (cond
      ; Lambda bindings contain type info in the name, to work around
      ; the lack of overloading in the target
      (= (:kind value) :lambda-definition)
      (mangle/mangle current)

      ; A non-function binding, so normal identifier codegen
      :else
      (codegen-impl (:name current)))))

(defmethod codegen-impl :binding-definition
  [current]
  ;(pprint (clean-scope current))
  (str (codegen-impl (assoc current :kind :binding-type))
       (codegen-impl (assoc current :kind :binding-name))
       "="
       (codegen-impl (:value current))))

(defmethod codegen-impl :function-call
  [current]
  ;(pprint (clean-scope current))
  ; External calls don't get mangled
  (str (if (:external? (:signature current))
         (codegen-impl (:name current))
         (mangle/mangle current))
       "("
       (util/comma-separate-args
         (map codegen-impl (:arguments current)))
       ")"))

(defmethod codegen-impl :argument-list
  [current]
  (str "("
       (util/comma-separate-params
         (util/swap-params
           (map codegen-impl (:values current))))
       ")"))

(defmethod codegen-impl :return-list
  [current]
  (if-let [ret (first (:values current))]
    (codegen-impl ret)
    "void"))

(defmethod codegen-impl :if-expression
  [current]
  (let [base (str "[=]()->"
                  ; If expressions used as returns need a type to be specified
                  (if-let [if-type (:type current)]
                    (codegen-impl if-type)
                    "void")
                  "{if("
                  (codegen-impl (:value (:condition current)))
                  "){"
                  (util/end-statement (codegen-impl (:value (:then current))))
                  "}")]
    (str
      (cond
        (contains? current :else)
        (str base
             "else{"
             (util/end-statement
               (codegen-impl (:value (:else current))))
             "}")
        :else
        base)
      "}()")))

(defmethod codegen-impl :return
  [current]
  (str "return "
       (when (some? (:value current))
         (codegen-impl (:value current)))))

(defmethod codegen-impl :list
  [current]
  (not-yet-implemented codegen-assert "lists"))

(defmethod codegen-impl :string
  [current]
  (str "\"" (:value current) "\""))

(defmethod codegen-impl :integer
  [current]
  (:value current))

(defmethod codegen-impl :real
  [current]
  (:value current))

(defmethod codegen-impl :boolean
  [current]
  (:value current))

(defmethod codegen-impl :identifier
  [current]
  ; Special case for function types
  (if (= "ƒ" (:name current))
    (codegen-impl (assoc current :kind :function-type))
    (str (apply str (mapcat (comp sanitize/sanitize str) (:name current)))
         ; Handle generic specializations
         (when (contains? current :generics) ; TODO: migrate
           (codegen-impl (:generics current))))))

(defmethod codegen-impl :function-type
  [current]
  (str "std::function<"
       (if-let [return (-> current :generics :values first :values first)]
         (codegen-impl return)
         "void")
       "("
       (util/comma-separate-args
         (map codegen-impl (-> current :generics :values second :values)))
       ")>"))

(defmethod codegen-impl :type
  [current]
  (str (codegen-impl (:value current)) " const"))

(defmethod codegen-impl :specialization-list
  [current]
  (str "<"
       (util/comma-separate-args
         (map codegen-impl (:values current)))
       ">"))

(defmethod codegen-impl :default
  [current]
  (codegen-assert false (str "no codegen for '" current "'")))

(defn codegen [ast]
  (util/print-statement
    (codegen-impl
      {:kind :function-definition
       :name {:kind :identifier
              :name "#main"}
       :value {:kind :lambda-definition
               :arguments {:kind :argument-list
                           :values []}
               :return {:kind :return-list
                        :values []}
               :body (:cells ast)}})))
