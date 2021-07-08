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

package org.kie.baaas.dfm.app.dfs.callbacks;

import java.net.URI;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.baaas.dfm.app.manager.DecisionLifecycleOrchestrator;
import org.kie.baaas.dfm.app.model.deployment.Deployment;
import org.kie.baaas.dfs.api.Phase;
import org.kie.baaas.dfs.api.Webhook;
import org.kie.baaas.dfs.api.WebhookBuilder;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class DecisionDecisionFleetShardCallbackResourceTest {

    @Mock
    private DecisionLifecycleOrchestrator decisionLifecycleOrch;

    @InjectMocks
    private DecisionFleetShardCallbackResource callbackResource;

    @Captor
    ArgumentCaptor<Deployment> deploymentCap;

    private Webhook createWithPhase(Phase phase) {
        return new WebhookBuilder().withAt(ZonedDateTime.now(ZoneOffset.UTC).toString())
                .withCustomer("1")
                .withDecision("my-decision")
                .withCurrentEndpoint(URI.create("https://mydecision.baaas.redhat.com"))
                .withVersionEndpoint(URI.create("https://mydecision-1.baaas.redhat.com"))
                .withMessage("message")
                .withNamespace("namespace")
                .withPhase(phase)
                .withVersion("1")
                .build();
    }

    @Test
    public void deployed() {

        Webhook webhook = createWithPhase(Phase.CURRENT);

        callbackResource.processCallback(webhook, webhook.getDecision(), 1l);

        verify(decisionLifecycleOrch).deployed(eq(webhook.getCustomer()), eq(webhook.getDecision()), eq(1l), deploymentCap.capture());

        Deployment deployment = deploymentCap.getValue();
        assertThat(deployment, is(notNullValue()));
        assertThat(deployment.getNamespace(), equalTo(webhook.getNamespace()));
        assertThat(deployment.getName(), equalTo(webhook.getDecision()));
        assertThat(deployment.getVersionName(), equalTo(webhook.getVersionResource()));
        assertThat(deployment.getVersionUrl(), equalTo(webhook.getVersionEndpoint().toString()));
    }

    @Test
    public void failed() {

        Webhook webhook = createWithPhase(Phase.FAILED);
        webhook.setVersionEndpoint(null);
        webhook.setCurrentEndpoint(null);
        callbackResource.processCallback(webhook, webhook.getDecision(), 1l);

        verify(decisionLifecycleOrch).failed(eq(webhook.getCustomer()), eq(webhook.getDecision()), eq(1l), deploymentCap.capture());

        Deployment deployment = deploymentCap.getValue();
        assertThat(deployment, is(notNullValue()));
        assertThat(deployment.getNamespace(), equalTo(webhook.getNamespace()));
        assertThat(deployment.getName(), equalTo(webhook.getDecision()));
        assertThat(deployment.getVersionName(), equalTo(webhook.getVersionResource()));
    }
}
