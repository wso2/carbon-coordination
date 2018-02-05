/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.cluster.coordinator.rdbms.util;

/**
 * Class that holds constants to extract queries
 */
public class QueryConstants {

    private QueryConstants() {
    }

    public static final String IS_LEADER_STATUS_TABLE_EXISTS = "IS_LEADER_STATUS_TABLE_EXISTS";
    public static final String IS_CLUSTER_NODE_STATUS_TABLE_EXISTS = "IS_CLUSTER_NODE_STATUS_TABLE_EXISTS";
    public static final String IS_MEMBERSHIP_EVENT_TABLE_EXISTS = "IS_MEMBERSHIP_EVENT_TABLE_EXISTS";
    public static final String IS_REMOVED_MEMBERS_TABLE_EXISTS = "IS_REMOVED_MEMBERS_TABLE_EXISTS";
    public static final String CREATE_LEADER_STATUS_TABLE = "CREATE_LEADER_STATUS_TABLE";
    public static final String CREATE_CLUSTER_NODE_STATUS_TABLE = "CREATE_CLUSTER_NODE_STATUS_TABLE";
    public static final String CREATE_MEMBERSHIP_EVENT_TABLE = "CREATE_MEMBERSHIP_EVENT_TABLE";
    public static final String CREATE_REMOVED_MEMBERS_TABLE = "CREATE_REMOVED_MEMBERS_TABLE";
    public static final String GET_COORDINATOR_NODE_ID = "GET_COORDINATOR_NODE_ID";
    public static final String MARK_NODE_NOT_NEW = "MARK_NODE_NOT_NEW";
    public static final String INSERT_COORDINATOR_ROW = "INSERT_COORDINATOR_ROW";
    public static final String INSERT_NODE_HEARTBEAT_ROW = "INSERT_NODE_HEARTBEAT_ROW";
    public static final String UPDATE_PROPERTIES_MAP = "UPDATE_PROPERTIES_MAP";
    public static final String GET_COORDINATOR_ROW_FOR_NODE_ID = "GET_COORDINATOR_ROW_FOR_NODE_ID";
    public static final String GET_COORDINATOR_HEARTBEAT = "GET_COORDINATOR_HEARTBEAT";
    public static final String UPDATE_COORDINATOR_HEARTBEAT = "UPDATE_COORDINATOR_HEARTBEAT";
    public static final String UPDATE_NODE_HEARTBEAT = "UPDATE_NODE_HEARTBEAT";
    public static final String GET_ALL_NODE_HEARTBEAT = "GET_ALL_NODE_HEARTBEAT";
    public static final String GET_NODE_DATA = "GET_NODE_DATA";
    public static final String DELETE_COORDINATOR = "DELETE_COORDINATOR";
    public static final String DELETE_NODE_HEARTBEAT = "DELETE_NODE_HEARTBEAT";
    public static final String CLEAR_NODE_HEARTBEATS = "CLEAR_NODE_HEARTBEATS";
    public static final String CLEAR_COORDINATOR_HEARTBEAT = "CLEAR_COORDINATOR_HEARTBEAT";
    public static final String INSERT_MEMBERSHIP_EVENT = "INSERT_MEMBERSHIP_EVENT";
    public static final String INSERT_REMOVED_MEMBER_DETAILS = "INSERT_REMOVED_MEMBER_DETAILS";
    public static final String SELECT_MEMBERSHIP_EVENT = "SELECT_MEMBERSHIP_EVENT";
    public static final String SELECT_REMOVED_MEMBER_DETAILS = "SELECT_REMOVED_MEMBER_DETAILS";
    public static final String DELETE_REMOVED_MEMBER_DETAIL_FOR_NODE = "DELETE_REMOVED_MEMBER_DETAIL_FOR_NODE";
    public static final String CLEAR_ALL_MEMBERSHIP_EVENTS = "CLEAR_ALL_MEMBERSHIP_EVENTS";
    public static final String CLEAN_MEMBERSHIP_EVENTS_FOR_NODE = "CLEAN_MEMBERSHIP_EVENTS_FOR_NODE";

}
