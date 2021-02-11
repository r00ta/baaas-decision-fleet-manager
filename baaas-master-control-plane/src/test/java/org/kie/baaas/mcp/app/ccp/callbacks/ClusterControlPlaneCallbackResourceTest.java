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

package org.kie.baaas.mcp.app.ccp.callbacks;

import java.net.URI;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.baaas.ccp.api.Phase;
import org.kie.baaas.ccp.api.Webhook;
import org.kie.baaas.mcp.app.exceptions.MasterControlPlaneException;
import org.kie.baaas.mcp.app.manager.DecisionManager;
import org.kie.baaas.mcp.app.model.deployment.Deployment;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ClusterControlPlaneCallbackResourceTest {

    @Mock
    private DecisionManager decisionManager;

    @InjectMocks
    private ClusterControlPlaneCallbackResource callbackResource;

    @Captor
    ArgumentCaptor<Deployment> deploymentCap;

    @Test
    public void deployed() {

        Webhook webhook = new Webhook("1", "my-decision", "1", Phase.CURRENT, URI.create("https://mydecision.baaas.redhat.com"), "foo", LocalDateTime.now().toString(), "namespace", "versionName");
        callbackResource.processClusterControlPlaneCallback(webhook.getDecision(), 1l, webhook);

        verify(decisionManager).deployed(eq(webhook.getCustomer()), eq(webhook.getDecision()), eq(1l), deploymentCap.capture());

        Deployment deployment = deploymentCap.getValue();
        assertThat(deployment, is(notNullValue()));
        assertThat(deployment.getNamespace(), equalTo(webhook.getNamespace()));
        assertThat(deployment.getName(), equalTo(webhook.getDecision()));
        assertThat(deployment.getVersionName(), equalTo(webhook.getVersionResource()));
        assertThat(deployment.getUrl(), equalTo(webhook.getEndpoint().toString()));
    }

    @Test
    public void failed() {

        Webhook webhook = new Webhook("1", "my-decision", "1", Phase.FAILED, URI.create("https://mydecision.baaas.redhat.com"), "foo", LocalDateTime.now().toString(), "namespace", "versionName");
        callbackResource.processClusterControlPlaneCallback(webhook.getDecision(), 1l, webhook);

        verify(decisionManager).failed(eq(webhook.getCustomer()), eq(webhook.getDecision()), eq(1l), deploymentCap.capture());

        Deployment deployment = deploymentCap.getValue();
        assertThat(deployment, is(notNullValue()));
        assertThat(deployment.getNamespace(), equalTo(webhook.getNamespace()));
        assertThat(deployment.getName(), equalTo(webhook.getDecision()));
        assertThat(deployment.getVersionName(), equalTo(webhook.getVersionResource()));
        assertThat(deployment.getUrl(), equalTo(webhook.getEndpoint().toString()));
    }

    @Test
    public void unsupportedCallback() {
        Webhook webhook = new Webhook("1", "my-decision", "1", Phase.READY, URI.create("https://mydecision.baaas.redhat.com"), "foo", LocalDateTime.now().toString(), "namespace", "versionName");
        assertThrows(MasterControlPlaneException.class, () -> {
            callbackResource.processClusterControlPlaneCallback(webhook.getDecision(), 1l, webhook);
        });
    }
}
