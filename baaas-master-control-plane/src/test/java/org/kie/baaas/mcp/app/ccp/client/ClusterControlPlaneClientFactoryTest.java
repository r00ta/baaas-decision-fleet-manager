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

package org.kie.baaas.mcp.app.ccp.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.baaas.mcp.app.ccp.ClusterControlPlaneClient;
import org.kie.baaas.mcp.app.model.ClusterControlPlane;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ClusterControlPlaneClientFactoryTest {

    @Mock
    private ClusterControlPlane ccp;

    private ClusterControlPlaneClientFactory factory;

    @BeforeEach
    public void before() {
        factory = new ClusterControlPlaneClientFactory();
    }

    @Test
    public void createClientFor() {
        String kubeApiUrl = "https://kube.baaas.redhat.com";
        when(ccp.getKubernetesApiUrl()).thenReturn(kubeApiUrl);

        ClusterControlPlaneClient controlPlaneClient = factory.createClientFor(ccp);
        assertThat(controlPlaneClient, is(notNullValue()));
    }
}
