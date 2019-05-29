/*
 * Copyright (c) 2017, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.cluster.coordinator.rdbms;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.cluster.coordinator.commons.CoordinationStrategy;
import org.wso2.carbon.cluster.coordinator.commons.MemberEventListener;
import org.wso2.carbon.cluster.coordinator.commons.configs.CoordinationPropertyNames;
import org.wso2.carbon.cluster.coordinator.commons.exception.ClusterCoordinationException;
import org.wso2.carbon.cluster.coordinator.commons.node.NodeDetail;
import org.wso2.carbon.cluster.coordinator.commons.util.MemberEventType;
import org.wso2.carbon.cluster.coordinator.rdbms.beans.ClusterCoordinatorConfigurations;
import org.wso2.carbon.cluster.coordinator.rdbms.beans.StrategyConfig;
import org.wso2.carbon.cluster.coordinator.rdbms.internal.RDBMSCoordinationServiceHolder;
import org.wso2.carbon.cluster.coordinator.rdbms.util.StringUtil;
import org.wso2.carbon.config.ConfigurationException;
import org.wso2.carbon.config.provider.ConfigProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import javax.sql.DataSource;

/**
 * This class controls the overall process of RDBMS coordination.
 */
public class RDBMSCoordinationStrategy implements CoordinationStrategy {

    /**
     * Class log.
     */
    private static Log log = LogFactory.getLog(RDBMSCoordinationStrategy.class);

    /**
     * Heartbeat interval in seconds.
     */
    private final int heartBeatInterval;

    /**
     * After this much of time the node is assumed to have left the cluster.
     */
    private final int heartbeatMaxRetryInterval;

    /**
     * Thread executor used to run the coordination algorithm.
     */
    private final ExecutorService threadExecutor;

    /**
     * Used to send and receive cluster notifications.
     */
    private RDBMSMemberEventProcessor rdbmsMemberEventProcessor;

    /**
     * Used to communicate with the communication bus context.
     */
    private RDBMSCommunicationBusContextImpl communicationBusContext;

    /**
     * Identifier used to identify the node uniquely in the cluster
     */
    private String localNodeId;

    /**
     * Identifier used to identify the cluster group
     */
    private String localGroupId;

    /**
     * Possible node states
     * <p>
     * +-----------+            +-------------+
     * |   MEMBER  +<---------->+ Coordinator |
     * +-----------+            +-------------+
     */
    private enum NodeState {
        COORDINATOR, MEMBER
    }

    /**
     * Instantiate RDBMSCoordinationStrategy with Datasource configuration read from deployment.yaml
     */
    public RDBMSCoordinationStrategy() {
        this(new RDBMSCommunicationBusContextImpl());
    }

    /**
     * Instantiate RDBMSCoordinationStrategy with provided Datasource
     */
    public RDBMSCoordinationStrategy(DataSource datasource) {
        this(new RDBMSCommunicationBusContextImpl(datasource));
    }

