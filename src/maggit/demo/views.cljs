(ns maggit.demo.views
  (:require [re-frame.core :as rf]
            [maggit.views :refer [navigable-list]]))

(defn status []
  (let [{:keys [branch-name
                head-commit-message
                unstaged
                staged]}
        @(rf/subscribe [:repo])

        {:keys [selected]}
        @(rf/subscribe [:status-view])]
    [:box#status
     {:top 0
      :right 0
      :width "100%"
      :height "50%"
      :style {:border {:fg :magenta}}
      :border {:type :line}
      :label " Status "}
     [:box#head
      {:top 1
       :left 1
       :right 2
       :align :left}
      [:text (str "Head: [" branch-name "] " head-commit-message)]]
     [navigable-list
      {:top 4
       :left 1
       :align :left
       :items [(str "Unstaged (" (count unstaged) ")")
               (str "Staged (" (count staged) ")")]
       :selected selected
       :on-select
       (fn [x]
         (rf/dispatch [:assoc-in [:status-view :selected] x])
         (rf/dispatch [:assoc-in [:files-view :files-path]
                       (case x
                         0 [:repo :unstaged]
                         1 [:repo :staged])])
         (rf/dispatch [:assoc-in [:files-view :label]
                       (case x
                         0 "Unstaged"
                         1 "Staged")])
         (rf/dispatch [:assoc-in [:router/view] :files]))}]]))

(defn files []
  (let [{:keys [files-path label]}
        @(rf/subscribe [:files-view])

        files @(rf/subscribe [:get-in files-path])]
    [:box#files
     {:top 0
      :right 0
      :width "100%"
      :height "50%"
      :style {:border {:fg :magenta}}
      :border {:type :line}
      :label (str " " label " ")}
     [navigable-list
      {:top 1
       :left 1
       :right 2
       :align :left
       :items files
       :on-back
       #(do
          (rf/dispatch [:assoc-in [:files-view] {}])
          (rf/dispatch [:assoc-in [:router/view] :status]))}]]))

(defn home []
  (let [view @(rf/subscribe [:view])]
    [(case view
       :status status
       :files files)]))
