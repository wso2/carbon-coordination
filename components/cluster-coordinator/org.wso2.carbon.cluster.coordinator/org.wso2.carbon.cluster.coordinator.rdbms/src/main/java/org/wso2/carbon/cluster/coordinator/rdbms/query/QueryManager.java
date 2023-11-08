/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.cluster.coordinator.rdbms.query;

import org.wso2.carbon.cluster.coordinator.commons.exception.ClusterCoordinationException;
import org.wso2.carbon.cluster.coordinator.rdbms.beans.ClusterCoordinatorConfigurations;
import org.wso2.carbon.cluster.coordinator.rdbms.beans.StrategyConfig;
import org.wso2.carbon.database.query.manager.QueryProvider;
import org.wso2.carbon.database.query.manager.config.Queries;
import org.wso2.carbon.database.query.manager.exception.QueryMappingNotAvailableException;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;
import org.yaml.snakeyaml.introspector.BeanAccess;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Holds the database queries.
 */
public class QueryManager {

    private Map<String, String> queries;
    private ClusterCoordinatorConfigurations deploymentConfigProvider;

    public QueryManager(String databaseType, String databaseVersion, ClusterCoordinatorConfigurations configProvider)
            throws QueryMappingNotAvailableException, IOException {
        deploymentConfigProvider = configProvider;
        queries = readConfigs(databaseType, databaseVersion);
    }

    private Map<String, String> readConfigs(String databaseType, String databaseVersion) throws IOException,
            QueryMappingNotAvailableException {
        try {
            StrategyConfig deploymentConfigurations = deploymentConfigProvider.getStrategyConfig();
            List<Queries> deploymentQueries = deploymentConfigurations.getQueries();
            List<Queries> componentQueries;
            URL url = this.getClass().getClassLoader().getResource("queries.yaml");
            if (url != null) {
                StrategyConfig componentConfigurations = readYamlContent(url.openStream());
                componentQueries = componentConfigurations.getQueries();
            } else {
                throw new ClusterCoordinationException("Unable to load queries.yaml file.");
            }
            queries = QueryProvider.mergeMapping(databaseType, databaseVersion, componentQueries, deploymentQueries);
        } catch (QueryMappingNotAvailableException e) {
            throw new QueryMappingNotAvailableException("Unable to load queries.", e);
        } catch (IOException e) {
            throw new IOException("Unable to load content from queries.yaml file.", e);
        }
        return queries;
    }

    public String getQuery(String key) {
        if (!queries.containsKey(key)) {
            throw new ClusterCoordinationException("Unable to find the configuration entry for the key: " + key);
        }
        return queries.get(key);
    }

    private StrategyConfig readYamlContent(InputStream yamlContent) {
        Yaml yaml = new Yaml(new CustomClassLoaderConstructor(StrategyConfig.class,
                StrategyConfig.class.getClassLoader(), new LoaderOptions()));
        yaml.setBeanAccess(BeanAccess.FIELD);
        return yaml.loadAs(yamlContent, StrategyConfig.class);
    }
}
