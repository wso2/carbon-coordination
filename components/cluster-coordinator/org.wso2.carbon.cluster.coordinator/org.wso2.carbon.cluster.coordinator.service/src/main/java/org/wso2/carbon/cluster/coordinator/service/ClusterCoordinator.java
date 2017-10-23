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

package org.wso2.carbon.cluster.coordinator.service;

import org.wso2.carbon.cluster.coordinator.commons.CoordinationStrategy;
import org.wso2.carbon.cluster.coordinator.commons.MemberEventListener;
import org.wso2.carbon.cluster.coordinator.commons.node.NodeDetail;

import java.util.List;
import java.util.Map;

/**
 * The OSGI service class for the coordinator algorithm.
 */
public class ClusterCoordinator {

    private CoordinationStrategy coordinationStrategy;

    /**
     * Creates a new instance of the ClusterCoordinator class and adds the local node to the configured group.
     * @param coordinationStrategy an implementation of a coordination strategy
     */
    public ClusterCoordinator(CoordinationStrategy coordinationStrategy) {
        coordinationStrategy.joinGroup();
        this.coordinationStrategy = coordinationStrategy;
    }

    /**
     * Get the node details of the current cluster group.
     *
     * @return the node details of the current group
     */
    public List<NodeDetail> getAllNodeDetails() {
        return coordinationStrategy.getAllNodeDetails();
    }

    /**
     * Get the leader ID of the current group.
     *
     * @return The leader node object of the current group
     */
    public NodeDetail getLeaderNode() {
        return coordinationStrategy.getLeaderNode();
    }

    /**
     * Check if this node is the leader of the given group Id
     *
     * @return true if node is leader. False otherwise.
     */
    public boolean isLeaderNode() {
        return coordinationStrategy.isLeaderNode();
    }

    /**
     * Register an event listener as an instance of the MemberEventListener class. Therefore the node
     * events can be notified via the listener class.
     *
     * @param memberEventListener The listener which should listen to the group events
     */
    public void registerEventListener(MemberEventListener memberEventListener) {
        this.coordinationStrategy.registerEventListener(memberEventListener);
    }

    /**
     * Updates the properties map of the current node
     * @param propertiesMap the map of properties to be saved
     */
    public void setPropertiesMap(Map<String, Object> propertiesMap) {
        coordinationStrategy.setPropertiesMap(propertiesMap);
    }
}
