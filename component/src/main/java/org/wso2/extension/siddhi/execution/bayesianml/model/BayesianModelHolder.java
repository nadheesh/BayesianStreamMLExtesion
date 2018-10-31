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
package org.wso2.extension.siddhi.execution.bayesianml.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Data holder which keeps the instances of @{@link BaseModel}.
 */
public class BayesianModelHolder {
    private static final BayesianModelHolder instance = new BayesianModelHolder();

    /**
     * Key - name of the model.
     * Value - @{@link BaseModel}
     */
    private Map<String, BaseModel> bayesianModelMap;

    private BayesianModelHolder() {
        bayesianModelMap = new HashMap();
    }

    public static BayesianModelHolder getInstance() {
        return instance;
    }

    public Map<String, BaseModel> getBayesianModelMap() {
        return bayesianModelMap;
    }

    public void setBayesianModelMap(Map<String, BaseModel> modelsMap) {
        this.bayesianModelMap = modelsMap;
    }

    public BaseModel getBayesianModel(String name) {
        return bayesianModelMap.get(name);
    }

    public void deleteBayesianModel(String name) {
        bayesianModelMap.remove(name);
    }

    public void addBayesianModel(String name, BaseModel model) {
        bayesianModelMap.put(name, model);
    }

}
