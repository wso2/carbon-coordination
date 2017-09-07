/*
 * Copyright (c) 2016, WSO2 Inc. (http://wso2.com) All Rights Reserved.
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
import org.wso2.carbon.cluster.coordinator.zookeeper.ZookeeperCoordinationStrategy;
import org.wso2.carbon.kernel.configprovider.CarbonConfigurationException;
import org.wso2.carbon.kernel.configprovider.ConfigProvider;

import java.util.Map;

/**
 * RDBMS cluster coordinator data service.
 */
@Component(
        name = "org.wso2.carbon.cluster.coordinator.rdbms.internal.ZookeeperCoordinationServiceComponent",
        immediate = true
//        property = {"componentName=rdbms-coordination-service"}
)
public class ZookeeperCoordinationServiceComponent {

    private static final Log log = LogFactory.getLog(ZookeeperCoordinationServiceComponent.class);

    /**
     * Config provider to read the configuration file
     */
    private Map clusterConfiguration;

    /**
     * This is the activation method of ZookeeperCoordinationServiceComponent. This will be called when its references are
     * satisfied.
     *
     * @param bundleContext the bundle context instance of this bundle.
     * @throws Exception this will be thrown if an issue occurs while executing the activate method
     */
    @Activate
    protected void start(BundleContext bundleContext) throws Exception {

        log.info("Getting the Coordination Strategy");

        if (clusterConfiguration.get("mode").equals("ha") &&
                clusterConfiguration.get("coordination.strategy.class").equals("RDBMSCoordinationStrategy")) {
            ZookeeperCoordinationStrategy zookeeperCoordinationStrategy = null;

            zookeeperCoordinationStrategy = new ZookeeperCoordinationStrategy();

            bundleContext.registerService(CoordinationStrategy.class, zookeeperCoordinationStrategy, null);

            log.info("Zookeeper Coordination Service Component Activated!");
        } else {
            log.info("Zookeeper Coordination has not been enabled");
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

    }

    @Reference(
            name = "carbon.config.provider",
            service = ConfigProvider.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unregisterConfigProvider"
    )
    protected void registerConfigProvider(ConfigProvider configProvider) throws CarbonConfigurationException {
        Map clusterConfiguration = configProvider.getConfigurationMap("cluster.config");
        if (clusterConfiguration != null && (boolean) clusterConfiguration.get("enabled")) {
            this.clusterConfiguration = clusterConfiguration;
        } else {
            log.info("Clustering has not been enabled. Zookeeper clustering will not be checked");
        }
    }

    protected void unregisterConfigProvider(ConfigProvider configProvider) {
        this.clusterConfiguration = null;
    }

}
