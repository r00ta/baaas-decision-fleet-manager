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

package org.kie.baaas.mcp.app.controller.modelmappers;

import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.baaas.mcp.app.config.MasterControlPlaneConfig;
import org.kie.baaas.mcp.app.model.Decision;
import org.kie.baaas.mcp.app.model.DecisionVersion;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HrefGeneratorTest {

    private static final String API_BASE_URL = "https://api.openshift.com/baaas/v1";

    @Mock
    private MasterControlPlaneConfig config;

    @InjectMocks
    private HrefGenerator hrefGenerator;

    @BeforeEach
    public void before() throws Exception {
        when(config.getApiBaseUrl()).thenReturn(new URL(API_BASE_URL));
    }

    @Test
    public void generateDecisionHref() {
        DecisionVersion version = new DecisionVersion();
        version.setVersion(1l);

        Decision decision = new Decision();
        version.setDecision(decision);

        String href = hrefGenerator.generateDecisionHref(version);
        assertThat(href, equalTo(API_BASE_URL + "/decisions/" + decision.getId() + "/versions/" + version.getVersion()));
    }

    @Test
    public void generateDecisionDMNHref() {

        DecisionVersion version = new DecisionVersion();
        version.setVersion(1l);

        Decision decision = new Decision();
        version.setDecision(decision);

        String href = hrefGenerator.generateDecisionDMNHref(version);
        assertThat(href, equalTo(API_BASE_URL + "/decisions/" + decision.getId() + "/versions/" + version.getVersion() + "/dmn"));
    }
}
