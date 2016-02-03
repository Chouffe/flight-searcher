(ns template.farm
  (:require [clj-http.client :as client]
            #_[template.utils :refer [merge-sorted-collections-by]]))

(def ^:private config
  {:host "http://localhost"
   :port 9000
   :providers #{:expedia :orbitz :priceline :travelocity :united}})

(def providers (get config :providers))

(def ^:private endpoint (format "%s:%s" (get config :host) (get config :port)))

(defn scrape [provider]
  {:pre [(get providers provider)]}
  (-> (format "%s/scrapers/%s" endpoint (name provider))
      (client/get {:as :json})
      (get-in [:body :results])))
