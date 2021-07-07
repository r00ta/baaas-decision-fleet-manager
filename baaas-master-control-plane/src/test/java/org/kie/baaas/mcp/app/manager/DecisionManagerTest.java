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

package org.kie.baaas.mcp.app.manager;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kie.baaas.mcp.api.decisions.DecisionRequest;
import org.kie.baaas.mcp.api.decisions.Model;
import org.kie.baaas.mcp.api.eventing.Eventing;
import org.kie.baaas.mcp.api.eventing.kafka.Kafka;
import org.kie.baaas.mcp.app.dao.DecisionDAO;
import org.kie.baaas.mcp.app.dao.DecisionVersionDAO;
import org.kie.baaas.mcp.app.model.Decision;
import org.kie.baaas.mcp.app.model.DecisionVersion;
import org.kie.baaas.mcp.app.model.DecisionVersionStatus;
import org.kie.baaas.mcp.app.model.ListResult;
import org.kie.baaas.mcp.app.model.deployment.Deployment;
import org.kie.baaas.mcp.app.model.eventing.KafkaConfig;
import org.kie.baaas.mcp.app.storage.DMNStorageRequest;
import org.kie.baaas.mcp.app.storage.DecisionDMNStorage;
import org.kie.baaas.mcp.app.storage.s3.S3DMNStorage;
import org.mockito.Mockito;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.kie.baaas.mcp.app.TestConstants.DEFAULT_CUSTOMER_ID;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
public class DecisionManagerTest {

    @Inject
    DecisionDAO decisionDAO;

    @Inject
    DecisionVersionDAO decisionVersionDAO;

    @Inject
    DecisionManager decisionManager;

    @Inject
    DecisionDMNStorage dmnStorage;

    private DecisionRequest createApiRequest() {

        Model model = new Model();
        model.setDmn("foo");

        DecisionRequest decisions = new DecisionRequest();
        decisions.setDescription("The Best Decision Ever");
        decisions.setName("robs-first-decision");
        decisions.setModel(model);
        return decisions;
    }

    @BeforeAll
    public static void beforeAll() {
        S3DMNStorage storage = Mockito.mock(S3DMNStorage.class);
        QuarkusMock.installMockForType(storage, S3DMNStorage.class);
    }

    private DMNStorageRequest createStorageRequest() {
        DMNStorageRequest request = new DMNStorageRequest("provider-url", "hash");
        when(dmnStorage.writeDMN(anyString(), Mockito.any(DecisionRequest.class), Mockito.any(DecisionVersion.class))).thenReturn(request);
        return request;
    }

    @TestTransaction
    @Test
    public void createNewVersion_withKafka() {
        DMNStorageRequest request = createStorageRequest();

        Eventing eventing = new Eventing();
        eventing.setKafka(new Kafka());
        eventing.getKafka().setSink("my-sink");
        eventing.getKafka().setSource("my-source");
        eventing.getKafka().setBootstrapServers("example:9002");

        DecisionRequest apiResponse = createApiRequest();
        apiResponse.setEventing(eventing);

        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(DEFAULT_CUSTOMER_ID, apiResponse);
        KafkaConfig kafkaConfig = decisionVersion.getKafkaConfig();
        assertThat(kafkaConfig.getSinkTopic(), equalTo(eventing.getKafka().getSink()));
        assertThat(kafkaConfig.getSourceTopic(), equalTo(eventing.getKafka().getSource()));
        assertThat(kafkaConfig.getBootstrapServers(), equalTo(eventing.getKafka().getBootstrapServers()));
        assertThat(decisionVersion.getDmnLocation(), equalTo(request.getProviderUrl()));
        assertThat(decisionVersion.getDmnMd5(), equalTo(request.getMd5Hash()));

    }

