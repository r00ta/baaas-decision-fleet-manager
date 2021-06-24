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

import java.net.URI;
import java.util.Collection;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.baaas.dfs.api.DecisionRequest;
import org.kie.baaas.mcp.app.config.MasterControlPlaneConfig;
import org.kie.baaas.mcp.app.dao.ClusterControlPlaneDAO;
import org.kie.baaas.mcp.app.model.Decision;
import org.kie.baaas.mcp.app.model.DecisionVersion;
import org.kie.baaas.mcp.app.model.DecisionVersionStatus;
import org.kie.baaas.mcp.app.model.deployment.Deployment;
import org.kie.baaas.mcp.app.model.eventing.Credential;
import org.kie.baaas.mcp.app.model.eventing.KafkaConfig;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesMockServerTestResource;
import io.quarkus.test.kubernetes.client.MockServer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.kie.baaas.mcp.app.TestConstants.DEFAULT_CUSTOMER_ID;

@QuarkusTestResource(KubernetesMockServerTestResource.class)
@QuarkusTest
public class DefaultClusterControlPlaneClientTest {

    @MockServer
    KubernetesMockServer mockServer;

    @Inject
    MasterControlPlaneConfig config;

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    ClusterControlPlaneDAO controlPlaneDAO;

    private DefaultClusterControlPlaneClient client;

    @BeforeEach
    public void before() {
        client = new DefaultClusterControlPlaneClient(config, kubernetesClient, controlPlaneDAO.findById(ClusterControlPlaneDAO.DEFAULT_CCP_ID));
    }

    private void createNamespace(String name) {
        Namespace ns = new NamespaceBuilder().withNewMetadata().withName(name).and().build();
        mockServer.expect().get().withPath("/api/v1/namespaces/" + name).andReturn(200, ns).always();
    }

    private void createClusterControlPlaneNamespace() {
        createNamespace(config.getCcpNamespace());
    }

    private DecisionVersion createDecisionVersion(boolean addKafka) {

        Decision decision = new Decision();
        decision.setName("my first decision");
        decision.setDescription("An amazing decision");
        decision.setCustomerId(DEFAULT_CUSTOMER_ID);

        DecisionVersion decisionVersion = new DecisionVersion();
        decision.addVersion(decisionVersion);
        decision.setCurrentVersion(decisionVersion);

        decisionVersion.setVersion(1l);
        decisionVersion.setStatus(DecisionVersionStatus.BUILDING);
        decisionVersion.setDmnLocation("s3://baaas-dmn-bucket/customers/1/my-first-decision/1/dmn.xml");

        if (addKafka) {
            KafkaConfig kafkaConfig = new KafkaConfig();
            kafkaConfig.setSourceTopic("kafka-source");
            kafkaConfig.setSinkTopic("kafka-sink");
            kafkaConfig.setBootstrapServers("my-kafka:9002");
            kafkaConfig.setCredential(new Credential()
                    .setClientId("the-id")
                    .setClientSecret("the-secret"));
            decisionVersion.setKafkaConfig(kafkaConfig);
        }

        return decisionVersion;
    }

    private CCPResponseBuilder expectDecisionRequest() {
        CCPResponseBuilder<DecisionRequest> responseBuilder = new CCPResponseBuilder(DecisionRequest.class);
        createClusterControlPlaneNamespace();
        mockServer.expect().post().withPath("/apis/operator.baaas/v1alpha1/namespaces/" + config.getCcpNamespace() + "/decisionrequests").andReply(responseBuilder).once();
        return responseBuilder;
    }

    @Test
    public void deploy() {

        CCPResponseBuilder<DecisionRequest> responseBuilder = expectDecisionRequest();
        DecisionVersion decisionVersion = createDecisionVersion(false);

        client.deploy(decisionVersion);

        DecisionRequest payload = responseBuilder.getPayload();
        assertThat(payload, is(notNullValue()));

        assertThat(payload.getMetadata().getName(), equalTo(DEFAULT_CUSTOMER_ID + "-" + decisionVersion.getDecision().getId() + "-1"));
        assertThat(payload.getSpec().getCustomerId(), equalTo(DEFAULT_CUSTOMER_ID));
        assertThat(payload.getSpec().getName(), equalTo(KubernetesResourceUtil.sanitizeName(decisionVersion.getDecision().getName())));

        assertThat(payload.getSpec().getVersion(), equalTo(String.valueOf(decisionVersion.getVersion())));
        assertThat(payload.getSpec().getSource(), equalTo(URI.create(decisionVersion.getDmnLocation())));
        assertThat(payload.getSpec().getKafka(), is(nullValue()));

        Collection<URI> webhooks = payload.getSpec().getWebhooks();
        assertThat(webhooks, hasSize(1));
        assertThat(webhooks.iterator().next().toString(),
                equalTo(config.getApiBaseUrl() + "/callback/decisions/" + decisionVersion.getDecision().getId() + "/versions/" + decisionVersion.getVersion()));
    }

