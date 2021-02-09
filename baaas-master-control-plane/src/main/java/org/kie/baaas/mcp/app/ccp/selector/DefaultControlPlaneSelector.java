/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.kie.baaas.mcp.app.ccp.selector;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.kie.baaas.mcp.app.ccp.ClusterControlPlaneSelector;
import org.kie.baaas.mcp.app.dao.ClusterControlPlaneDAO;
import org.kie.baaas.mcp.app.model.ClusterControlPlane;
import org.kie.baaas.mcp.app.model.Decision;

/**
 * Determines which ClusterControlPlane we will deploy the given Decision to.
 */
@ApplicationScoped
public class DefaultControlPlaneSelector implements ClusterControlPlaneSelector {

    private final ClusterControlPlaneDAO controlPlaneDAO;

    @Inject
    public DefaultControlPlaneSelector(ClusterControlPlaneDAO controlPlaneDAO) {
        this.controlPlaneDAO = controlPlaneDAO;
    }

    public ClusterControlPlane selectControlPlaneForDeployment(Decision decision) {
        return controlPlaneDAO.findOne();
    }
}
