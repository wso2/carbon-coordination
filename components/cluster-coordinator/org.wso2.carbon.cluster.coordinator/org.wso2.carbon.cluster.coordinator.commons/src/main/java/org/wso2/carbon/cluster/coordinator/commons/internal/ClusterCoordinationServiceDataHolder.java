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

package org.wso2.carbon.cluster.coordinator.commons.internal;

import org.wso2.carbon.cluster.coordinator.commons.CoordinationStrategy;

/**
 * Data holder class for Cluster coordination service
 */
public class ClusterCoordinationServiceDataHolder {

    /**
     * Singleton instance field
     */
    private static final ClusterCoordinationServiceDataHolder instance = new ClusterCoordinationServiceDataHolder();

    /**
     * Coordination strategy implementation available in the OSGI environment
     */
    private CoordinationStrategy coordinationStrategy;

    /**
     * Get the singleton instance of the class
     *
     * @return Singleton instance
     */
    public static ClusterCoordinationServiceDataHolder getInstance() {
        return instance;
    }

    /**
     * Getter for coordinationStrategy
     */
    CoordinationStrategy getCoordinationStrategy() {
        return coordinationStrategy;
    }

    /**
     * Setter for coordinationStrategy
     */
    void setStrategy(CoordinationStrategy strategy) {
        coordinationStrategy = strategy;
    }
}