    @TestTransaction
    @Test
    public void createNewVersion_newDecision() {
        DMNStorageRequest request = createStorageRequest();
        DecisionRequest apiRequest = createApiRequest();

        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(DEFAULT_CUSTOMER_ID, apiRequest);
        assertThat(decisionVersion, is(notNullValue()));

        Decision decision = decisionVersion.getDecision();

        assertThat(decision.getName(), equalTo(apiRequest.getName()));
        assertThat(decisionVersion.getDescription(), equalTo(apiRequest.getDescription()));
        assertThat(decision.getCustomerId(), equalTo(DEFAULT_CUSTOMER_ID));

        assertThat(decisionVersion.getVersion(), equalTo(1L));
        assertThat(decisionVersion.getStatus(), equalTo(DecisionVersionStatus.BUILDING));
        assertThat(decisionVersion.getSubmittedAt(), is(notNullValue()));
        assertThat(decisionVersion.getDmnMd5(), equalTo(request.getMd5Hash()));
        assertThat(decisionVersion.getDmnLocation(), equalTo((request.getProviderUrl())));

        ZonedDateTime zdt = ZonedDateTime.parse(decisionVersion.getSubmittedAt().toString(), DateTimeFormatter.ISO_ZONED_DATE_TIME);
        assertThat(zdt, is(notNullValue()));
    }

    private Deployment createDeployment() {
        Deployment deployment = new Deployment();
        deployment.setVersionUrl("http://foo-1");
        deployment.setCurrentUrl("http://foo-current");
        deployment.setVersionName("my-version");
        deployment.setNamespace("my-namespace");
        deployment.setName("my-name");
        return deployment;
    }

    @TestTransaction
    @Test
    public void deployed_withFirstVersionOfDecision() {
        createStorageRequest();
        DecisionRequest apiRequest = createApiRequest();

        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(DEFAULT_CUSTOMER_ID, apiRequest);
        Decision decision = decisionVersion.getDecision();

        Deployment deployment = createDeployment();
        DecisionVersion deployed = decisionManager.deployed(decision.getCustomerId(), decision.getId(), decisionVersion.getVersion(), deployment);

        assertThat(deployed, is(notNullValue()));
        assertThat(deployed.getStatus(), equalTo(DecisionVersionStatus.CURRENT));
        assertThat(deployed.getPublishedAt(), is(notNullValue()));
        assertThat(deployed.getDeployment(), is(notNullValue()));
        assertThat(deployed.getDecision().getCurrentVersion().getVersion(), equalTo(deployed.getVersion()));
        assertThat(deployed.getDecision().getNextVersion(), is(nullValue()));
    }

    @TestTransaction
    @Test
    public void failed_withFirstVersionOfDecision() {
        DMNStorageRequest request = createStorageRequest();
        DecisionRequest apiRequest = createApiRequest();

        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(DEFAULT_CUSTOMER_ID, apiRequest);
        Decision decision = decisionVersion.getDecision();

        DecisionVersion deployed = decisionManager.failed(decision.getCustomerId(), decision.getId(), decisionVersion.getVersion(), createDeployment());
        assertThat(deployed, is(notNullValue()));
        assertThat(deployed.getStatus(), equalTo(DecisionVersionStatus.FAILED));
        assertThat(deployed.getDmnMd5(), equalTo(request.getMd5Hash()));
        assertThat(deployed.getDmnLocation(), equalTo(request.getProviderUrl()));

        assertThat(decision.getCurrentVersion().getStatus(), equalTo(DecisionVersionStatus.FAILED));
        assertThat(decision.getNextVersion(), is(nullValue()));
    }

    @TestTransaction
    @Test
    public void createNewVersion_updateFailsWhenStillCreating() {
        createStorageRequest();
        DecisionRequest apiRequest = createApiRequest();

        decisionManager.createOrUpdateVersion(DEFAULT_CUSTOMER_ID, apiRequest);

        apiRequest.setDescription("An updated version!");
        apiRequest.getModel().setDmn("Updated dmn!");

        DecisionLifecycleException thrown = assertThrows(DecisionLifecycleException.class, () -> decisionManager.createOrUpdateVersion(DEFAULT_CUSTOMER_ID, apiRequest));

        assertThat(thrown.getMessage(), equalTo("A lifecycle operation is already in progress for Version '1' of Decision 'robs-first-decision'"));
    }