    private RDBMSCoordinationStrategy(RDBMSCommunicationBusContextImpl communicationBusContext) {

        ClusterCoordinatorConfigurations clusterConfiguration = RDBMSCoordinationServiceHolder.
                getClusterConfiguration();
        if (clusterConfiguration != null) {
            StrategyConfig strategyConfiguration = clusterConfiguration.getStrategyConfig();
            if (strategyConfiguration != null) {
                this.heartBeatInterval = strategyConfiguration.getHeartbeatInterval();
                // Maximum age of a heartbeat. After this much of time, the heartbeat is considered invalid and node is
                // considered to have left the cluster.
                this.heartbeatMaxRetryInterval = heartBeatInterval * strategyConfiguration.getHeartbeatMaxRetry();
                this.localGroupId = clusterConfiguration.getGroupId();
            } else {
                throw new ClusterCoordinationException("Strategy Configurations not found in" +
                        " deployment.yaml, please check " + CoordinationPropertyNames.STRATEGY_CONFIG_NS + " under "
                        + CoordinationPropertyNames.CLUSTER_CONFIG_NS + " namespace configurations");
            }
        } else {
            throw new ClusterCoordinationException("Cluster Configurations not found in" +
                    " deployment.yaml, please check " + CoordinationPropertyNames.CLUSTER_CONFIG_NS +
                    " namespace configurations");
        }
        if (this.localGroupId == null) {
            throw new ClusterCoordinationException(
                    "Group Id not set in cluster.config in deployment.yaml configuration file");
        }
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat("RDBMSCoordinationStrategy-%d").build();
        this.threadExecutor = Executors.newSingleThreadExecutor(namedThreadFactory);
        try {
            ConfigProvider configProvider = RDBMSCoordinationServiceHolder.getConfigProvider();
            if (configProvider != null) {
                this.localNodeId = (String) ((Map) configProvider.getConfigurationObject("wso2.carbon")).get("id");
            } else {
                this.localNodeId = generateRandomId();
                log.warn("The id of the server has not been set in wso2.carbon namespace of deployment.yaml. " +
                        " auto-generated value of " + this.localNodeId + " will be used as the node Id.");
            }
        } catch (ConfigurationException e) {
            throw new ClusterCoordinationException("The id has not been set in wso2.carbon namespace under" +
                    " the id property.");
        }
        this.communicationBusContext = communicationBusContext;
        this.rdbmsMemberEventProcessor = new RDBMSMemberEventProcessor(localNodeId, localGroupId,
                heartbeatMaxRetryInterval, communicationBusContext);
    }

    @Override
    public List<NodeDetail> getAllNodeDetails() throws ClusterCoordinationException {
        List<NodeDetail> allNodeDetails = communicationBusContext.getAllNodeData(localGroupId);
        List<NodeDetail> liveNodeDetails = new ArrayList<>();
        for (NodeDetail nodeDetail : allNodeDetails) {
            long heartbeatAge = System.currentTimeMillis() - nodeDetail.getLastHeartbeat();
            if (heartbeatAge < heartbeatMaxRetryInterval) {
                liveNodeDetails.add(nodeDetail);
            }
        }
        return liveNodeDetails;
    }

    @Override
    public NodeDetail getLeaderNode() {
        List<NodeDetail> nodeDetails = communicationBusContext.getAllNodeData(localGroupId);
        for (NodeDetail nodeDetail : nodeDetails) {
            if (nodeDetail.isCoordinator()) {
                return nodeDetail;
            }
        }
        return null;
    }

    @Override
    public boolean isLeaderNode() throws ClusterCoordinationException {
        NodeDetail nodeDetail = communicationBusContext.getNodeData(localNodeId, localGroupId);
        if (nodeDetail == null) {
            return false;
        }
        return nodeDetail.isCoordinator();
    }

    @Override
    public void joinGroup() {
        joinGroup(null);
    }

    @Override
    public void joinGroup(Map<String, Object> propertiesMap) {
        //clear old membership events for the node
        communicationBusContext.clearMembershipEvents(localNodeId, localGroupId);
        NodeDetail nodeDetail = communicationBusContext.getNodeData(localNodeId, localGroupId);
        boolean isNodeExist = false;
        if (nodeDetail != null) {
            // This check is done to verify if the node details in the database are of an inactive node.
            // This check would fail if a node goes down and is restarted before the heart beat value expires.
            // Assumed that this case doesn't happen since node startup time is greater than hear beat value by default.
            // Restarting the server again so that heart beat values expires will solve this issue.
            long lastHeartBeat = nodeDetail.getLastHeartbeat();
            long currentTimeMillis = System.currentTimeMillis();
            long heartbeatAge = currentTimeMillis - lastHeartBeat;
            isNodeExist = (heartbeatAge < heartbeatMaxRetryInterval);
        }

        if (!isNodeExist) {
            this.threadExecutor.execute(new HeartBeatExecutionTask(propertiesMap));
        } else {
            throw new ClusterCoordinationException("Node with ID " + localNodeId + " in group " + localGroupId +
                    " already exists.");
        }
    }

