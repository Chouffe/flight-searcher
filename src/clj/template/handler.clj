(ns template.handler
  (:require [compojure.core :refer [ANY GET defroutes]]
            [compojure.route :refer [not-found resources]]
            [hiccup.core :refer [html]]
            [hiccup.page :refer [include-js include-css]]
            [template.middleware :refer [wrap-middleware]]

            [ring.util.response :refer [resource-response response]]
            [ring.middleware.json :as middleware-json]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults api-defaults]]

            [template.farm :as farm]
            [template.utils :refer [merge-sorted-collections-by]]
            [template.decorators :refer [logger recover-and-report]]

            [cheshire.core :as json]
            [environ.core :refer [env]]))

(def mount-target
  [:div#app
      [:h3 "ClojureScript has not been compiled!"]
      [:p "please run "
       [:b "lein figwheel"]
       " in order to start the compiler"]])

(def loading-page
  (html
   [:html
    [:head
     [:meta {:charset "utf-8"}]
     [:meta {:name "viewport"
             :content "width=device-width, initial-scale=1"}]
     [:link {:rel "stylesheet"
             :href "https://maxcdn.bootstrapcdn.com/font-awesome/4.5.0/css/font-awesome.min.css"}]
     (include-css (if (env :dev)
                    "bower_components/bootstrap/dist/css/bootstrap.css"
                    "bower_components/bootstrap/dist/css/bootstrap.min.css"))]
    [:body
     mount-target
     (include-js "js/app.js")]]))

(defroutes routes
  (GET "/" [] loading-page)
  (GET "/about" [] loading-page)

  (resources "/")
  (not-found "Not Found"))

(def html-routes (wrap-middleware #'routes))

(defroutes api-routes
  (GET "/flights/search" []
       (->> farm/providers
            ;; Add a timeout?
            (pmap (->> #'farm/scrape logger recover-and-report))
            (merge-sorted-collections-by :agony <)
            (hash-map :results)
            response)))

(defn wrap-api-middleware [handler]
  (-> handler
      (middleware-json/wrap-json-body)
      (middleware-json/wrap-json-response)
      (wrap-defaults api-defaults)
      wrap-params))

(defroutes main-routes
  (ANY "/flights/*" [] (wrap-api-middleware #'api-routes))
  (ANY "*" [] (wrap-middleware #'routes)))

(def app main-routes)