    @TestTransaction
    @Test
    public void createNewVersion_updateDecision() {
        createStorageRequest();
        DecisionRequest apiRequest = createApiRequest();

        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(DEFAULT_CUSTOMER_ID, apiRequest);
        Decision decision = decisionVersion.getDecision();

        decisionManager.deployed(decision.getCustomerId(), decision.getName(), decisionVersion.getVersion(), createDeployment());

        apiRequest.setDescription("An updated version!");
        apiRequest.getModel().setDmn("Updated dmn!");

        decisionVersion = decisionManager.createOrUpdateVersion(DEFAULT_CUSTOMER_ID, apiRequest);
        assertThat(decisionVersion.getStatus(), equalTo(DecisionVersionStatus.BUILDING));
        assertThat(decisionVersion.getVersion(), equalTo(2L));

        decision = decisionVersion.getDecision();
        assertThat(decision.getCurrentVersion().getStatus(), equalTo(DecisionVersionStatus.CURRENT));
        assertThat(decision.getNextVersion().getStatus(), equalTo(DecisionVersionStatus.BUILDING));
    }

    @TestTransaction
    @Test
    public void createNewVersion_updateDecisionUpdateSuccess() {
        createStorageRequest();
        DecisionRequest apiRequest = createApiRequest();

        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(DEFAULT_CUSTOMER_ID, apiRequest);
        Decision decision = decisionVersion.getDecision();

        decisionManager.deployed(decision.getCustomerId(), decision.getName(), decisionVersion.getVersion(), createDeployment());

        apiRequest.setDescription("An updated version!");
        apiRequest.getModel().setDmn("fff");

        DecisionVersion nextVersion = decisionManager.createOrUpdateVersion(DEFAULT_CUSTOMER_ID, apiRequest);
        nextVersion = decisionManager.deployed(decision.getCustomerId(), decision.getName(), nextVersion.getVersion(), createDeployment());
        assertThat(nextVersion.getStatus(), equalTo(DecisionVersionStatus.CURRENT));
        assertThat(nextVersion.getDeployment().getVersionUrl(), is(notNullValue()));
        assertThat(nextVersion.getDeployment().getCurrentUrl(), is(notNullValue()));

        decision = nextVersion.getDecision();
        assertThat(decision.getCurrentVersion().getStatus(), equalTo(DecisionVersionStatus.CURRENT));
        assertThat(decision.getCurrentVersion().getVersion(), equalTo(nextVersion.getVersion()));
        assertThat(decision.getNextVersion(), is(nullValue()));

        decisionVersion = decisionVersionDAO.findById(decisionVersion.getId());
        assertThat(decisionVersion.getDeployment().getVersionUrl(), is(notNullValue()));
        assertThat(decisionVersion.getDeployment().getCurrentUrl(), is(notNullValue()));
    }

    @TestTransaction
    @Test
    public void createNewVersion_twoFailures() {
        createStorageRequest();
        DecisionRequest apiRequest = createApiRequest();

        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(DEFAULT_CUSTOMER_ID, apiRequest);
        Decision decision = decisionVersion.getDecision();
        decisionManager.failed(decision.getCustomerId(), decision.getName(), decisionVersion.getVersion(), createDeployment());

        apiRequest.setDescription("An updated version!");
        apiRequest.getModel().setDmn("fff");

        DecisionVersion failedVersion = decisionManager.createOrUpdateVersion(DEFAULT_CUSTOMER_ID, apiRequest);
        failedVersion = decisionManager.failed(decision.getCustomerId(), decision.getName(), failedVersion.getVersion(), createDeployment());
        assertThat(failedVersion.getStatus(), equalTo(DecisionVersionStatus.FAILED));

        decision = failedVersion.getDecision();
        assertThat(decision.getCurrentVersion().getStatus(), equalTo(DecisionVersionStatus.FAILED));
        assertThat(decision.getCurrentVersion().getVersion(), equalTo(failedVersion.getVersion()));
        assertThat(decision.getNextVersion(), is(nullValue()));
    }

