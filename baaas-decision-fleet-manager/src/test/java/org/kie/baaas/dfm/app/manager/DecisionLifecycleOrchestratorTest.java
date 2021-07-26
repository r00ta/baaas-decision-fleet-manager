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

package org.kie.baaas.dfm.app.manager;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.baaas.dfm.api.decisions.DecisionRequest;
import org.kie.baaas.dfm.api.eventing.Eventing;
import org.kie.baaas.dfm.api.eventing.kafka.Kafka;
import org.kie.baaas.dfm.app.dfs.DecisionFleetShardClient;
import org.kie.baaas.dfm.app.dfs.DecisionFleetShardSelector;
import org.kie.baaas.dfm.app.dfs.client.DecisionFleetShardClientFactory;
import org.kie.baaas.dfm.app.exceptions.DecisionFleetManagerException;
import org.kie.baaas.dfm.app.listener.ListenerManager;
import org.kie.baaas.dfm.app.managedservices.ManagedServicesClient;
import org.kie.baaas.dfm.app.managedservices.ManagedServicesException;
import org.kie.baaas.dfm.app.model.Decision;
import org.kie.baaas.dfm.app.model.DecisionFleetShard;
import org.kie.baaas.dfm.app.model.DecisionVersion;
import org.kie.baaas.dfm.app.model.deployment.Deployment;
import org.kie.baaas.dfm.app.model.eventing.KafkaConfig;
import org.kie.baaas.dfm.app.storage.DecisionDMNStorage;
import org.kie.baaas.dfm.app.vault.Secret;
import org.kie.baaas.dfm.app.vault.VaultService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.openshift.cloud.api.kas.invoker.ApiException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DecisionLifecycleOrchestratorTest {

    @Mock
    private DecisionFleetShardClient client;

    @Mock
    private DecisionFleetShardSelector selector;

    @Mock
    private DecisionFleetShard fleetShard;

    @Mock
    private DecisionManager decisionManager;

    @Mock
    private DecisionDMNStorage decisionDMNStorage;

    @Mock
    private DecisionFleetShardClientFactory clientFactory;

    @Mock
    @SuppressWarnings("unused")
    private ListenerManager listenerManager;

    @Mock
    VaultService vaultService;

    @Mock
    ManagedServicesClient managedServicesClient;

    @InjectMocks
    private DecisionLifecycleOrchestrator orchestrator;

    @Test
    public void createOrUpdateDecision() {
        String customerId = "foo";
        DecisionRequest request = mock(DecisionRequest.class);
        DecisionVersion decisionVersion = mock(DecisionVersion.class);
        Decision decision = mock(Decision.class);

        when(decisionVersion.getDecision()).thenReturn(decision);
        when(decisionManager.createOrUpdateVersion(customerId, request)).thenReturn(decisionVersion);
        when(selector.selectFleetShardForDeployment(decision)).thenReturn(fleetShard);
        when(clientFactory.createClientFor(fleetShard)).thenReturn(client);

        DecisionVersion created = orchestrator.createOrUpdateVersion(customerId, request);
        assertThat(created, is(notNullValue()));
        assertThat(created, equalTo(decisionVersion));

        verify(client).deploy(decisionVersion);
        verifyNoInteractions(vaultService);
    }

    @Test
    public void createOrUpdateDecisionNewServiceAccount() {
        String customerId = "foo";
        String saName = "daas-" + customerId + "-credentials";
        DecisionRequest request = mock(DecisionRequest.class);
        DecisionVersion decisionVersion = mock(DecisionVersion.class);
        Decision decision = mock(Decision.class);

        Eventing eventing = new Eventing();
        eventing.setKafka(new Kafka());
        when(request.getEventing()).thenReturn(eventing);
        when(decisionVersion.getDecision()).thenReturn(decision);
        when(decisionVersion.getKafkaConfig()).thenReturn(new KafkaConfig());
        when(decisionManager.createOrUpdateVersion(customerId, request)).thenReturn(decisionVersion);
        when(selector.selectFleetShardForDeployment(decision)).thenReturn(fleetShard);
        when(clientFactory.createClientFor(fleetShard)).thenReturn(client);
        Secret secret = new Secret().setId(saName).setValues(Map.of(ManagedServicesClient.CLIENT_ID, "foo", ManagedServicesClient.CLIENT_SECRET, "bar"));
        when(managedServicesClient.createOrReplaceServiceAccount(saName)).thenReturn(secret);

        DecisionVersion created = orchestrator.createOrUpdateVersion(customerId, request);
        assertThat(created, is(notNullValue()));
        assertThat(created, equalTo(decisionVersion));

        verify(client).deploy(decisionVersion);
        verify(vaultService, times(1)).get(eq(saName));
        verify(managedServicesClient, times(1)).createOrReplaceServiceAccount(eq(saName));
        verify(vaultService, times(1)).create(eq(secret));
    }

    @Test
    public void createOrUpdateDecisionErrorCreatingSA() {
        String customerId = "foo";
        String saName = "daas-" + customerId + "-credentials";
        DecisionRequest request = mock(DecisionRequest.class);

        DecisionVersion decisionVersion = mock(DecisionVersion.class);

        Eventing eventing = new Eventing();
        eventing.setKafka(new Kafka());
        when(request.getEventing()).thenReturn(eventing);
        when(decisionVersion.getKafkaConfig()).thenReturn(new KafkaConfig());
        when(decisionManager.createOrUpdateVersion(customerId, request)).thenReturn(decisionVersion);
        when(managedServicesClient.createOrReplaceServiceAccount(anyString()))
                .thenThrow(new ManagedServicesException("some error", new ApiException("api error")));

        assertThrows(ManagedServicesException.class, () -> orchestrator.createOrUpdateVersion(customerId, request));

        verifyNoInteractions(client);
        verify(vaultService, times(1)).get(eq(saName));
        verify(managedServicesClient, times(1)).createOrReplaceServiceAccount(eq(saName));
        verify(vaultService, times(0)).create(any(Secret.class));
    }

    @Test
    public void createOrUpdateDecision_recordsFailure() {
        String customerId = "foo";
        String decisionId = "bob";
        long version = 1l;
        DecisionRequest request = mock(DecisionRequest.class);
        DecisionVersion decisionVersion = mock(DecisionVersion.class);
        when(decisionVersion.getVersion()).thenReturn(version);
        Decision decision = mock(Decision.class);
        when(decision.getId()).thenReturn(decisionId);

        Eventing eventing = new Eventing();
        eventing.setKafka(new Kafka());
        when(request.getEventing()).thenReturn(eventing);
        when(decisionVersion.getDecision()).thenReturn(decision);
        when(decisionVersion.getKafkaConfig()).thenReturn(new KafkaConfig());
        when(decisionManager.createOrUpdateVersion(customerId, request)).thenReturn(decisionVersion);
        when(selector.selectFleetShardForDeployment(decision)).thenReturn(fleetShard);
        when(clientFactory.createClientFor(fleetShard)).thenReturn(client);
        when(vaultService.get(anyString())).thenReturn(new Secret());
        doThrow(new RuntimeException("Nope!")).when(client).deploy(decisionVersion);

        assertThrows(DecisionFleetManagerException.class, () -> {
            orchestrator.createOrUpdateVersion(customerId, request);
        });

        verify(decisionManager).failed(eq(customerId), eq(decisionId), eq(version), any(Deployment.class));
        verify(vaultService, times(1)).get(eq("daas-" + customerId + "-credentials"));
    }

    @Test
    public void deleteDecision() {

        String customerId = "foo";
        String decisionName = "bar";
        Decision decision = mock(Decision.class);

        when(decisionManager.deleteDecision(customerId, decisionName)).thenReturn(decision);
        when(selector.selectFleetShardForDeployment(decision)).thenReturn(fleetShard);
        when(clientFactory.createClientFor(fleetShard)).thenReturn(client);

        Decision deleted = orchestrator.deleteDecision(customerId, decisionName);
        assertThat(deleted, is(notNullValue()));
        assertThat(deleted, equalTo(decision));

        verify(client).delete(decision);
        verify(decisionDMNStorage).deleteDMN(customerId, deleted);
    }

    @Test
    public void deleteDecisionById() {
        String customerId = "foo";
        String decisionId = "id";
        Decision decision = new Decision();
        decision.setCustomerId(customerId);

        when(decisionManager.deleteDecision(decisionId)).thenReturn(decision);
        when(selector.selectFleetShardForDeployment(decision)).thenReturn(fleetShard);
        when(clientFactory.createClientFor(fleetShard)).thenReturn(client);

        Decision deleted = orchestrator.deleteDecision(decisionId);
        assertThat(deleted, is(notNullValue()));
        assertThat(deleted, equalTo(decision));

        verify(client).delete(decision);
        verify(decisionDMNStorage).deleteDMN(customerId, deleted);
    }

    @Test
    public void deleteVersion() {
        String customerId = "foo";
        String decisionName = "bar";
        long version = 2l;

        DecisionVersion decisionVersion = mock(DecisionVersion.class);
        Decision decision = mock(Decision.class);
        when(decisionVersion.getDecision()).thenReturn(decision);

        when(decisionManager.deleteVersion(customerId, decisionName, version)).thenReturn(decisionVersion);
        when(selector.selectFleetShardForDeployment(decision)).thenReturn(fleetShard);
        when(clientFactory.createClientFor(fleetShard)).thenReturn(client);

        DecisionVersion deleted = orchestrator.deleteVersion(customerId, decisionName, version);
        assertThat(deleted, is(notNullValue()));
        assertThat(deleted, equalTo(decisionVersion));

        verify(client).delete(decisionVersion);
    }

    @Test
    public void deleteVersionById() {
        String customerId = "foo";
        String decisionId = "id";
        long version = 2l;

        DecisionVersion decisionVersion = mock(DecisionVersion.class);
        Decision decision = new Decision();
        decision.setCustomerId(customerId);
        when(decisionVersion.getDecision()).thenReturn(decision);

        when(decisionManager.deleteVersion(decisionId, version)).thenReturn(decisionVersion);
        when(selector.selectFleetShardForDeployment(decision)).thenReturn(fleetShard);
        when(clientFactory.createClientFor(fleetShard)).thenReturn(client);

        DecisionVersion deleted = orchestrator.deleteVersion(decisionId, version);
        assertThat(deleted, is(notNullValue()));
        assertThat(deleted, equalTo(decisionVersion));

        verify(client).delete(decisionVersion);
    }

    @Test
    public void rollbackToVersion() {
        String customerId = "foo";
        String decisionName = "bar";
        long version = 2l;

        DecisionVersion decisionVersion = mock(DecisionVersion.class);
        Decision decision = mock(Decision.class);
        when(decisionVersion.getDecision()).thenReturn(decision);

        when(decisionManager.setCurrentVersion(customerId, decisionName, version)).thenReturn(decisionVersion);
        when(selector.selectFleetShardForDeployment(decision)).thenReturn(fleetShard);
        when(clientFactory.createClientFor(fleetShard)).thenReturn(client);

        DecisionVersion rollback = orchestrator.setCurrentVersion(customerId, decisionName, version);
        assertThat(rollback, is(notNullValue()));
        assertThat(rollback, equalTo(decisionVersion));

        verify(client).deploy(decisionVersion);
    }
}
