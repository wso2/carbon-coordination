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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.cluster.coordinator.commons.CoordinationStrategy;
import org.wso2.carbon.cluster.coordinator.commons.configs.CoordinationPropertyNames;
import org.wso2.carbon.cluster.coordinator.commons.exception.ClusterCoordinationException;
import org.wso2.carbon.cluster.coordinator.rdbms.RDBMSCoordinationStrategy;
import org.wso2.carbon.datasource.core.api.DataSourceService;
import org.wso2.carbon.kernel.configprovider.CarbonConfigurationException;
import org.wso2.carbon.kernel.configprovider.ConfigProvider;


import java.util.Map;

/**
 * RDBMS cluster coordinator data service.
 */
@Component(
        name = "org.wso2.carbon.cluster.coordinator.rdbms.internal.RDBMSCoordinationServiceComponent",
        immediate = true
)
public class RDBMSCoordinationServiceComponent {

    private static final Log log = LogFactory.getLog(RDBMSCoordinationServiceComponent.class);

    /**
     * This is the activation method of RDBMSCoordinationServiceComponent. This will be called when its references are
     * satisfied.
     *
     * @param bundleContext the bundle context instance of this bundle.
     */
    @Activate
    protected void start(BundleContext bundleContext) {

        Map<String, Object> clusterConfiguration;
        try {
            clusterConfiguration = RDBMSCoordinationServiceHolder.getConfigProvider().
                    getConfigurationMap(CoordinationPropertyNames.CLUSTER_CONFIG_NS);
            if (clusterConfiguration != null) {
                RDBMSCoordinationServiceHolder.setClusterConfiguration(clusterConfiguration);
            } else {
                throw new ClusterCoordinationException("Configurations for cluster coordination is not " +
                        "available in deployment.yaml");
            }
        } catch (CarbonConfigurationException e) {
            throw new ClusterCoordinationException("Error in reading the cluster coordination configurations " +
                    "from deployment.yaml");
        }
        if ((Boolean) clusterConfiguration.get(CoordinationPropertyNames.ENABLED_PROPERTY)) {
            if (clusterConfiguration.get(CoordinationPropertyNames.COORDINATION_STRATEGY_CLASS_PROPERTY).
                    equals(RDBMSCoordinationStrategy.class.getCanonicalName())) {

                bundleContext.registerService(CoordinationStrategy.class, new RDBMSCoordinationStrategy(), null);
                log.info("RDBMS Coordination Service Component Activated");
            } else {
                log.warn("No such coordination strategy service found: " +
                        clusterConfiguration.get(CoordinationPropertyNames.COORDINATION_STRATEGY_CLASS_PROPERTY));
            }
        } else {
            log.warn("Cluster Coordination has been disabled. Enable it in deployment.yaml " +
                    "to use the clustering service.");
        }
    }

    /**
     * This is the deactivation method of RDBMSCoordinationServiceComponent. This will be called when this component
     * is being stopped or references are satisfied during runtime.
     */
    @Deactivate
    protected void stop() {
        RDBMSCoordinationServiceHolder.setClusterConfiguration(null);
    }

    @Reference(
            name = "carbon.config.provider",
            service = ConfigProvider.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unregisterConfigProvider"
    )
    protected void registerConfigProvider(ConfigProvider configProvider) throws CarbonConfigurationException {
        RDBMSCoordinationServiceHolder.setConfigProvider(configProvider);
    }

    protected void unregisterConfigProvider(ConfigProvider configProvider) {
        RDBMSCoordinationServiceHolder.setConfigProvider(null);
    }

    @Reference(
            name = "org.wso2.carbon.datasource.DataSourceService",
            service = DataSourceService.class,
            cardinality = ReferenceCardinality.AT_LEAST_ONE,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unregisterDataSourceListener"
    )
    protected void registerDataSourceListener(DataSourceService dataSourceService) {
        RDBMSCoordinationServiceHolder.setDataSourceService(dataSourceService);
    }

    protected void unregisterDataSourceListener(DataSourceService dataSourceService) {
        RDBMSCoordinationServiceHolder.setDataSourceService(null);
    }
}
