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
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.cluster.coordinator.commons.node.NodeDetail;
import org.wso2.carbon.cluster.coordinator.rdbms.RDBMSCoordinationStrategy;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CoordinationEventFlowTestCase extends RDBMSCoordinationStratergyBaseTest {
    RDBMSCoordinationStrategy rdbmsCoordinationStrategyNodeOne;
    RDBMSCoordinationStrategy rdbmsCoordinationStrategyNodeTwo;
    RDBMSCoordinationStrategy rdbmsCoordinationStrategyNodeThree;
    EventListener eventListener;

    private static final Log log = LogFactory.getLog(CoordinationEventFlowTestCase.class);

    @BeforeClass
    public void initialize() throws InterruptedException, FileNotFoundException {
        init();
        rdbmsCoordinationStrategyNodeOne = new RDBMSCoordinationStrategy(dataSource);
        rdbmsCoordinationStrategyNodeTwo = new RDBMSCoordinationStrategy(dataSource);
        rdbmsCoordinationStrategyNodeThree = new RDBMSCoordinationStrategy(dataSource);
        eventListener = new EventListener();
    }

    @Test
    public void testMemberJoined() throws InterruptedException {
        Map<String, Object> nodeOnePropertyMap = new HashMap<>();
        nodeOnePropertyMap.put("id", "node1");
        rdbmsCoordinationStrategyNodeOne.joinGroup();
        Thread.sleep(100);
        rdbmsCoordinationStrategyNodeOne.setPropertiesMap(nodeOnePropertyMap);
        eventListener.setGroupId("testGroupOne");
        rdbmsCoordinationStrategyNodeOne.registerEventListener(eventListener);

    }

    @Test(dependsOnMethods = {"testMemberJoined"})
    public void testCoordinatorElected() throws InterruptedException {
        String leaderId = null;
        int count = 0;
        boolean coordinatorIdentified = false;
        while (count < 10) {
            NodeDetail leaderNodeDetail = rdbmsCoordinationStrategyNodeOne.getLeaderNode();
            if (leaderNodeDetail != null) {
                leaderId = (String) leaderNodeDetail.getPropertiesMap().get("id");
                if (leaderId.equals("node1")) {
                    coordinatorIdentified = true;
                    break;
                }
            }

            Thread.sleep(2000);
            count++;
        }
        Assert.assertTrue(coordinatorIdentified, "Coordinator was not elected in group");
    }

    @Test(dependsOnMethods = {"testCoordinatorElected"})
    public void testMultipleMemberJoined() throws InterruptedException {
        Map<String, Object> nodeTwoPropertyMap = new HashMap<>();
        nodeTwoPropertyMap.put("id", "node2");
        rdbmsCoordinationStrategyNodeTwo.joinGroup();
        Thread.sleep(100);
        rdbmsCoordinationStrategyNodeTwo.setPropertiesMap(nodeTwoPropertyMap);
        Map<String, Object> nodeThreePropertyMap = new HashMap<>();
        nodeThreePropertyMap.put("id", "node3");
        rdbmsCoordinationStrategyNodeThree.joinGroup();
        Thread.sleep(500);
        rdbmsCoordinationStrategyNodeThree.setPropertiesMap(nodeThreePropertyMap);

        int count = 0;
        boolean membersJoined = false;
        while (count < 10) {
            if (rdbmsCoordinationStrategyNodeOne.getAllNodeDetails().size() == 3) {
                membersJoined = true;
                break;
            }
            Thread.sleep(2000);
            count++;
        }
        Assert.assertTrue(membersJoined, "Multiple members were not joined to group");
    }

    @Test(dependsOnMethods = {"testMultipleMemberJoined"})
    public void testMemberAddedEventReceived() throws InterruptedException {
        int count = 0;
        boolean eventReceived = false;
        while (count < 10) {
            if (eventListener.memberAdded.size() == 3) {
                eventReceived = true;
                break;
            }
            Thread.sleep(2000);
            count++;
        }
        Assert.assertTrue(eventReceived, "Member added event not received.");
    }

    @Test(dependsOnMethods = {"testMemberAddedEventReceived"})
    public void testMemberRemoved() throws InterruptedException {
        rdbmsCoordinationStrategyNodeTwo.stop();
        int count = 0;
        boolean membersRemoved = false;
        while (count < 10) {
            if (rdbmsCoordinationStrategyNodeOne.getAllNodeDetails().size() == 2) {
                membersRemoved = true;
                break;
            }
            Thread.sleep(2000);
            count++;
        }
        Assert.assertTrue(membersRemoved, "Member not removed from group");
    }

    @Test(dependsOnMethods = {"testMemberRemoved"})
    public void testMemberRemovedEventReceived() throws InterruptedException {
        int count = 0;
        boolean eventReceived = false;
        while (count < 10) {
            if (eventListener.memberRemoved.size() == 1) {
                eventReceived = true;
                break;
            }
            Thread.sleep(2000);
            count++;
        }
        Assert.assertTrue(eventReceived, "Member removed event not received.");
    }

    @Test(dependsOnMethods = {"testMemberRemovedEventReceived"})
    public void testCoordinatorChanged() throws InterruptedException {
        int count;
        String leaderId = null;
        RDBMSCoordinationStrategy rdbmsCoordinationStrategyNodeFour = new RDBMSCoordinationStrategy(dataSource);
        Map<String, Object> nodeFourPropertyMap = new HashMap<>();
        nodeFourPropertyMap.put("id", "node4");
        rdbmsCoordinationStrategyNodeFour.joinGroup();
        Thread.sleep(500);
        rdbmsCoordinationStrategyNodeFour.setPropertiesMap(nodeFourPropertyMap);
        EventListener eventListener = new EventListener();
        eventListener.setGroupId("testGroupOne");
        rdbmsCoordinationStrategyNodeFour.registerEventListener(eventListener);
        boolean coordinatorChanged = false;
        count = 0;

        // Wait for cluster to stabilize
        TimeUnit.SECONDS.sleep(1);
        rdbmsCoordinationStrategyNodeOne.stop();

        while (count < 10) {
            NodeDetail leaderNodeDetail = rdbmsCoordinationStrategyNodeThree.getLeaderNode();
            if (leaderNodeDetail != null) {
                leaderId = (String) leaderNodeDetail.getPropertiesMap().get("id");
            } else {
                leaderId = "";
            }
            if (leaderId.equals("node3") || leaderId.equals("node4")) {
                coordinatorChanged = true;
                break;
            }
            Thread.sleep(2000);
            count++;
        }

        Assert.assertTrue(coordinatorChanged, "Coordinator not changed");
        count = 0;
        boolean coordinatorEventReceived = false;
        while (count < 10) {
            if (eventListener.coordinatorChanged.size() == 1) {
                coordinatorEventReceived = true;
                break;
            }
            Thread.sleep(2000);
            count++;
        }
        Assert.assertTrue(coordinatorEventReceived, "Coordinator changed event not received.");
    }
}
