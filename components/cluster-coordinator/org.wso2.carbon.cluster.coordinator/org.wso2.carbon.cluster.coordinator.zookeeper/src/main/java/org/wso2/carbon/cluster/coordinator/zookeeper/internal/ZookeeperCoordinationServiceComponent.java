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

package org.wso2.carbon.cluster.coordinator.zookeeper.internal;

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
import org.wso2.carbon.cluster.coordinator.zookeeper.ZookeeperCoordinationStrategy;
import org.wso2.carbon.cluster.coordinator.zookeeper.exception.ZookeeperCoordinationConfigurationException;
import org.wso2.carbon.config.ConfigurationException;
import org.wso2.carbon.config.provider.ConfigProvider;

import java.io.IOException;
import java.util.Map;

/**
 * Zookeeper cluster coordinator data service.
 */
@Component(
        name = "org.wso2.carbon.cluster.coordinator.zookeeper.internal.ZookeeperCoordinationServiceComponent",
        immediate = true
)
public class ZookeeperCoordinationServiceComponent {

    private static final Log log = LogFactory.getLog(ZookeeperCoordinationServiceComponent.class);

    /**
     * Config provider to read the configuration file
     */
    private Map clusterConfiguration;

    /**
     * This is the activation method of ZookeeperCoordinationServiceComponent. This will be called when its references
     * are satisfied.
     *
     * @param bundleContext the bundle context instance of this bundle.
     */
    @Activate
    protected void start(BundleContext bundleContext) {

        log.info("Getting the Coordination Strategy");

        Map clusterConfiguration = null;
        try {
            clusterConfiguration = (Map) ZookeeperCoordinationServiceHolder.getConfigProvider().
                    getConfigurationObject(CoordinationPropertyNames.CLUSTER_CONFIG_NS);
            ZookeeperCoordinationServiceHolder.setClusterConfiguration(clusterConfiguration);
        } catch (ConfigurationException e) {
            throw new ClusterCoordinationException("Configurations for cluster coordination is not " +
                    "available in deployment.yaml", e);
        }
        if ((Boolean) clusterConfiguration.get(CoordinationPropertyNames.ENABLED_PROPERTY) &&
                clusterConfiguration.get(CoordinationPropertyNames.COORDINATION_STRATEGY_CLASS_PROPERTY).
                        equals(ZookeeperCoordinationStrategy.class.getCanonicalName())) {
            ZookeeperCoordinationStrategy zookeeperCoordinationStrategy = null;

            try {
                zookeeperCoordinationStrategy = new ZookeeperCoordinationStrategy();
            } catch (IOException e) {
                throw new ZookeeperCoordinationConfigurationException("Zookeeper Service can not be found", e);
            }

            bundleContext.registerService(CoordinationStrategy.class, zookeeperCoordinationStrategy, null);

            log.info("Zookeeper Coordination Service Component Activated!");
        } else {
            log.warn("Cluster Coordination using ZookeeperCoordinationStrategy has been disabled." +
                    " Enable it in deployment.yaml or remove" +
                    " org.wso2.carbon.cluster.coordinator.zookeeper.jar from the lib folder");
        }
    }

    /**
     * This is the deactivation method of ZookeeperCoordinationServiceComponent. This will be called when this component
     * is being stopped or references are satisfied during runtime.
     *
     * @throws Exception this will be thrown if an issue occurs while executing the de-activate method
     */
    @Deactivate
    protected void stop() throws Exception {
        ZookeeperCoordinationServiceHolder.setClusterConfiguration(null);
    }

    @Reference(
            name = "carbon.config.provider",
            service = ConfigProvider.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unregisterConfigProvider"
    )
    protected void registerConfigProvider(ConfigProvider configProvider) throws ConfigurationException {
        ZookeeperCoordinationServiceHolder.setConfigProvider(configProvider);
    }

    protected void unregisterConfigProvider(ConfigProvider configProvider) {
        this.clusterConfiguration = null;
    }

}