    /**
     * This class will schedule and execute coordination tasks
     */
    public class HeartBeatExecutionTask implements Runnable {
        private CoordinatorElectionTask coordinatorElectionTask;
        private long currentHeartbeatTime = System.currentTimeMillis();
        public HeartBeatExecutionTask(Map<String, Object> propertiesMap) {
            coordinatorElectionTask = new CoordinatorElectionTask(localNodeId, localGroupId, propertiesMap);
        }
        @Override
        public void run() {
            while (true) {
                try {
                    coordinatorElectionTask.runCoordinationElectionTask(currentHeartbeatTime);
                    long taskEndedTime = System.currentTimeMillis();
                    long nextHeartbeatTime = currentHeartbeatTime + heartBeatInterval;
                    if (taskEndedTime - nextHeartbeatTime >= 0) {
                        log.warn(currentHeartbeatTime + " heartBeatInterval is " + heartBeatInterval +
                                ". But current Heartbeat is happening after " +
                                (taskEndedTime - currentHeartbeatTime) + " millis");
                    } else {
                        try {
                            Thread.sleep((nextHeartbeatTime - taskEndedTime));
                        } catch (InterruptedException e) {
                            log.error("Exception on HeartBeatExecutionTask thread sleep.");
                        }
                    }
                    currentHeartbeatTime = System.currentTimeMillis();
                } catch (Throwable t) {
                    throw new ClusterCoordinationException("Error occurred while performing coordinator tasks.", t);
                }
            }
        }
    }

    @Override
    public void registerEventListener(MemberEventListener memberEventListener) {
        // Register listener for membership changes
        memberEventListener.setGroupId(localGroupId);
        rdbmsMemberEventProcessor.addEventListener(memberEventListener);
    }

    @Override
    public void setPropertiesMap(Map<String, Object> propertiesMap) {
        communicationBusContext.updatePropertiesMap(localNodeId, localGroupId, propertiesMap);
    }

    public void stop() {
        this.threadExecutor.shutdown();
        this.rdbmsMemberEventProcessor.stop();
    }

    private String generateRandomId() {
        return UUID.randomUUID().toString();
    }

    /**
     * For each member, this class will run in a separate thread.
     */
    private class CoordinatorElectionTask {

        /**
         * Current state of the node.
         */
        private NodeState currentNodeState;

        /**
         * Used to uniquely identify a node in the cluster.
         */
        private String localNodeId;

        /**
         * Used to uniquely identify the group ID in the cluster.
         */
        private String localGroupId;

        /**
         * The unique property map of the node.
         */
        private Map<String, Object> localPropertiesMap;

        /**
         * Constructor.
         *
         * @param nodeId  node ID of the current node
         * @param groupId group ID of the current group
         */
        private CoordinatorElectionTask(String nodeId, String groupId, Map<String, Object> propertiesMap) {
            this.localGroupId = groupId;
            this.localNodeId = nodeId;
            this.localPropertiesMap = propertiesMap;
            this.currentNodeState = NodeState.MEMBER;
        }

