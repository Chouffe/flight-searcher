(ns template.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
    (:require [reagent.core :as reagent :refer [atom]]
              [cljs.core.async :refer [<! put! chan timeout]]
              [cljs-http.client :as http]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]))

;; ------------------------
;; Model

(def app-state (atom {:search nil :searching? false}))

(defn providers [search-results]
  (into #{} (map :provider search-results)))

;; -------------------------
;; API calls

(def base-api "http://localhost:8000/")

(defn search! []
  (swap! app-state assoc :searching? true)
  (go (when-let [{:keys [body] :as response} (<! (http/get (str  base-api "flights/search")))]
        (swap! app-state assoc :search (:results body))
        (swap! app-state assoc :searching? false))))

;; -------------------------
;; Views

(defn row->tr [row]
  (into [:tr] (map (fn [cell] [:td (str cell)]) row)))

(defn header->th [header] [:th header])

(def table
  (fn [[headers & rows]]
    [:table.table.table-striped
     (into [:thead] (map header->th headers))
     (into [:tbody] (apply map row->tr rows))]))

(defn navbar []
  [:nav.navbar.navbar-default
   [:div.container-fluid
    [:div.navbar-header
     [:a.navbar-brand {:href "#"} "Hipmunk Coding Challenge"]]]])

(defn make-page [& components]
  [:div
   [navbar]
   (into [:div.container] components)])

(defn home-page []
  (make-page
    [:div.text-center
     [:img {:src "/images/brand.png" :style {:height "200px"}}]
     [:br]
     [:br]
     (if-not (get @app-state :searching?)
       [:div
        [:div.btn.btn-primary.btn-lg {:on-click (fn [_] (search!))}
         "Search Hipmunk"]]
       [:div.btn.btn-success.btn-lg "Searching Hipmunk "
        [:i.fa.fa-circle-o-notch.fa-spin.fa-1x]])]
    (when-let [search-results (get @app-state :search)]
      [:div.text-center {:style {:padding-top "10px"}}
       "Found " (str (count search-results)) " flights from "
       (str (count (providers search-results))) " providers"])
    [:br]
    [:br]
    (when-let [search-results (get @app-state :search)]
      [table [(mapv name (keys (first search-results))) (mapv vals search-results)]])))

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))

;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (accountant/configure-navigation!)
  (accountant/dispatch-current!)
  (mount-root))
