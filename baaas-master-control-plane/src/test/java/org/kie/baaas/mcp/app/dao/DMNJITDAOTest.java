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

import java.net.URL;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.baaas.mcp.api.DMNJIT;
import org.kie.baaas.mcp.app.exceptions.MasterControlPlaneException;
import org.kie.baaas.mcp.app.model.ClusterControlPlane;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DMNJITDAOTest {

    @Mock
    private ClusterControlPlaneDAO controlPlaneDAO;

    @Mock
    private ClusterControlPlane clusterControlPlane;

    @InjectMocks
    private DMNJITDAO dmnjitdao;

    @Test
    public void findOne() throws Exception {
        String dmnJitUrl = "https://my-favourite-dmn-jit.com";
        configureClusterControlPlane(dmnJitUrl);

        DMNJIT dmnjit = dmnjitdao.findOne();
        assertThat(dmnjit.getUrl(), equalTo(new URL(dmnJitUrl)));
    }

    @Test
    public void findOne_noControlPlaneRegistered() {

        when(controlPlaneDAO.findOne()).thenReturn(null);

        MasterControlPlaneException thrown = assertThrows(MasterControlPlaneException.class, () -> {
            dmnjitdao.findOne();
        });

        assertThat(thrown.getMessage(), equalTo("There are zero registered Cluster Control Planes. Unable to retrieve DMN JIT details."));
    }

    @Test
    public void findOne_dmnJitUrlIsMalformed() {
        configureClusterControlPlane("not-an-url");

        MasterControlPlaneException thrown = assertThrows(MasterControlPlaneException.class, () -> {
            dmnjitdao.findOne();
        });

        assertThat(thrown.getMessage(), equalTo("The DMN JIT URL 'not-an-url' is malformed."));
    }

    private void configureClusterControlPlane(String url) {
        when(clusterControlPlane.getDmnJitUrl()).thenReturn(url);
        when(controlPlaneDAO.findOne()).thenReturn(clusterControlPlane);
    }
}