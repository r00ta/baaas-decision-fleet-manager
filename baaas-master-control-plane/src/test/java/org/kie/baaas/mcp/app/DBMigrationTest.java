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

package org.kie.baaas.mcp.app;

import javax.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.kie.baaas.mcp.app.dao.ClusterControlPlaneDAO;
import org.kie.baaas.mcp.app.model.ClusterControlPlane;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
public class DBMigrationTest {

    @Inject
    ClusterControlPlaneDAO controlPlaneDAO;

    @Test
    public void flyway_migrate() {

        ClusterControlPlane clusterControlPlane = controlPlaneDAO.findOne();
        assertThat(clusterControlPlane.getDmnJitUrl(), equalTo("https://baaas-dmn-jit-baaas-dmn-jit-demo.apps.kogito-cloud.automation.rhmw.io/jitdmn"));
        assertThat(clusterControlPlane.getKubernetesApiUrl(), equalTo("https://kubernetes.default.svc"));
    }
}