    @TestTransaction
    @Test
    public void createNewVersion_updateDecisionUpdateFailed() {
        createStorageRequest();
        DecisionRequest apiRequest = createApiRequest();

        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(DEFAULT_CUSTOMER_ID, apiRequest);
        Decision decision = decisionVersion.getDecision();
        decisionManager.deployed(decision.getCustomerId(), decision.getName(), decisionVersion.getVersion(), createDeployment());

        apiRequest.setDescription("An updated version!");
        apiRequest.getModel().setDmn("fff");

        DecisionVersion failedVersion = decisionManager.createOrUpdateVersion(DEFAULT_CUSTOMER_ID, apiRequest);
        failedVersion = decisionManager.failed(decision.getCustomerId(), decision.getName(), failedVersion.getVersion(), createDeployment());
        assertThat(failedVersion.getStatus(), equalTo(DecisionVersionStatus.FAILED));

        decision = failedVersion.getDecision();
        assertThat(decision.getCurrentVersion().getStatus(), equalTo(DecisionVersionStatus.CURRENT));
        assertThat(decision.getCurrentVersion().getVersion(), equalTo(decisionVersion.getVersion()));
        assertThat(decision.getNextVersion(), is(nullValue()));
    }

    @Test
    public void deleteDecision() {
        createStorageRequest();
        DecisionRequest apiRequest = createApiRequest();

        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(DEFAULT_CUSTOMER_ID, apiRequest);
        Decision decision = decisionVersion.getDecision();

        decisionVersion = decisionManager.deployed(decision.getCustomerId(), decision.getName(), decisionVersion.getVersion(), createDeployment());
        decisionManager.deleteDecision(decision.getCustomerId(), decision.getName());

        assertThat(decisionDAO.findById(decisionVersion.getId()), is(nullValue()));
        assertThat(decisionVersionDAO.findById(decisionVersion.getId()), is(nullValue()));
    }

    @Test
    public void deleteDecision_decisionDoesNotExist() {
        assertThrows(NoSuchDecisionException.class, () -> {
            decisionManager.deleteDecision(DEFAULT_CUSTOMER_ID, "foo");
        });
    }

    @TestTransaction
    @Test
    public void deleteVersion_versionDoesNotExist() {
        createStorageRequest();
        DecisionRequest apiRequest = createApiRequest();

        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(DEFAULT_CUSTOMER_ID, apiRequest);
        Decision decision = decisionVersion.getDecision();

        assertThrows(NoSuchDecisionVersionException.class, () -> decisionManager.deleteVersion(DEFAULT_CUSTOMER_ID, decision.getName(), decisionVersion.getVersion() + 1L));
    }

    @TestTransaction
    @Test
    public void deleteVersion() {
        createStorageRequest();
        DecisionRequest apiRequest = createApiRequest();

        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(DEFAULT_CUSTOMER_ID, apiRequest);
        Decision decision = decisionVersion.getDecision();
        decisionManager.deployed(DEFAULT_CUSTOMER_ID, decision.getName(), decisionVersion.getVersion(), createDeployment());

        apiRequest.getModel().setDmn("updated dmn!");

        decisionVersion = decisionManager.createOrUpdateVersion(DEFAULT_CUSTOMER_ID, apiRequest);
        decisionVersion = decisionManager.deployed(DEFAULT_CUSTOMER_ID, decision.getName(), decisionVersion.getVersion(), createDeployment());
        decision = decisionVersion.getDecision();

        assertThat(decision.getCurrentVersion().getVersion(), equalTo(2L));
        assertThat(decision.getCurrentVersion().getStatus(), equalTo(DecisionVersionStatus.CURRENT));

        DecisionVersion deletedVersion = decisionManager.deleteVersion(DEFAULT_CUSTOMER_ID, decision.getName(), 1L);
        assertThat(deletedVersion.getStatus(), equalTo(DecisionVersionStatus.DELETED));
    }

