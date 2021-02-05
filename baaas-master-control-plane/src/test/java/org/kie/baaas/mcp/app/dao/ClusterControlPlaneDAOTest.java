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

import javax.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.baaas.mcp.app.config.MasterControlPlaneConfig;
import org.kie.baaas.mcp.app.model.ClusterControlPlane;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ClusterControlPlaneDAOTest {

    private static final String KUBERNETES_URL = "https://kubernetes.default";

    private static final String DMN_JIT_URL = "https://my-favourite-dmn-jit.com";

    @Mock
    private EntityManager em;

    @Mock
    private ClusterControlPlane controlPlane;

    @Captor
    private ArgumentCaptor<ClusterControlPlane> captor;

    @Mock
    private MasterControlPlaneConfig controlPlaneConfig;

    @InjectMocks
    private ClusterControlPlaneDAO controlPlaneDAO;

    @Test
    public void findOne() {
        when(em.find(ClusterControlPlane.class, 1)).thenReturn(controlPlane);

        ClusterControlPlane ccp = controlPlaneDAO.findOne();
        assertThat(ccp, is(notNullValue()));
        assertThat(ccp, equalTo(controlPlane));
    }

    @Test
    public void init_createsNewControlPlane() throws Exception {

        when(controlPlaneConfig.getDmnJitUrl()).thenReturn(DMN_JIT_URL);
        when(controlPlaneConfig.getKubernetesApiUrl()).thenReturn(KUBERNETES_URL);
        when(em.find(ClusterControlPlane.class, 1)).thenReturn(null);

        controlPlaneDAO.init();

        verify(em).merge(captor.capture());

        ClusterControlPlane ccp = captor.getValue();
        assertThat(ccp.getId(), equalTo(1));
        assertThat(ccp, is(notNullValue()));
        assertThat(ccp.getKubernetesApiUrl(), equalTo(KUBERNETES_URL));
        assertThat(ccp.getDmnJitUrl(), equalTo(DMN_JIT_URL));
    }

    @Test
    public void init_updatesExistingControlPlane() throws Exception {
        when(controlPlaneConfig.getDmnJitUrl()).thenReturn(DMN_JIT_URL);
        when(controlPlaneConfig.getKubernetesApiUrl()).thenReturn(KUBERNETES_URL);
        when(em.find(ClusterControlPlane.class, 1)).thenReturn(controlPlane);

        controlPlaneDAO.init();

        verify(controlPlane).setDmnJitUrl(DMN_JIT_URL);
        verify(controlPlane).setKubernetesApiUrl(KUBERNETES_URL);
        verify(em).merge(controlPlane);
    }
}