        public void runCoordinationElectionTask (long currentHeartbeatTime) {
            try {
                if (log.isDebugEnabled()) {
                    log.debug("Current node state: " + currentNodeState);
                }
                long timeTakenForMemberTasks[] = new long[4];
                long timeTakenForCoordinatorTasks[] = new long[5];
                switch (currentNodeState) {
                    case MEMBER:
                        performMemberTask(currentHeartbeatTime, timeTakenForMemberTasks);
                        break;
                    case COORDINATOR:
                        performCoordinatorTask(currentHeartbeatTime, timeTakenForCoordinatorTasks);
                        break;
                }
                long clusterTaskEndingTime = System.currentTimeMillis();
                if (log.isDebugEnabled() && clusterTaskEndingTime - currentHeartbeatTime > 1000) {
                    log.debug(clusterTaskEndingTime + " Cluster task took " +
                            (clusterTaskEndingTime - currentHeartbeatTime) + " millis to complete on " +
                            currentNodeState + " node.");
                    switch (currentNodeState) {
                        case MEMBER:
                            log.debug(clusterTaskEndingTime +
                                    "\nupdateNodeHeartBeat(): " + timeTakenForMemberTasks[0] +
                                    "\ncheckIfCoordinatorValid(): " + timeTakenForMemberTasks[1] +
                                    "\nremoveCoordinator() if coordinator invalid: " + timeTakenForMemberTasks[2] +
                                    "\nperformElectionTask() if coordinator invalid: " + timeTakenForMemberTasks[3]);
                            break;
                        case COORDINATOR:
                            log.debug(clusterTaskEndingTime +
                                    "\nupdateCoordinatorHeartbeat(): " + timeTakenForCoordinatorTasks[0] +
                                    "\nupdateNodeHeartBeat() if still coordinator: " + timeTakenForCoordinatorTasks[1] +
                                    "\ngetAllNodeData() if still coordinator: " + timeTakenForCoordinatorTasks[2] +
                                    "\nfindAddedRemovedMembers() if still coordinator: " +
                                        timeTakenForCoordinatorTasks[3] +
                                    "\nperformElectionTask() if NOT still coordinator: " +
                                        timeTakenForCoordinatorTasks[4]);
                            break;
                    }
                }
                // We are catching throwable to avoid subsequent executions getting suppressed
            } catch (Throwable e) {
                log.error("Error detected while running coordination algorithm. Node became a "
                        + NodeState.MEMBER + " node in group " + localGroupId, e);
                currentNodeState = NodeState.MEMBER;
            }
        }

        /**
         * Perform periodic task that should be done by a MEMBER node.
         *
         * @throws ClusterCoordinationException
         * @throws InterruptedException
         */
        private void performMemberTask(long currentHeartbeatTime, long[] timeTakenForMemberTasks)
                throws ClusterCoordinationException, InterruptedException {
            long taskStartTime = System.currentTimeMillis();
            long taskEndTime;
            updateNodeHeartBeat(currentHeartbeatTime);
            taskEndTime = System.currentTimeMillis();
            timeTakenForMemberTasks[0] = taskEndTime - taskStartTime;
            taskStartTime = taskEndTime;
            boolean coordinatorValid = communicationBusContext.checkIfCoordinatorValid(localGroupId,
                    heartbeatMaxRetryInterval, currentHeartbeatTime);
            taskEndTime = System.currentTimeMillis();
            timeTakenForMemberTasks[1] = taskEndTime - taskStartTime;
            if (!coordinatorValid) {
                if (log.isDebugEnabled()) {
                    log.debug("Node ID :" + localNodeId
                            + " Going for election since the Coordinator is invalid for group ID: " + localGroupId);
                }
                taskStartTime = taskEndTime;
                communicationBusContext.removeCoordinator(localGroupId, heartbeatMaxRetryInterval,
                        currentHeartbeatTime);
                taskEndTime = System.currentTimeMillis();
                timeTakenForMemberTasks[2] = taskEndTime - taskStartTime;
                taskStartTime = taskEndTime;
                performElectionTask(currentHeartbeatTime);
                taskEndTime = System.currentTimeMillis();
                timeTakenForMemberTasks[3] = taskEndTime - taskStartTime;
            }
        }

        /**
         * Try to update the heart beat entry for local node in the DB. If the entry is deleted by the coordinator,
         * this will recreate the entry.
         *
         * @throws ClusterCoordinationException
         */
        private void updateNodeHeartBeat(long currentHeartbeatTime) throws ClusterCoordinationException {
            boolean heartbeatEntryExists = communicationBusContext.updateNodeHeartbeat(localNodeId, localGroupId,
                    currentHeartbeatTime);
            if (!heartbeatEntryExists) {
                communicationBusContext.createNodeHeartbeatEntry(localNodeId, localGroupId, localPropertiesMap);
            }
        }

