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

import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.carbon.cluster.coordinator.rdbms.RDBMSCoordinationStrategy;

import java.io.File;

public class CoordinatorConfigEmptyValuesTestCase {

    @Test(expectedExceptions = RuntimeException.class)
    public void testEmptyValues() throws InterruptedException {
        RDBMSCoordinationStrategyUtil.init("conf" + File.separator + "deploymentFive.yaml", "dbFive");
        RDBMSCoordinationStrategy rdbmsCoordinationStrategyNodeOne = new RDBMSCoordinationStrategy(
                RDBMSCoordinationStrategyUtil.dataSource);
        rdbmsCoordinationStrategyNodeOne.joinGroup();
        Assert.assertEquals(rdbmsCoordinationStrategyNodeOne.getAllNodeDetails().size(), 1);
    }
}
