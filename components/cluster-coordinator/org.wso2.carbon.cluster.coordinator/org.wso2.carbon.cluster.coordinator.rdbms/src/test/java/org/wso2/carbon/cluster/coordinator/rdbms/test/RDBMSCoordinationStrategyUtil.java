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

package org.wso2.carbon.cluster.coordinator.rdbms.test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.wso2.carbon.cluster.coordinator.commons.configs.CoordinationPropertyNames;
import org.wso2.carbon.cluster.coordinator.commons.exception.ClusterCoordinationException;
import org.wso2.carbon.cluster.coordinator.rdbms.internal.RDBMSCoordinationServiceHolder;
import org.wso2.carbon.config.ConfigurationException;
import org.wso2.carbon.config.provider.ConfigProvider;
import org.wso2.carbon.config.provider.ConfigProviderImpl;
import org.wso2.carbon.config.reader.YAMLBasedConfigFileReader;
import org.wso2.carbon.secvault.internal.SecureVaultImpl;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import javax.sql.DataSource;

public class RDBMSCoordinationStrategyUtil {

    private static final Log log = LogFactory.getLog(RDBMSCoordinationStrategyUtil.class);
    private static final String DROP_LEADER_STATUS_TABLE = "DROP TABLE LEADER_STATUS_TABLE;";
    private static final String DROP_CLUSTER_NODE_STATUS_TABLE = "DROP TABLE CLUSTER_NODE_STATUS_TABLE;";
    private static final String DROP_MEMBERSHIP_EVENT_TABLE = "DROP TABLE MEMBERSHIP_EVENT_TABLE;";
    private static final String DROP_REMOVED_MEMBERS_TABLE = "DROP TABLE REMOVED_MEMBERS_TABLE;";
    private static final String CREATE_LEADER_STATUS_TABLE =
            "CREATE TABLE IF NOT EXISTS LEADER_STATUS_TABLE (\n"
                    + "                        GROUP_ID VARCHAR(512) NOT NULL,\n"
                    + "                        NODE_ID VARCHAR(512) NOT NULL,\n"
                    + "                        LAST_HEARTBEAT BIGINT NOT NULL,\n"
                    + "                        PRIMARY KEY (GROUP_ID)\n" + ");\n";
    private static final String CREATE_CLUSTER_NODE_STATUS_TABLE =
            "CREATE TABLE IF NOT EXISTS CLUSTER_NODE_STATUS_TABLE (\n"
                    + "                        GROUP_ID VARCHAR(512) NOT NULL,\n"
                    + "                        NODE_ID VARCHAR(512) NOT NULL,\n"
                    + "                        PROPERTY_MAP BLOB NOT NULL,\n"
                    + "                        IS_NEW_NODE INT NOT NULL,\n"
                    + "                        LAST_HEARTBEAT BIGINT NOT NULL,\n"
                    + "                        PRIMARY KEY (GROUP_ID,NODE_ID)\n" + ");\n";
    private static final String CREATE_MEMBERSHIP_EVENT_TABLE =
            "CREATE TABLE IF NOT EXISTS MEMBERSHIP_EVENT_TABLE (\n"
                    + "                        GROUP_ID VARCHAR(512) NOT NULL,\n"
                    + "                        NODE_ID VARCHAR(512) NOT NULL,\n"
                    + "                        CHANGE_TYPE INT NOT NULL,\n"
                    + "                        CHANGED_MEMBER_ID VARCHAR(512) NOT NULL\n" + ");\n";
    private static final String CREATE_REMOVED_MEMBERS_TABLE =
            "CREATE TABLE IF NOT EXISTS REMOVED_MEMBERS_TABLE (\n"
                    + "                        GROUP_ID VARCHAR(512) NOT NULL,\n"
                    + "                        NODE_ID VARCHAR(512) NOT NULL,\n"
                    + "                        PROPERTY_MAP BLOB NOT NULL,\n"
                    + "                        REMOVED_MEMBER_ID VARCHAR(512) NOT NULL\n" + ");\n";
    private static final String CLEAR_LEADER_STATUS_TABLE = "DELETE FROM LEADER_STATUS_TABLE;";
    private static final String CLEAR_CLUSTER_NODE_STATUS_TABLE = "DELETE FROM CLUSTER_NODE_STATUS_TABLE;";
    private static final String CLEAR_MEMBERSHIP_EVENT_TABLE = "DELETE FROM MEMBERSHIP_EVENT_TABLE;";
    private static final String CLEAR_REMOVED_MEMBERS_TABLE = "DELETE FROM REMOVED_MEMBERS_TABLE;";
    public static DataSource dataSource;

    static void init(String deploymentFile, String databaseName) {
        Path deploymentPath = null;
        try {
            deploymentPath = Paths.get(ClassLoader.getSystemResource(deploymentFile).toURI());
        } catch (URISyntaxException e) {
            log.error("The URI for " + deploymentFile + " in invalid", e);
        }
        ConfigProvider configProvider = new ConfigProviderImpl(new YAMLBasedConfigFileReader(deploymentPath),
                new SecureVaultImpl());

        try {
            Map clusterConfig = (Map) configProvider.
                    getConfigurationObject(CoordinationPropertyNames.CLUSTER_CONFIG_NS);
            RDBMSCoordinationServiceHolder.setClusterConfiguration(clusterConfig);
        } catch (ConfigurationException e) {
            log.error("Configuration file " + deploymentFile + " not found in resources folder " + e);
        }
        try {
            Class.forName("org.h2.Driver");
            JdbcDataSource h2DataSource = new JdbcDataSource();
            h2DataSource.setURL("jdbc:h2:./target/ANALYTICS_EVENT_STORE" + databaseName +
                    ";DB_CLOSE_ON_EXIT=FALSE;LOCK_TIMEOUT=60000;AUTO_SERVER=true");
            h2DataSource.setUser("wso2carbon");
            h2DataSource.setPassword("wso2carbon");
            dataSource = h2DataSource;
            createTables();
            clearTables();
        } catch (ClassNotFoundException e) {
            throw new ClusterCoordinationException("Error while initializing database", e);
        }
    }

    private static void clearTables() {
        executeQuery(CLEAR_LEADER_STATUS_TABLE);
        executeQuery(CLEAR_CLUSTER_NODE_STATUS_TABLE);
        executeQuery(CLEAR_MEMBERSHIP_EVENT_TABLE);
        executeQuery(CLEAR_REMOVED_MEMBERS_TABLE);
    }

    private static void createTables() {
        executeQuery(CREATE_LEADER_STATUS_TABLE);
        executeQuery(CREATE_CLUSTER_NODE_STATUS_TABLE);
        executeQuery(CREATE_MEMBERSHIP_EVENT_TABLE);
        executeQuery(CREATE_REMOVED_MEMBERS_TABLE);
    }

    public static void dropTables() {
        executeQuery(DROP_LEADER_STATUS_TABLE);
        executeQuery(DROP_CLUSTER_NODE_STATUS_TABLE);
        executeQuery(DROP_MEMBERSHIP_EVENT_TABLE);
        executeQuery(DROP_REMOVED_MEMBERS_TABLE);
    }

    private static void executeQuery(String query) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.execute();
            connection.commit();
            connection.close();
        } catch (SQLException e) {
            throw new ClusterCoordinationException("Error while executing query", e);
        }
    }
}
