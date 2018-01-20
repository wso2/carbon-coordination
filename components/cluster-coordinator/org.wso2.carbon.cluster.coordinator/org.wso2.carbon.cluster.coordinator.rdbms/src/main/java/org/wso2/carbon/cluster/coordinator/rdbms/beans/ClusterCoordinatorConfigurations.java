/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.cluster.coordinator.rdbms.beans;

import org.wso2.carbon.config.annotation.Configuration;

/**
 * Bean class of carbon coordination properties
 */
@Configuration(namespace = "cluster.config", description = "Configurations for cluster coordination")
public class ClusterCoordinatorConfigurations {

    private boolean enabled = false;
    private String groupId;
    private String coordinationStrategyClass = "org.wso2.carbon.cluster.coordinator.rdbms.RDBMSCoordinationStrategy";
    private StrategyConfig strategyConfig;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getCoordinationStrategyClass() {
        return coordinationStrategyClass;
    }

    public void setCoordinationStrategyClass(String coordinationStrategyClass) {
        this.coordinationStrategyClass = coordinationStrategyClass;
    }

    public StrategyConfig getStrategyConfig() {
        return strategyConfig;
    }

    public void setStrategyConfig(StrategyConfig strategyConfig) {
        this.strategyConfig = strategyConfig;
    }
}
