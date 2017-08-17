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

package org.wso2.carbon.cluster.coordinator.commons.configs;

/**
 * POJO for RDBMS related configurations
 */
public class RdbmsConfig {

    /**
     * Datasource name config
     */
    private String dataSource;

    /**
     * Heartbeat interval config
     */
    private int heartbeatInterval;

    /**
     * Event polling interval config
     */
    private int eventPollingInterval;

    /**
     * Task thread count config
     */
    private int taskThreadCount;

    /**
     * Getter for dataSource
     */
    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Getter for heartbeatInterval
     */
    public int getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public void setHeartbeatInterval(int heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    /**
     * Getter for eventPollingInterval
     */
    public int getEventPollingInterval() {
        return eventPollingInterval;
    }

    public void setEventPollingInterval(int eventPollingInterval) {
        this.eventPollingInterval = eventPollingInterval;
    }

    /**
     * Getter for taskThreadCount
     */
    public int getTaskThreadCount() {
        return taskThreadCount;
    }

    public void setTaskThreadCount(int taskThreadCount) {
        this.taskThreadCount = taskThreadCount;
    }
}
