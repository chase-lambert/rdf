(ns rdf.core
  (:require 
    [openai-clojure.api :as api])
  (:import 
    [org.apache.jena.rdf.model ModelFactory] 
    [org.apache.jena.vocabulary  RDF]))

(def openai-config {:api-key (System/getenv "OPENAI_API_KEY")})

(defn create-knowledge-graph []
  (let [model (ModelFactory/createDefaultModel)
        ns    "http://example.org/"]
    (-> model
        (.createResource (str ns "Person1"))
        (.addProperty RDF/type (.createResource model (str ns "Person")))
        (.addProperty (.createProperty model (str ns "name")) "Alice")
        (.addProperty (.createProperty model (str ns "role")) "Developer"))
    model))

(defn rdf-to-prompt [model]
  (with-open [out (java.io.StringWriter.)]
    (.write model out "TURTLE")
    (str "Given this RDF data in Turtle format:\n\n"
         (.toString out)
         "\n\nPlease analyze this data and describe the person.")))

(defn query-llm [prompt]
  (let [response (api/create-chat-completion
                  {:model    "gpt-4o-2024-11-20"
                   :messages [{:role    "system"
                               :content "You are a helpful assistant that analyzes RDF data."}
                              {:role    "user"
                               :content prompt}]}
                  {:api-key (:api-key openai-config)})]
    response))

(defn -main [& args]
  (let [kg       (create-knowledge-graph)
        prompt   (rdf-to-prompt kg)
        response (query-llm prompt)]
    (println "Analysis:")
    (println (-> response :choices first :message :content))
    (System/exit 0)))

(comment
  (let [kg       (create-knowledge-graph)
        prompt   (rdf-to-prompt kg)
        response (query-llm prompt)]
    (println "Analysis:")
    (println (-> response :choices first :message :content)))
 ,)