    @Test
    public void deploy_withUpperCaseDecisionName() {

        CCPResponseBuilder<DecisionRequest> responseBuilder = expectDecisionRequest();
        DecisionVersion decisionVersion = createDecisionVersion(false);
        decisionVersion.getDecision().setName("My-Decision");

        client.deploy(decisionVersion);

        DecisionRequest payload = responseBuilder.getPayload();
        assertThat(payload, is(notNullValue()));

        assertThat(payload.getMetadata().getName(), equalTo(DEFAULT_CUSTOMER_ID + "-" + decisionVersion.getDecision().getId() + "-1"));
        assertThat(payload.getSpec().getCustomerId(), equalTo(DEFAULT_CUSTOMER_ID));
        assertThat(payload.getSpec().getName(), equalTo(KubernetesResourceUtil.sanitizeName(decisionVersion.getDecision().getName()).toLowerCase()));

        assertThat(payload.getSpec().getVersion(), equalTo(String.valueOf(decisionVersion.getVersion())));
        assertThat(payload.getSpec().getSource(), equalTo(URI.create(decisionVersion.getDmnLocation())));
        assertThat(payload.getSpec().getKafka(), is(nullValue()));

        Collection<URI> webhooks = payload.getSpec().getWebhooks();
        assertThat(webhooks, hasSize(1));
        assertThat(webhooks.iterator().next().toString(),
                equalTo(config.getApiBaseUrl() + "/callback/decisions/" + decisionVersion.getDecision().getId() + "/versions/" + decisionVersion.getVersion()));
    }

    @Test
    public void deploy_withKafka() {

        CCPResponseBuilder<DecisionRequest> responseBuilder = expectDecisionRequest();
        DecisionVersion decisionVersion = createDecisionVersion(true);

        client.deploy(decisionVersion);

        DecisionRequest payload = responseBuilder.getPayload();
        assertThat(payload, is(notNullValue()));

        assertThat(payload.getMetadata().getName(), equalTo(DEFAULT_CUSTOMER_ID + "-" + decisionVersion.getDecision().getId() + "-1"));
        assertThat(payload.getSpec().getCustomerId(), equalTo(DEFAULT_CUSTOMER_ID));
        assertThat(payload.getSpec().getName(), equalTo(KubernetesResourceUtil.sanitizeName(decisionVersion.getDecision().getName())));

        assertThat(payload.getSpec().getVersion(), equalTo(String.valueOf(decisionVersion.getVersion())));
        assertThat(payload.getSpec().getSource(), equalTo(URI.create(decisionVersion.getDmnLocation())));
        assertThat(payload.getSpec().getKafka().getInputTopic(), equalTo(decisionVersion.getKafkaConfig().getSourceTopic()));
        assertThat(payload.getSpec().getKafka().getOutputTopic(), equalTo(decisionVersion.getKafkaConfig().getSinkTopic()));
        assertThat(payload.getSpec().getKafka().getCredential().getClientId(),
                equalTo(decisionVersion.getKafkaConfig().getCredential().getClientId()));
        assertThat(payload.getSpec().getKafka().getCredential().getClientSecret(),
                equalTo(decisionVersion.getKafkaConfig().getCredential().getClientSecret()));
        assertThat(payload.getSpec().getKafka().getBootstrapServers(),
                equalTo(decisionVersion.getKafkaConfig().getBootstrapServers()));

        Collection<URI> webhooks = payload.getSpec().getWebhooks();
        assertThat(webhooks, hasSize(1));
        assertThat(webhooks.iterator().next().toString(),
                equalTo(config.getApiBaseUrl() + "/callback/decisions/" + decisionVersion.getDecision().getId() + "/versions/" + decisionVersion.getVersion()));
    }

    private DecisionVersion createDecisionVersionWithDeployment(boolean addKafka) {
        DecisionVersion decisionVersion = createDecisionVersion(addKafka);
        Deployment deployment = new Deployment();
        deployment.setNamespace("test-namespace");
        deployment.setVersionName("foo-" + System.currentTimeMillis());
        deployment.setName(KubernetesResourceUtil.sanitizeName(decisionVersion.getDecision().getName()));
        decisionVersion.setDeployment(deployment);
        return decisionVersion;
    }

    private void createDeploymentNamespace(Deployment deployment) {
        createNamespace(deployment.getNamespace());
    }

    private void createDecision(Deployment deployment) {
        org.kie.baaas.dfs.api.Decision decision = new org.kie.baaas.dfs.api.Decision();
        String path = "/apis/operator.baaas/v1alpha1/namespaces/" + deployment.getNamespace() + "/decisions/" + deployment.getName();
        mockServer.expect().get().withPath(path).andReturn(200, decision).once();
    }

