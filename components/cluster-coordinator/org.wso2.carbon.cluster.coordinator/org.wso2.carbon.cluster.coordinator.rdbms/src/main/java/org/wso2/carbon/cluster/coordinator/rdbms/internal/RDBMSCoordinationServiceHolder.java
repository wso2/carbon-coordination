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

package org.wso2.carbon.cluster.coordinator.rdbms.internal;

import org.wso2.carbon.config.provider.ConfigProvider;
import org.wso2.carbon.datasource.core.api.DataSourceService;

import java.util.Map;

/**
 * The service holder class for the cluster coordinator service.
 */
public class RDBMSCoordinationServiceHolder {

    private static DataSourceService dataSourceService;

    private static ConfigProvider configProvider;

    private static Map<String, Object> clusterConfiguration;

    public static DataSourceService getDataSourceService() {
        return dataSourceService;
    }

    public static void setDataSourceService(DataSourceService dataSourceService) {
        RDBMSCoordinationServiceHolder.dataSourceService = dataSourceService;
    }

    public static ConfigProvider getConfigProvider() {
        return configProvider;
    }

    public static void setConfigProvider(ConfigProvider configProvider) {
        RDBMSCoordinationServiceHolder.configProvider = configProvider;
    }

    public static Map<String, Object> getClusterConfiguration() {
        return clusterConfiguration;
    }

    public static void setClusterConfiguration(Map<String, Object> clusterConfiguration) {
        RDBMSCoordinationServiceHolder.clusterConfiguration = clusterConfiguration;
    }
}
