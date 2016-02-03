(ns template.server
  (:require [template.handler :refer [app]]
            [ring.adapter.jetty :refer [run-jetty]])
  (:gen-class))

 (defn -main [& args]
   (run-jetty app {:port 8000 :join? false}))