    private void createDecisionVersion(Deployment deployment) {
        org.kie.baaas.dfs.api.DecisionVersion dv = new org.kie.baaas.dfs.api.DecisionVersion();
        String path = "/apis/operator.baaas/v1alpha1/namespaces/" + deployment.getNamespace() + "/decisionversions/" + deployment.getVersionName();
        mockServer.expect().get().withPath(path).andReturn(200, dv).once();
    }

    @Test
    public void deleteVersion() {

        DecisionVersion decisionVersion = createDecisionVersionWithDeployment(false);
        Deployment deployment = decisionVersion.getDeployment();
        createDeploymentNamespace(deployment);
        createDecisionVersion(deployment);

        CCPResponseBuilder<org.kie.baaas.dfs.api.DecisionVersion> deleteResponse = new CCPResponseBuilder<>(org.kie.baaas.dfs.api.DecisionVersion.class);
        String deletePath = "/apis/operator.baaas/v1alpha1/namespaces/" + deployment.getNamespace() + "/decisionversions/" + deployment.getVersionName();
        mockServer.expect().delete().withPath(deletePath).andReply(deleteResponse).once();

        client.delete(decisionVersion);

        assertThat(deleteResponse.isInvoked(), is(true));
    }

    @Test
    public void deleteVersion_versionDoesNotExist() {

        DecisionVersion decisionVersion = createDecisionVersionWithDeployment(false);
        Deployment deployment = decisionVersion.getDeployment();
        createDeploymentNamespace(deployment);

        CCPResponseBuilder<org.kie.baaas.dfs.api.DecisionVersion> deleteResponse = new CCPResponseBuilder<>(org.kie.baaas.dfs.api.DecisionVersion.class);
        String deletePath = "/apis/operator.baaas/v1alpha1/namespaces/" + deployment.getNamespace() + "/decisionversions/" + deployment.getVersionName();
        mockServer.expect().delete().withPath(deletePath).andReply(deleteResponse).once();

        client.delete(decisionVersion);

        assertThat(deleteResponse.isInvoked(), is(false));
    }

    @Test
    public void deleteVersion_noNamespaceOnDeploymentDueToFailure() {
        DecisionVersion decisionVersion = createDecisionVersionWithDeployment(false);
        Deployment deployment = decisionVersion.getDeployment();
        deployment.setNamespace(null);
        deployment.setName(null);
        deployment.setVersionName(null);

        CCPResponseBuilder<org.kie.baaas.dfs.api.DecisionVersion> deleteResponse = new CCPResponseBuilder<>(org.kie.baaas.dfs.api.DecisionVersion.class);
        String deletePath = "/apis/operator.baaas/v1alpha1/namespaces/" + deployment.getNamespace() + "/decisionversions/" + deployment.getVersionName();
        mockServer.expect().delete().withPath(deletePath).andReply(deleteResponse).once();

        client.delete(decisionVersion);

        assertThat(deleteResponse.isInvoked(), is(false));
    }

    @Test
    public void deleteDecision() {

        DecisionVersion decisionVersion = createDecisionVersionWithDeployment(false);
        Deployment deployment = decisionVersion.getDeployment();
        createDeploymentNamespace(deployment);
        createDecision(deployment);

        CCPResponseBuilder<org.kie.baaas.dfs.api.Decision> deleteResponse = new CCPResponseBuilder<>(org.kie.baaas.dfs.api.Decision.class);
        String deletePath = "/apis/operator.baaas/v1alpha1/namespaces/" + deployment.getNamespace() + "/decisions/" + deployment.getName();
        mockServer.expect().delete().withPath(deletePath).andReply(deleteResponse).once();

        client.delete(decisionVersion.getDecision());

        assertThat(deleteResponse.isInvoked(), is(true));
    }

    @Test
    public void deleteDecision_decisionDoesNotExist() {

        DecisionVersion decisionVersion = createDecisionVersionWithDeployment(false);
        Deployment deployment = decisionVersion.getDeployment();
        createDeploymentNamespace(deployment);

        CCPResponseBuilder<org.kie.baaas.dfs.api.Decision> deleteResponse = new CCPResponseBuilder<>(org.kie.baaas.dfs.api.Decision.class);
        String deletePath = "/apis/operator.baaas/v1alpha1/namespaces/" + deployment.getNamespace() + "/decisions/" + deployment.getName();
        mockServer.expect().delete().withPath(deletePath).andReply(deleteResponse).once();

        client.delete(decisionVersion.getDecision());
        assertThat(deleteResponse.isInvoked(), is(false));
    }
}