    @Test
    @TestTransaction
    public void deleteVersion_versionIsCurrentVersion() {
        createStorageRequest();
        DecisionRequest apiRequest = createApiRequest();

        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(DEFAULT_CUSTOMER_ID, apiRequest);
        Decision decision = decisionVersion.getDecision();

        DecisionVersion deployed = decisionManager.deployed(decision.getCustomerId(), decision.getName(), decisionVersion.getVersion(), createDeployment());
        assertThrows(DecisionLifecycleException.class, () -> decisionManager.deleteVersion(DEFAULT_CUSTOMER_ID, decision.getName(), deployed.getVersion()));
    }

    @Test
    @TestTransaction
    public void deleteVersion_canDeleteAFailedVersion() {
        createStorageRequest();
        DecisionRequest apiRequest = createApiRequest();

        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(DEFAULT_CUSTOMER_ID, apiRequest);
        Decision decision = decisionVersion.getDecision();

        decisionVersion = decisionManager.failed(DEFAULT_CUSTOMER_ID, decision.getName(), decisionVersion.getVersion(), createDeployment());

        decisionVersion = decisionManager.deleteVersion(DEFAULT_CUSTOMER_ID, decision.getName(), decisionVersion.getVersion());
        assertThat(decisionVersion.getStatus(), equalTo(DecisionVersionStatus.DELETED));
        assertThat(decisionVersion.getDecision().getCurrentVersion().getStatus(), equalTo(DecisionVersionStatus.DELETED));
    }

    @TestTransaction
    @Test
    public void listDecisionVersions_withDecisionId() {
        createStorageRequest();
        DecisionRequest apiRequest = createApiRequest();
        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(DEFAULT_CUSTOMER_ID, apiRequest);

        ListResult<DecisionVersion> versions = decisionManager.listDecisionVersions(DEFAULT_CUSTOMER_ID, decisionVersion.getDecision().getId(), 0, 100);
        assertThat(versions.getSize(), equalTo(1L));
        assertThat(versions.getItems().get(0).getId(), equalTo(decisionVersion.getId()));
        assertThat(versions.getTotal(), equalTo(1L));
    }

    @TestTransaction
    @Test
    public void getVersion_decisionDoesNotExist() {
        NoSuchDecisionException thrown = assertThrows(NoSuchDecisionException.class, () -> {
            decisionManager.getVersion(DEFAULT_CUSTOMER_ID, "foo", 1L);
        });

        assertThat(thrown.getMessage(), equalTo("Decision with id or name 'foo' does not exist for customer 'customer-id'"));
    }

    @TestTransaction
    @Test
    public void getVersion_versionDoesNotExist() {
        createStorageRequest();
        DecisionRequest apiRequest = createApiRequest();
        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(DEFAULT_CUSTOMER_ID, apiRequest);

        assertThrows(NoSuchDecisionVersionException.class, () -> {
            decisionManager.getVersion(DEFAULT_CUSTOMER_ID, decisionVersion.getDecision().getId(), decisionVersion.getVersion() + 1L);
        });
    }

    @TestTransaction
    @Test
    public void getVersion_byDecisionId() {
        createStorageRequest();
        DecisionRequest apiRequest = createApiRequest();
        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(DEFAULT_CUSTOMER_ID, apiRequest);

        DecisionVersion found = decisionManager.getVersion(DEFAULT_CUSTOMER_ID, decisionVersion.getDecision().getId(), decisionVersion.getVersion());
        assertThat(found.getId(), equalTo(decisionVersion.getId()));
    }

    @TestTransaction
    @Test
    public void getVersion_byDecisionName() {
        createStorageRequest();
        DecisionRequest apiRequest = createApiRequest();
        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(DEFAULT_CUSTOMER_ID, apiRequest);

        DecisionVersion found = decisionManager.getVersion(DEFAULT_CUSTOMER_ID, decisionVersion.getDecision().getName(), decisionVersion.getVersion());
        assertThat(found.getId(), equalTo(decisionVersion.getId()));
    }

