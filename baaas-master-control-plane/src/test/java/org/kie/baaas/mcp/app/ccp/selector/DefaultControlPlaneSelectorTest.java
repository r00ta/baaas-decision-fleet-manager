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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.baaas.mcp.app.dao.ClusterControlPlaneDAO;
import org.kie.baaas.mcp.app.model.ClusterControlPlane;
import org.kie.baaas.mcp.app.model.Decision;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DefaultControlPlaneSelectorTest {

    @Mock
    private ClusterControlPlaneDAO controlPlaneDAO;

    @Mock
    private ClusterControlPlane controlPlane;

    @Mock
    private Decision decision;

    @InjectMocks
    private DefaultControlPlaneSelector selector;

    @Test
    public void selectControlPlaneForDeployment() {
        when(controlPlaneDAO.findOne()).thenReturn(controlPlane);

        ClusterControlPlane planeForDeployment = selector.selectControlPlaneForDeployment(decision);
        assertThat(planeForDeployment, is(notNullValue()));
        assertThat(planeForDeployment, equalTo(controlPlane));
    }
}