        /**
         * Perform periodic task that should be done by a Coordinating node.
         *
         * @throws ClusterCoordinationException
         * @throws InterruptedException
         */
        private void performCoordinatorTask(long currentHeartbeatTime, long[] timeTakenForCoordinatorTasks)
                throws ClusterCoordinationException, InterruptedException {
            // Try to update the coordinator heartbeat
            long taskStartTime = System.currentTimeMillis();
            long taskEndTime;
            boolean stillCoordinator = communicationBusContext.
                    updateCoordinatorHeartbeat(localNodeId, localGroupId, currentHeartbeatTime);
            taskEndTime = System.currentTimeMillis();
            timeTakenForCoordinatorTasks[0] = taskEndTime - taskStartTime;
            taskStartTime = taskEndTime;
            if (stillCoordinator) {
                updateNodeHeartBeat(currentHeartbeatTime);
                taskEndTime = System.currentTimeMillis();
                timeTakenForCoordinatorTasks[1] = taskEndTime - taskStartTime;
                taskStartTime = taskEndTime;
                List<NodeDetail> allNodeInformation = communicationBusContext.getAllNodeData(localGroupId);
                taskEndTime = System.currentTimeMillis();
                timeTakenForCoordinatorTasks[2] = taskEndTime - taskStartTime;
                taskStartTime = taskEndTime;
                findAddedRemovedMembers(allNodeInformation, currentHeartbeatTime);
                taskEndTime = System.currentTimeMillis();
                timeTakenForCoordinatorTasks[3] = taskEndTime - taskStartTime;
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Going for election since Coordinator state is lost in group " + localGroupId);
                }
                performElectionTask(currentHeartbeatTime);
                taskEndTime = System.currentTimeMillis();
                timeTakenForCoordinatorTasks[4] = taskEndTime - taskStartTime;
            }

        }

        /**
         * Finds the newly added and removed nodes to the group.
         *
         * @param allNodeInformation all the node information of the group
         * @param currentTimeMillis  current timestamp
         */
        private void findAddedRemovedMembers(List<NodeDetail> allNodeInformation, long currentTimeMillis) {
            List<String> allActiveNodeIds = getNodeIds(allNodeInformation);
            List<NodeDetail> removedNodeDetails = new ArrayList<>();
            List<String> newNodes = new ArrayList<String>();
            List<String> removedNodes = new ArrayList<String>();
            for (NodeDetail nodeDetail : allNodeInformation) {
                long heartbeatAge = currentTimeMillis - nodeDetail.getLastHeartbeat();
                String nodeId = nodeDetail.getNodeId();
                if (heartbeatAge >= heartbeatMaxRetryInterval) {
                    removedNodes.add(nodeId);
                    allActiveNodeIds.remove(nodeId);
                    removedNodeDetails.add(nodeDetail);
                    communicationBusContext.removeNode(nodeId, localGroupId);
                } else if (nodeDetail.isNewNode()) {
                    newNodes.add(nodeId);
                    communicationBusContext.markNodeAsNotNew(nodeId, localGroupId);
                }
            }
            notifyAddedMembers(newNodes, allActiveNodeIds);
            notifyRemovedMembers(removedNodes, allActiveNodeIds, removedNodeDetails);
        }

        /**
         * Notifies the members in the group about the newly added nodes.
         *
         * @param newNodes         The list of newly added members to the group
         * @param allActiveNodeIds all the node IDs of the current group
         */
        private void notifyAddedMembers(List<String> newNodes, List<String> allActiveNodeIds) {
            for (String newNode : newNodes) {
                if (log.isDebugEnabled()) {
                    log.debug("Member added " + StringUtil.removeCRLFCharacters(newNode) + "to group " +
                            StringUtil.removeCRLFCharacters(localGroupId));
                }
                rdbmsMemberEventProcessor.notifyMembershipEvent(newNode, localGroupId, allActiveNodeIds,
                        MemberEventType.MEMBER_ADDED);
            }
        }

        /**
         * Stores the removed member detail in the database.
         *
         * @param allActiveNodeIds   all the node IDs of the current group
         * @param removedNodeDetails node details of the removed nodes
         */
        private void storeRemovedMemberDetails(List<String> allActiveNodeIds, List<NodeDetail> removedNodeDetails) {
            for (NodeDetail nodeDetail : removedNodeDetails) {
                communicationBusContext.insertRemovedNodeDetails(nodeDetail.getNodeId(), nodeDetail.getGroupId(),
                        allActiveNodeIds, nodeDetail.getPropertiesMap());
            }
        }

