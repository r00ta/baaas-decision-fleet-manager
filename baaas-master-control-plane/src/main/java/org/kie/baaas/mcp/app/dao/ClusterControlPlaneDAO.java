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

package org.kie.baaas.mcp.app.dao;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.kie.baaas.mcp.app.config.MasterControlPlaneConfig;
import org.kie.baaas.mcp.app.model.ClusterControlPlane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DAO Implementation for working with ClusterControlPlane instances.
 */
@ApplicationScoped
@Transactional
public class ClusterControlPlaneDAO {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterControlPlaneDAO.class);

    private static final int DEFAULT_CCP_ID = 1;

    private final MasterControlPlaneConfig controlPlaneConfig;

    private final EntityManager em;

    @Inject
    public ClusterControlPlaneDAO(MasterControlPlaneConfig controlPlaneConfig, EntityManager em) {

        Objects.requireNonNull(controlPlaneConfig, "controlPlaneConfig cannot be null");
        Objects.requireNonNull(controlPlaneConfig, "em cannot be null");

        this.controlPlaneConfig = controlPlaneConfig;
        this.em = em;
    }

    public void init() {
        ClusterControlPlane ccp = em.find(ClusterControlPlane.class, DEFAULT_CCP_ID);
        if (ccp == null) {
            ccp = new ClusterControlPlane();
            ccp.setId(DEFAULT_CCP_ID);
        }

        LOGGER.info("Registering Cluster Control Plane. Kubernetes API URL: '{}'. DMN JIT URL: '{}'.", controlPlaneConfig.getCcpKubernetesApiUrl(), controlPlaneConfig.getCcpDmnJitUrl());

        ccp.setKubernetesApiUrl(controlPlaneConfig.getCcpKubernetesApiUrl());
        ccp.setDmnJitUrl(controlPlaneConfig.getCcpDmnJitUrl());
        ccp.setNamespace(controlPlaneConfig.getCcpNamespace());
        em.merge(ccp);
    }

    public ClusterControlPlane findOne() {
        return em.find(ClusterControlPlane.class, DEFAULT_CCP_ID);
    }
}