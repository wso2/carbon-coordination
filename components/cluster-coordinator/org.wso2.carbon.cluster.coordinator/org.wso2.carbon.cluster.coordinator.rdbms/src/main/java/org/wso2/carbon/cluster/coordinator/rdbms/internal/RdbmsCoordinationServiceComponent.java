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
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.cluster.coordinator.commons.CoordinationStrategy;
import org.wso2.carbon.cluster.coordinator.rdbms.RDBMSCoordinationStrategy;
import org.wso2.carbon.ndatasource.core.DataSourceService;

/**
 * @scr.component name="org.wso2.carbon.cluster.coordinator.rdbms.internal.RdbmsCoordinationServiceComponent"
 * immediate="true"
 * @scr.reference name="config.context.service"
 * interface="org.wso2.carbon.ndatasource.core.DataSourceService"
 * cardinality="1..1" policy="dynamic"
 * bind="setDataSourceService"
 * unbind="unsetDataSourceService"
 */
@SuppressWarnings("unused")
public class RdbmsCoordinationServiceComponent {

    /**
     * Class logger
     */
    private final static Log logger = LogFactory.getLog(RdbmsCoordinationServiceComponent.class);

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
        RDBMSCoordinationStrategy rdbmsCoordinationStrategyService = null;
        try {
            rdbmsCoordinationStrategyService = new RDBMSCoordinationStrategy();
        } catch (Exception e) {
            logger.fatal("Error while initializing RDBMS coordination service", e);
        }
        serviceRegistration = context.getBundleContext().registerService(CoordinationStrategy.class.getName(),
                                                                         rdbmsCoordinationStrategyService,
                                                                         null);
        logger.debug("RDBMS coordination strategy service registered");
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

    protected void setDataSourceService(DataSourceService dataSourceService) {
        logger.debug("Datasource service set");
    }

    protected void unsetDataSourceService(DataSourceService dataSourceService) {
        logger.debug("Datasource context service unset");
    }
}
