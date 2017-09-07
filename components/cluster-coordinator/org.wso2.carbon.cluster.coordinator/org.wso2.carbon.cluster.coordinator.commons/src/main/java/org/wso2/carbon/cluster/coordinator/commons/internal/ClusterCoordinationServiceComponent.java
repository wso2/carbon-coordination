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
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.cluster.coordinator.commons.api.ClusterCoordinator;
import org.wso2.carbon.cluster.coordinator.commons.impl.ClusterCoordinatorImpl;
import org.wso2.carbon.cluster.coordinator.commons.CoordinationStrategy;

@Component(
        name = "org.wso2.carbon.cluster.coordinator.commons.internal.ClusterCoordinationServiceComponent",
        immediate = true,
        property = {
                "componentName=rdbms-coordination-service"
        }
)
public class ClusterCoordinationServiceComponent {

    private static final Log log = LogFactory.getLog(ClusterCoordinationServiceComponent.class);
    private ServiceRegistration<?> serviceRegistration;

    @Activate
    public void start(BundleContext bundleContext) throws Exception {
        ClusterCoordinatorImpl clusterCoordinator = null;
        CoordinationStrategy coordinationStrategy = ClusterCoordinationServiceDataHolder.getCoordinationStrategy();

        if (coordinationStrategy != null) {
            clusterCoordinator = new ClusterCoordinatorImpl(coordinationStrategy);
        } else {
            log.fatal("Coordination Strategy not found!");
        }

        serviceRegistration = bundleContext.registerService(ClusterCoordinator.class, clusterCoordinator, null);
        log.info("Cluster Coordinator Service Component Activated!");
    }

    @Deactivate
    public void stop(BundleContext bundleContext) throws Exception {
        serviceRegistration.unregister();
    }

    @Reference(
            name = "org.wso2.carbon.cluster.coordinator.commons.CoordinationStrategy",
            service = CoordinationStrategy.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unRegisterCoordinationStrategy"
    )
    protected void registerCoordinationStrategy(CoordinationStrategy coordinationStrategy) {
        ClusterCoordinationServiceDataHolder.setStrategy(coordinationStrategy);
    }

    protected void unRegisterCoordinationStrategy(CoordinationStrategy coordinationStrategy) {
        ClusterCoordinationServiceDataHolder.setStrategy(null);
    }
}