    @TestTransaction
    @Test
    public void getCurrentVersion_byDecisionName() {
        createStorageRequest();
        DecisionRequest apiRequest = createApiRequest();
        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(DEFAULT_CUSTOMER_ID, apiRequest);

        DecisionVersion found = decisionManager.getCurrentVersion(DEFAULT_CUSTOMER_ID, decisionVersion.getDecision().getName());
        assertThat(found.getId(), equalTo(decisionVersion.getId()));
    }

    @TestTransaction
    @Test
    public void getCurrentVersion_byDecisionId() {
        createStorageRequest();
        DecisionRequest apiRequest = createApiRequest();
        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(DEFAULT_CUSTOMER_ID, apiRequest);

        DecisionVersion found = decisionManager.getCurrentVersion(DEFAULT_CUSTOMER_ID, decisionVersion.getDecision().getId());
        assertThat(found.getId(), equalTo(decisionVersion.getId()));
    }

    @Test
    @TestTransaction
    public void getBuildingVersion_byDecisionId() {
        createStorageRequest();
        DecisionRequest apiRequest = createApiRequest();
        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(DEFAULT_CUSTOMER_ID, apiRequest);

        DecisionVersion found = decisionManager.getBuildingVersion(DEFAULT_CUSTOMER_ID, decisionVersion.getDecision().getId());
        assertThat(found.getId(), equalTo(decisionVersion.getId()));
    }

    @Test
    @TestTransaction
    public void getBuildingVersion_byDecisionName() {
        createStorageRequest();
        DecisionRequest apiRequest = createApiRequest();
        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(DEFAULT_CUSTOMER_ID, apiRequest);

        DecisionVersion found = decisionManager.getBuildingVersion(DEFAULT_CUSTOMER_ID, decisionVersion.getDecision().getName());
        assertThat(found.getId(), equalTo(decisionVersion.getId()));
    }

    @Test
    @TestTransaction
    public void getBuildingVersion_noBuildingVersion() {
        createStorageRequest();
        DecisionRequest apiRequest = createApiRequest();
        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(DEFAULT_CUSTOMER_ID, apiRequest);
        decisionManager.deployed(DEFAULT_CUSTOMER_ID, decisionVersion.getDecision().getId(), decisionVersion.getVersion(), createDeployment());

        assertThrows(NoSuchDecisionVersionException.class, () -> {
            decisionManager.getBuildingVersion(DEFAULT_CUSTOMER_ID, decisionVersion.getDecision().getName());
        });
    }

    @Test
    public void getCurrentVersion_decisionDoesNotExist() {

        NoSuchDecisionException thrown = assertThrows(NoSuchDecisionException.class, () -> {
            decisionManager.getCurrentVersion(DEFAULT_CUSTOMER_ID, "foo");
        });

        assertThat(thrown.getMessage(), equalTo("Decision with id or name 'foo' does not exist for customer 'customer-id'"));
    }

    @TestTransaction
    @Test
    public void listDecisionVersions_withDecisionName() {
        createStorageRequest();
        DecisionRequest apiRequest = createApiRequest();
        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(DEFAULT_CUSTOMER_ID, apiRequest);

        ListResult<DecisionVersion> versions = decisionManager.listDecisionVersions(DEFAULT_CUSTOMER_ID, decisionVersion.getDecision().getName(), 0, 100);
        assertThat(versions.getSize(), equalTo(1L));
        assertThat(versions.getItems().get(0).getId(), equalTo(decisionVersion.getId()));
        assertThat(versions.getTotal(), equalTo(1L));
    }

    @TestTransaction
    @Test
    public void listDecisionVersions_noSuchIdOrName() {

        NoSuchDecisionException thrown = assertThrows(NoSuchDecisionException.class, () -> {
            decisionManager.listDecisionVersions(DEFAULT_CUSTOMER_ID, "foo", 0, 100);
        });

        assertThat(thrown.getMessage(), equalTo("Decision with id or name 'foo' does not exist for customer 'customer-id'"));
    }

