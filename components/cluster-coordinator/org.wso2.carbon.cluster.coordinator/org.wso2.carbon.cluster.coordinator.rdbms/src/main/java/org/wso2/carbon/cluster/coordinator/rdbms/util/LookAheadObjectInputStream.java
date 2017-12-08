/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
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

package org.wso2.carbon.cluster.coordinator.rdbms.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.Map;

/**
 * Hardened implementation of {@link ObjectInputStream} to deserialize only Map classes to be used for properties
 */
public class LookAheadObjectInputStream extends ObjectInputStream {

    public LookAheadObjectInputStream(InputStream inputStream) throws IOException {
        super(inputStream);
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException,
            ClassNotFoundException {
        try {
            if (!(Class.forName(desc.getName()).newInstance() instanceof Map)) {
                throw new InvalidClassException("Unauthorized deserialization attempt ", desc.getName());
            }
        } catch (InstantiationException | IllegalAccessException e) {
            throw new InvalidClassException("Invalid class of type " + desc.getName() +
                    " used for cluster properties. Use an instance of java.util.Map");
        }
        return super.resolveClass(desc);
    }
}
