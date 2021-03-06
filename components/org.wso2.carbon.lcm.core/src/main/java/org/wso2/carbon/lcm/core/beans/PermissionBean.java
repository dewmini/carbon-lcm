/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.lcm.core.beans;

import java.util.ArrayList;
import java.util.List;

/**
 * This bean holds the data related to permissions which requires to perform the lifecycle state change operations.
 */
public class PermissionBean {

    private List<String> roles;
    private String forTarget;

    public PermissionBean() {
        this.roles = new ArrayList<String>();
        this.forTarget = "";
    }

    public String getForTarget() {
        return forTarget;
    }

    public void setForTarget(String forTarget) {
        this.forTarget = forTarget;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
}
