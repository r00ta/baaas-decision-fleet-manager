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

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.kie.baaas.mcp.app.config.MasterControlPlaneConfig;
import org.kie.baaas.mcp.app.model.ClusterControlPlane;
import org.kie.baaas.mcp.app.model.ListResult;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
public class ClusterControlPlaneDAOTest {

    @Inject
    MasterControlPlaneConfig controlPlaneConfig;

    @Inject
    ClusterControlPlaneDAO controlPlaneDAO;

    @TestTransaction
    @Test
    public void init_createsNewControlPlane() {
        ClusterControlPlane fleetShard = controlPlaneDAO.findById(1);
        assertFleetShard(fleetShard);
    }

    @Test
    @TestTransaction
    public void listAll() {
        ListResult<ClusterControlPlane> listResult = controlPlaneDAO.listAll(0, 100);
        assertThat(listResult.getPage(), equalTo(0L));
        assertThat(listResult.getTotal(), equalTo(1L));
        assertThat(listResult.getSize(), equalTo(1L));

        ClusterControlPlane fleetShard = listResult.getItems().get(0);
        assertFleetShard(fleetShard);
    }

    private void assertFleetShard(ClusterControlPlane fleetShard) {
        assertThat(fleetShard.getId(), equalTo(1));
        assertThat(fleetShard, is(notNullValue()));
        assertThat(fleetShard.getKubernetesApiUrl(), equalTo(controlPlaneConfig.getCcpKubernetesApiUrl()));
        assertThat(fleetShard.getDmnJitUrl(), equalTo(controlPlaneConfig.getCcpDmnJitUrl()));
        assertThat(fleetShard.getNamespace(), equalTo(controlPlaneConfig.getCcpNamespace()));
    }
}