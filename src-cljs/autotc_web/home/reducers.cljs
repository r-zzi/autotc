(ns autotc-web.home.reducers
  (:require [rex.ext.cursor :as c]
            [rex.ext.reducer-for-type :as r]
            [autotc-web.util.reducer-helpers :as h]))

(defn update-agent-selection [set agent selected?]
  ((if selected? conj disj) set (:id agent)))

(defn adjust-filter-value [old-filter-value all-agents new-selected-agents]
  (if (or (= (count new-selected-agents)
             (count all-agents))
          (= 0 (count new-selected-agents)))
    :all
    old-filter-value))

(defn- define-reducers []
  (r/reducer-for-type :init-page
                      (fn [state action]
                        (c/set-state (:cursor action) state {})))

  (r/reducer-for-type
   :on-server-list-loaded
   (fn [state action]
     (c/set-state (:cursor action)
                  state
                  {:servers (:servers action)
                   :agents []
                   :branches []
                   :project {}
                   :selected-agents #{}
                   :manually-selected-agents #{}
                   :current-problems {:problems nil
                                      :problems-count nil
                                      :current-page 1
                                      :page-count 0
                                      :show-stacktraces false}
                   :tests-with-expanded-stack-traces #{}})))

  (r/reducer-for-type
   :on-agents-list-loaded
   (fn [state action]
     (let [{:keys [cursor
                   agents]}
           action

           {:keys [selected-agents
                   manually-selected-agents]}
           (c/get-state cursor state)

           agents-ids (vec (map :id agents))]
       (h/merge-state state
                      (:cursor action)
                      {:agents
                       (:agents action)

                       :branches
                       (:branches action)

                       :project
                       (:project action)

                       :show-agent-list-loader
                       false

                       :selected-agents
                       (apply hash-set
                              (filter (fn [a] (contains? selected-agents a))
                                      agents-ids))
                       :manually-selected-agents
                       (apply hash-set
                              (filter (fn [a]
                                        (contains? manually-selected-agents a))
                                      agents-ids))}))))

  (r/reducer-for-type
   :agent-list-is-loading
   (fn [state action]
     (h/merge-state state
                    (:cursor action)
                    {:show-agent-list-loader true
                     :agents []
                     :branches []
                     :project {}})))

  (r/reducer-for-type
   :init-load-agent-list
   (fn [state action]
     (h/merge-state state
                    (:cursor action)
                    {:show-agent-list-loader true
                     :selected-server-index (:server-index action)
                     :selected-agents #{}
                     :manually-selected-agents #{}
                     :agents []
                     :branches []
                     :project {}
                     :filter-value nil
                     :current-problems {:problems nil
                                        :problems-count nil
                                        :current-page 1
                                        :page-count 0
                                        :show-stacktraces false}
                     :tests-with-expanded-stack-traces #{}})))

  (r/reducer-for-type
   :attach-poll-agent-timer
   (fn [state action]
     (h/merge-state state
                    (:cursor action)
                    {:poll-agent-timer (:poll-agent-timer action)})))

  (r/reducer-for-type
   :agent-selected
   (fn [state action]
     (let [cursor (:cursor action)
           {:keys [agents
                   selected-agents
                   manually-selected-agents
                   filter-value]} (c/get-state cursor state)
           {:keys [agent
                   selected?]} action
           new-selected-agents (update-agent-selection selected-agents
                                                       agent
                                                       selected?)]
       (h/merge-state state
                      cursor
                      {:selected-agents
                       new-selected-agents

                       :manually-selected-agents
                       (update-agent-selection manually-selected-agents
                                               agent
                                               selected?)

                       :filter-value
                       (adjust-filter-value filter-value
                                            agents
                                            new-selected-agents)}))))

  (r/reducer-for-type
   :select-all-agents
   (fn [state action]
     (let [cursor (:cursor action)
           old-state (c/get-state cursor state)
           {:keys [agents
                   selected-agents
                   manually-selected-agents
                   filter-value]} old-state
           new-selected-agents
           (if (empty? selected-agents)
             (if (empty? manually-selected-agents)
               (apply hash-set (map :id agents))
               (apply hash-set manually-selected-agents))
             (if (< (count selected-agents) (count agents))
               (apply hash-set (map :id agents))
               #{}))]
       (h/merge-state state
                      cursor
                      {:selected-agents
                       new-selected-agents

                       :filter-value
                       (adjust-filter-value filter-value
                                            agents
                                            new-selected-agents)}))))

  (r/reducer-for-type
   :show-message
   (fn [state action]
     (let [{:keys [message
                   message-timer]} action]
       (h/merge-state state
                      (:cursor action)
                      {:message message
                       :message-timer message-timer}))))

  (r/reducer-for-type
   :hide-message
   (fn [state action]
     (h/merge-state state
                    (:cursor action)
                    {:message nil})))

  (r/reducer-for-type
   :filter-changed
   (fn [state action]
     (let [cursor (:cursor action)
           old-state (c/get-state cursor state)
           {:keys [selected-agents
                   agents
                   filter-value]} old-state

           new-filter-value
           (cond
             (empty? selected-agents)
             :all

             (= (count selected-agents)
                (count agents))
             :all

             true
             (:value action))]
       (h/merge-state state
                      (:cursor action)
                      {:filter-value new-filter-value}))))

  (r/reducer-for-type
   :on-current-problems-list-loaded
   (fn [state action]
     (h/merge-state state
                    (:cursor action)
                    {:current-problems
                     {:problems (:current-problems action)
                      :problems-count (:problems-count action)
                      :current-page (:current-page action)
                      :page-count (:page-count action)
                      :show-stacktraces (:show-stacktraces action)}})))
  (r/reducer-for-type
   :on-select-current-problems-page
   (fn [state action]
     (c/update-state (:cursor action)
                     state
                     (fn [s]
                       (assoc-in s [:current-problems :current-page]
                                 (:page action))))))

  (r/reducer-for-type
   :on-toggle-stack-traces
   (fn [state action]
     (c/update-state
      (:cursor action)
      state
      (fn [s]
        (assoc-in s
                  [:current-problems :show-stacktraces]
                  (:value action))))))

  (r/reducer-for-type
   :expand-stack-trace
   (fn [state action]
     (let [test-name (:test-name action)]
       (c/update-state
        (:cursor action)
        state
        (fn [s]
          (update-in s
                     [:tests-with-expanded-stack-traces]
                     (fn [old]
                       (let [f (if (contains? old test-name) disj conj)]
                         (f old test-name))))))))))