    @TestTransaction
    @Test
    public void listDecisions() {
        createStorageRequest();

        DecisionRequest apiRequest = createApiRequest();
        DecisionRequest apiRequest2 = createApiRequest();
        apiRequest2.setName("another-decision");

        DecisionRequest apiRequest3 = createApiRequest();
        apiRequest3.setName("yet-another-decision");

        Map<String, String> config = new HashMap<>();
        config.put("configKey", "configValue");
        apiRequest.setConfiguration(config);

        Map<String, String> tags = new HashMap<>();
        tags.put("tagKey", "tagValue");
        apiRequest2.setTags(tags);

        decisionManager.createOrUpdateVersion(DEFAULT_CUSTOMER_ID, apiRequest);
        decisionManager.createOrUpdateVersion(DEFAULT_CUSTOMER_ID, apiRequest2);
        decisionManager.createOrUpdateVersion(DEFAULT_CUSTOMER_ID, apiRequest3);

        ListResult<Decision> decisions = decisionManager.listDecisions(DEFAULT_CUSTOMER_ID, 0, 100);
        assertThat(decisions.getTotal(), equalTo(3L));
        assertThat(decisions.getSize(), equalTo(3L));

        Decision found = decisions.getItems().get(0);
        assertThat(found.getName(), equalTo(apiRequest2.getName()));
        assertThat(found.getCurrentVersion().getConfiguration(), is(anEmptyMap()));
        assertThat(found.getCurrentVersion().getTags().containsKey("tagKey"), is(true));

        found = decisions.getItems().get(1);
        assertThat(found.getName(), equalTo(apiRequest.getName()));
        assertThat(found.getCurrentVersion().getConfiguration().containsKey("configKey"), is(true));
        assertThat(found.getCurrentVersion().getTags(), is(anEmptyMap()));

        found = decisions.getItems().get(2);
        assertThat(found.getName(), equalTo(apiRequest3.getName()));
        assertThat(found.getCurrentVersion().getConfiguration(), is(anEmptyMap()));
        assertThat(found.getCurrentVersion().getTags(), is(anEmptyMap()));
    }

    @TestTransaction
    @Test
    public void listDecisions_withPaging() {

        createStorageRequest();

        DecisionRequest apiRequest = createApiRequest();
        DecisionRequest apiRequest2 = createApiRequest();
        apiRequest2.setName("another-decision");

        DecisionRequest apiRequest3 = createApiRequest();
        apiRequest3.setName("yet-another-decision");

        decisionManager.createOrUpdateVersion(DEFAULT_CUSTOMER_ID, apiRequest);
        decisionManager.createOrUpdateVersion(DEFAULT_CUSTOMER_ID, apiRequest2);
        decisionManager.createOrUpdateVersion(DEFAULT_CUSTOMER_ID, apiRequest3);

        ListResult<Decision> decisions = decisionManager.listDecisions(DEFAULT_CUSTOMER_ID, 2, 1);
        assertThat(decisions.getTotal(), equalTo(3L));
        assertThat(decisions.getSize(), equalTo(1L));

        Decision found = decisions.getItems().get(0);
        assertThat(found.getName(), equalTo(apiRequest3.getName()));
    }

    @TestTransaction
    @Test
    public void newDecision_decisionDoesNotExist() {
        assertThrows(NoSuchDecisionException.class, () -> {
            decisionManager.setCurrentVersion(DEFAULT_CUSTOMER_ID, "foo", 1);
        });
    }

    @TestTransaction
    @Test
    public void newDecision_decisionVersionDoesNotExist() {
        createStorageRequest();

        DecisionRequest apiRequest = createApiRequest();
        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(DEFAULT_CUSTOMER_ID, apiRequest);

        assertThrows(NoSuchDecisionVersionException.class, () -> {
            decisionManager.setCurrentVersion(DEFAULT_CUSTOMER_ID, decisionVersion.getDecision().getName(), decisionVersion.getVersion() + 1L);
        });
    }

