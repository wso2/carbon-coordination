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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.cluster.coordinator.commons.ClusterCoordinator;
import org.wso2.carbon.cluster.coordinator.commons.CoordinationStrategy;

/**
 * @scr.component name="org.wso2.carbon.cluster.coordinator.commons.internal.ClusterCoordinationServiceComponent"
 * immediate="true"
 * @scr.reference name="org.wso2.carbon.cluster.coordinator.commons.CoordinationStrategy"
 * interface="org.wso2.carbon.cluster.coordinator.commons.CoordinationStrategy"
 * cardinality="1..1"
 * policy="dynamic"
 * bind="setCoordinationStrategy"
 * unbind="unsetCoordinationStrategy"
 */
public class ClusterCoordinationServiceComponent {

    /**
     * Class logger
     */
    private final static Log logger = LogFactory.getLog(ClusterCoordinationServiceComponent.class);

    /**
     * Hold reference to service after registration
     */
    private ServiceRegistration<?> serviceRegistration;

    /**
     * Called when component is activated in the OSGI environment
     *
     * @param context component context
     */
    protected void activate(ComponentContext context) {
        ClusterCoordinator clusterCoordinator = null;
        CoordinationStrategy coordinationStrategy = ClusterCoordinationServiceDataHolder.getInstance()
                                                                                        .getCoordinationStrategy();
        if (coordinationStrategy != null) {
            clusterCoordinator = new ClusterCoordinator(coordinationStrategy);
        } else {
            logger.fatal("Cannot initialize cluster coordination service without strategy implementation");
        }

        serviceRegistration = context.getBundleContext().registerService(ClusterCoordinator.class.getName(),
                                                                         clusterCoordinator,
                                                                         null);
        logger.debug("Cluster coordination service registered");
    }

    /**
     * Called when component is deactivated in the OSGI environment
     *
     * @param context component context
     */
    protected void deactivate(ComponentContext context) {
        serviceRegistration.unregister();
        logger.debug("Cluster coordination service unregistered");
    }

    /**
     * Called after Coordination strategy service become available
     *
     * @param strategy Coordination strategy implementation
     */
    protected void setCoordinationStrategy(CoordinationStrategy strategy) {
        logger.debug("Setting CoordinationStrategy from service");
        ClusterCoordinationServiceDataHolder.getInstance().setStrategy(strategy);
    }

    /**
     * Called after Coordination strategy service become unavailable
     *
     * @param strategy Coordination strategy implementation
     */
    protected void unsetCoordinationStrategy(CoordinationStrategy strategy) {
        logger.debug("Unsetting CoordinationStrategy");
        ClusterCoordinationServiceDataHolder.getInstance().setStrategy(null);
    }
}
