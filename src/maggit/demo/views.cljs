(ns maggit.demo.views
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [maggit.views :refer [navigable-list scrollable-list text-input]]))

(defn <sub [query]
  (rf/subscribe [:get-in query]))

(defn status []
  (let [{:keys [branch-name
                head-commit-summary
                untracked
                unstaged
                staged]}
        @(<sub [:repo])

        selected (<sub [:router/view-state :selected])]
    [:box#status
     {:top 0
      :right 0
      :width "100%"
      :style {:border {:fg :magenta}}
      :border {:type :line}
      :label " Status "}
     [:box#head
      {:top 1
       :left 1
       :right 2
       :align :left}
      [:text (str "Head: [" branch-name "] " head-commit-summary)]]
     [navigable-list
      {:top 4
       :left 1
       :align :left
       :items [(str "Untracked (" (count untracked) ")")
               (str "Unstaged (" (count unstaged) ")")
               (str "Staged (" (count staged) ")")
               (str "Commit Log")]
       :selected @selected
       :custom-key-handlers
       {["c"] {:f (fn [idx]
                    (rf/dispatch [:router/goto :input
                                  {:label "Commit Message"
                                   :on-submit (fn [msg]
                                                (rf/dispatch [:assoc-in [:router/view-state :selected] idx])
                                                (rf/dispatch [:toast "Commiting"])
                                                (rf/dispatch [:commit msg])
                                                (rf/dispatch [:router/goto-and-forget :commits]))
                                   :on-cancel #(rf/dispatch [:router/go-back])}]))
               :label "Commit"
               :type "Action"}}
       :on-select
       (fn [x]
         (rf/dispatch [:assoc-in [:router/view-state :selected] x])
         (cond
           (< x 3)
           (rf/dispatch [:router/goto :files
                         (case x
                             0 {:label "Untracked"
                                :files-path [:repo :untracked]}
                             1 {:label "Unstaged"
                                :files-path [:repo :unstaged]}
                             2 {:label "Staged"
                                :files-path [:repo :staged]})])
           (== x 3)
           (rf/dispatch [:router/goto :commits])))}]]))

(defn files []
  (let [{:keys [files-path label selected]}
        @(<sub [:router/view-state])

        files (<sub files-path)]
    [:box#files
     {:top 0
      :right 0
      :width "100%"
      :style {:border {:fg :magenta}}
      :border {:type :line}
      :label (str " " label " ")}
     [navigable-list
      {:top 1
       :left 1
       :right 2
       :align :left
       :items @files
       :selected selected
       :custom-key-handlers
       {["s"] {:f (fn [x]
                    (rf/dispatch [:toast "Staging " (nth @files x)])
                    (rf/dispatch [:stage-file (nth @files x)]))
               :label "Stage"
               :type "Action"}
        ["u"] {:f (fn [x]
                    (rf/dispatch [:toast "Unstaging " (nth @files x)])
                    (rf/dispatch [:unstage-file (nth @files x)]))
               :label "Unstage"
               :type "Action"}
        ["r"] {:f (fn [x]
                    (rf/dispatch [:toast "Untracking " (nth @files x)])
                    (rf/dispatch [:untrack-file (nth @files x)]))
               :label "Untrack"
               :type "Action"}
        ["k"] {:f (fn [x]
                    (rf/dispatch [:toast "Checking out " (nth @files x)])
                    (rf/dispatch [:checkout-file (nth @files x)]))
               :label "Checkout"
               :type "Action"}}
       :on-select
       (fn [idx]
         (rf/dispatch [:assoc-in [:router/view-state :selected] idx])
         (rf/dispatch [:router/goto :diffs]))
       :on-back
       #(rf/dispatch [:router/go-back])}]]))

(defn diffs []
  (let [text (<sub [:router/view-state :text])
        size (<sub [:terminal/size])
        rows (:rows @size)]
    [:box#diffs
     {:top 0
      :right 0
      :width "100%"
      :style {:border {:fg :magenta}}
      :border {:type :line}
      :label (str " Diff ")}
     [scrollable-list
      {:top 1
       :left 1
       :right 2
       :align :left
       :window-size (-> rows (* 0.6) (- 4))
       :items (clojure.string/split @text #"\n")
       :item-props-f
       (fn [line]
         (case (first line)
           \- {:style {:fg :red}}
           \+ {:style {:fg :green}}
           {:style {:fg :white}}))
       :on-back
       #(rf/dispatch [:router/go-back])}]]))


(defn commits []
  (let [commits (<sub [:repo :commits])
        selected (<sub [:router/view-state :selected])
        size (<sub [:terminal/size])
        rows (:rows @size)]
    (with-meta
      [:box#commits
       {:top 0
        :right 0
        :width "100%"
        :style {:border {:fg :magenta}}
        :border {:type :line}
        :label " Commit Log "}
       [scrollable-list
        {:top 1
         :left 1
         :right 2
         :align :left
         :window-size (-> rows (* 0.6) (- 4))
         :items (for [{:keys [sha summary]} @commits]
                  (str (->> sha (take 7) clojure.string/join)
                       " "
                       summary))
         :selected @selected
         :on-select
         (fn [idx]
           (rf/dispatch [:assoc-in [:router/view-state :selected] idx])
           (rf/dispatch [:show-commit (nth @commits idx)]))
         :on-back
         #(rf/dispatch [:router/go-back])}]]
      {:component-did-mount
       (fn [this]
         (reset! rows (-> this .-refs .-commits .-height)))})))

(defn input []
  (let [{:keys [label on-submit on-cancel]}
        @(<sub [:router/view-state])]
    [:box
     {:top 0
      :right 0
      :width "100%"
      :style {:border {:fg :magenta}}
      :border {:type :line}
      :label (str " " label " ")}
     [text-input
      {:top 1
       :left 1
       :height 10
       :on-submit on-submit
       :on-cancel on-cancel}]]))

(defn viewport [height]
  [:box#viewport
   {:height height}
   (let [view @(<sub [:router/view])]
     [(case view
        :status status
        :files files
        :commits commits
        :diffs diffs
        :input input)])])

(defn toast []
  (let [text @(<sub [:toast/view-state :text])]
    [:box#toast
     {:bottom 0
      :height 3
      :style {:border {:fg :magenta}}
      :border {:type :line}}
     text]))

(defn home []
  (let [size (<sub [:terminal/size])
        rows (:rows @size)]
    [:box#home
     {:top 0
      :left 0
      :height "100%"
      :width "100%"}
     [viewport (- rows 3)]
     [toast]]))