    @Test
    @TestTransaction
    public void newDecision_decisionVersionNotInReadyState() {
        createStorageRequest();

        DecisionRequest apiRequest = createApiRequest();
        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(DEFAULT_CUSTOMER_ID, apiRequest);

        DecisionVersion deployed = decisionManager.deployed(DEFAULT_CUSTOMER_ID, decisionVersion.getDecision().getName(), decisionVersion.getVersion(), createDeployment());

        assertThrows(DecisionLifecycleException.class, () -> decisionManager.setCurrentVersion(DEFAULT_CUSTOMER_ID, deployed.getDecision().getId(), deployed.getVersion()));
    }

    @TestTransaction
    @Test
    public void newDecision_lifecycleOperationAlreadyInProgress() {
        createStorageRequest();

        DecisionRequest apiRequest = createApiRequest();
        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(DEFAULT_CUSTOMER_ID, apiRequest);

        decisionManager.deployed(DEFAULT_CUSTOMER_ID, decisionVersion.getDecision().getName(), decisionVersion.getVersion(), createDeployment());

        apiRequest = createApiRequest();
        apiRequest.getModel().setDmn("Updated dmn!");

        decisionVersion = decisionManager.createOrUpdateVersion(DEFAULT_CUSTOMER_ID, apiRequest);
        decisionManager.deployed(DEFAULT_CUSTOMER_ID, decisionVersion.getDecision().getName(), decisionVersion.getVersion(), createDeployment());

        apiRequest = createApiRequest();
        apiRequest.getModel().setDmn("Updated dmn again!");

        decisionVersion = decisionManager.createOrUpdateVersion(DEFAULT_CUSTOMER_ID, apiRequest);

        DecisionVersion firstVersion = decisionManager.getVersion(DEFAULT_CUSTOMER_ID, decisionVersion.getDecision().getId(), 1L);
        assertThat(firstVersion.getStatus(), equalTo(DecisionVersionStatus.READY));

        assertThrows(DecisionLifecycleException.class, () -> decisionManager.setCurrentVersion(DEFAULT_CUSTOMER_ID, firstVersion.getDecision().getId(), firstVersion.getVersion()));
    }

    @TestTransaction
    @Test
    public void newVersion() {
        createStorageRequest();

        DecisionRequest apiRequest = createApiRequest();
        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(DEFAULT_CUSTOMER_ID, apiRequest);

        decisionManager.deployed(DEFAULT_CUSTOMER_ID, decisionVersion.getDecision().getName(), decisionVersion.getVersion(), createDeployment());

        apiRequest = createApiRequest();
        apiRequest.getModel().setDmn("Updated dmn!");

        decisionVersion = decisionManager.createOrUpdateVersion(DEFAULT_CUSTOMER_ID, apiRequest);
        decisionManager.deployed(DEFAULT_CUSTOMER_ID, decisionVersion.getDecision().getName(), decisionVersion.getVersion(), createDeployment());

        apiRequest = createApiRequest();
        apiRequest.getModel().setDmn("Updated dmn again!");

        decisionVersion = decisionManager.createOrUpdateVersion(DEFAULT_CUSTOMER_ID, apiRequest);
        decisionVersion = decisionManager.deployed(DEFAULT_CUSTOMER_ID, decisionVersion.getDecision().getName(), decisionVersion.getVersion(), createDeployment());

        DecisionVersion firstVersion = decisionManager.getVersion(DEFAULT_CUSTOMER_ID, decisionVersion.getDecision().getId(), 1L);
        assertThat(firstVersion.getStatus(), equalTo(DecisionVersionStatus.READY));

        DecisionVersion newVersion = decisionManager.setCurrentVersion(DEFAULT_CUSTOMER_ID, firstVersion.getDecision().getId(), firstVersion.getVersion());
        assertThat(newVersion.getStatus(), equalTo(DecisionVersionStatus.BUILDING));
        assertThat(newVersion.getDecision().getNextVersion().getVersion(), equalTo(newVersion.getVersion()));
        assertThat(newVersion.getDecision().getCurrentVersion().getVersion(), equalTo(decisionVersion.getVersion()));
        assertThat(newVersion.getDecision().getCurrentVersion().getStatus(), equalTo(DecisionVersionStatus.CURRENT));
    }
}
