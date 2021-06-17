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

import java.util.List;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.kie.baaas.mcp.app.config.MasterControlPlaneConfig;
import org.kie.baaas.mcp.app.model.ClusterControlPlane;
import org.kie.baaas.mcp.app.model.ListResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;

import static io.quarkus.panache.common.Sort.ascending;

/**
 * DAO Implementation for working with ClusterControlPlane instances.
 */
@ApplicationScoped
@Transactional
public class ClusterControlPlaneDAO implements PanacheRepositoryBase<ClusterControlPlane, Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterControlPlaneDAO.class);

    public static final int DEFAULT_CCP_ID = 1;

    private final MasterControlPlaneConfig controlPlaneConfig;

    @Inject
    public ClusterControlPlaneDAO(MasterControlPlaneConfig controlPlaneConfig) {

        Objects.requireNonNull(controlPlaneConfig, "controlPlaneConfig cannot be null");
        this.controlPlaneConfig = controlPlaneConfig;
    }

    public void init() {
        ClusterControlPlane ccp = findById(DEFAULT_CCP_ID);
        if (ccp == null) {
            ccp = new ClusterControlPlane();
            ccp.setId(DEFAULT_CCP_ID);
        }

        LOGGER.info("Registering Cluster Control Plane. Kubernetes API URL: '{}'. DMN JIT URL: '{}'.", controlPlaneConfig.getCcpKubernetesApiUrl(), controlPlaneConfig.getCcpDmnJitUrl());

        ccp.setKubernetesApiUrl(controlPlaneConfig.getCcpKubernetesApiUrl());
        ccp.setDmnJitUrl(controlPlaneConfig.getCcpDmnJitUrl());
        ccp.setNamespace(controlPlaneConfig.getCcpNamespace());
        persist(ccp);
    }

    public ListResult<ClusterControlPlane> listAll(int page, int size) {
        PanacheQuery<ClusterControlPlane> pagedQuery = findAll(ascending(ClusterControlPlane.DMN_JIT_URL_PARAM)).page(Page.of(page, size));
        List<ClusterControlPlane> webhooks = pagedQuery.list();
        long count = pagedQuery.count();
        return new ListResult<>(webhooks, page, count);
    }
}