        /**
         * Notifies the members in the group about the removed nodes from the group.
         *
         * @param removedNodes     The list of removed membwes from the group
         * @param allActiveNodeIds all the node IDs of the current group
         */
        private void notifyRemovedMembers(List<String> removedNodes, List<String> allActiveNodeIds,
                                          List<NodeDetail> removedNodeDetails) {
            storeRemovedMemberDetails(allActiveNodeIds, removedNodeDetails);
            for (String removedNode : removedNodes) {
                if (log.isDebugEnabled()) {
                    log.debug("Member removed " + StringUtil.removeCRLFCharacters(removedNode) + "from group "
                            + StringUtil.removeCRLFCharacters(localGroupId));
                }
                rdbmsMemberEventProcessor.notifyMembershipEvent(removedNode, localGroupId, allActiveNodeIds,
                        MemberEventType.MEMBER_REMOVED);
            }
        }

        /**
         * Perform new coordinator election task.
         *
         * @throws InterruptedException
         */
        private void performElectionTask(long currentHeartbeatTime) throws InterruptedException {
            try {
                this.currentNodeState = tryToElectSelfAsCoordinator(currentHeartbeatTime);
            } catch (ClusterCoordinationException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Current node became a " + NodeState.MEMBER + " node in group "
                            + localGroupId, e);
                }
                this.currentNodeState = NodeState.MEMBER;
            }
        }

        /**
         * Try to elect local node as the coordinator by creating the coordinator entry.
         *
         * @return next NodeState
         * @throws ClusterCoordinationException
         * @throws InterruptedException
         */
        private NodeState tryToElectSelfAsCoordinator(long currentHeartbeatTime)
                throws ClusterCoordinationException, InterruptedException {
            NodeState nextState;
            boolean electedAsCoordinator = communicationBusContext.createCoordinatorEntry(localNodeId, localGroupId);
            if (electedAsCoordinator) {
                if (log.isDebugEnabled()) {
                    log.debug("Elected current node as the coordinator in group " + localGroupId);
                }
                List<NodeDetail> allNodeInformation = communicationBusContext.getAllNodeData(localGroupId);
                findAddedRemovedMembers(allNodeInformation, currentHeartbeatTime);
                nextState = NodeState.COORDINATOR;
                // notify nodes about coordinator change
                List<String> nodeIdentifiers = new ArrayList<>();
                for (NodeDetail nodeDetail : allNodeInformation) {
                    nodeIdentifiers.add(nodeDetail.getNodeId());
                }
                rdbmsMemberEventProcessor.notifyMembershipEvent(localNodeId, localGroupId, nodeIdentifiers,
                        MemberEventType.COORDINATOR_CHANGED);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Election resulted in current node becoming a " + NodeState.MEMBER
                            + " node in group " + localGroupId);
                }
                nextState = NodeState.MEMBER;
            }
            return nextState;
        }

        /**
         * Get the node IDs of the current group.
         *
         * @return node IDs of the current group
         * @throws ClusterCoordinationException
         */
        public List<String> getAllNodeIdentifiers() throws ClusterCoordinationException {
            List<NodeDetail> allNodeInformation = communicationBusContext.getAllNodeData(localGroupId);
            return getNodeIds(allNodeInformation);
        }

        /**
         * Return a list of node ids from the heartbeat data list.
         *
         * @param allHeartbeatData list of heartbeat data
         * @return list of node IDs
         */
        private List<String> getNodeIds(List<NodeDetail> allHeartbeatData) {
            List<String> allNodeIds = new ArrayList<String>(allHeartbeatData.size());
            for (NodeDetail nodeDetail : allHeartbeatData) {
                allNodeIds.add(nodeDetail.getNodeId());
            }
            return allNodeIds;
        }
    }
}
