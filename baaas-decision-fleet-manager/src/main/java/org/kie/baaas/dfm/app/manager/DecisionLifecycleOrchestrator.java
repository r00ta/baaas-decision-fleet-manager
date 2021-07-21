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

import java.io.ByteArrayOutputStream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.kie.baaas.dfm.api.decisions.DecisionRequest;
import org.kie.baaas.dfm.api.eventing.Eventing;
import org.kie.baaas.dfm.app.controller.modelmappers.DecisionMapper;
import org.kie.baaas.dfm.app.dfs.DecisionFleetShardClient;
import org.kie.baaas.dfm.app.dfs.DecisionFleetShardSelector;
import org.kie.baaas.dfm.app.dfs.client.DecisionFleetShardClientFactory;
import org.kie.baaas.dfm.app.event.AfterDeployedEvent;
import org.kie.baaas.dfm.app.event.AfterFailedEvent;
import org.kie.baaas.dfm.app.event.BeforeCreateOrUpdateVersionEvent;
import org.kie.baaas.dfm.app.exceptions.DecisionFleetManagerException;
import org.kie.baaas.dfm.app.listener.ListenerManager;
import org.kie.baaas.dfm.app.manager.kafkaservice.KafkaServiceNotSupportedException;
import org.kie.baaas.dfm.app.manager.kafkaservice.KafkaServiceProducer;
import org.kie.baaas.dfm.app.model.Decision;
import org.kie.baaas.dfm.app.model.DecisionFleetShard;
import org.kie.baaas.dfm.app.model.DecisionVersion;
import org.kie.baaas.dfm.app.model.ListResult;
import org.kie.baaas.dfm.app.model.deployment.Deployment;
import org.kie.baaas.dfm.app.model.eventing.Credential;
import org.kie.baaas.dfm.app.storage.DecisionDMNStorage;

/**
 * This class orchestrates a number of internal components to manage the lifecycle of a Decision.
 * The primary purpose of this class is to ensure that we respect data integrity with regards
 * to Decision Versioning by not allowing transactions to span long-running operations.
 */
@ApplicationScoped
public class DecisionLifecycleOrchestrator implements DecisionLifecycle {

    private final DecisionFleetShardClientFactory clientFactory;

    private final DecisionFleetShardSelector fleetShardSelector;

    private final DecisionManager decisionManager;

    private final DecisionDMNStorage decisionDMNStorage;

    private final ListenerManager listenerManager;

    private final DecisionMapper decisionMapper;

    private final KafkaServiceProducer kafkaServiceProducer;

    private final KafkaService kafkaService;

    @Inject
    public DecisionLifecycleOrchestrator(DecisionFleetShardClientFactory clientFactory, DecisionFleetShardSelector fleetShardSelector, DecisionManager decisionManager,
            DecisionDMNStorage decisionDMNStorage,
            ListenerManager listenerManager,
            DecisionMapper decisionMapper, KafkaService kafkaService,
            KafkaServiceProducer kafkaServiceProducer) {
        this.clientFactory = clientFactory;
        this.fleetShardSelector = fleetShardSelector;
        this.decisionManager = decisionManager;
        this.decisionDMNStorage = decisionDMNStorage;
        this.listenerManager = listenerManager;
        this.decisionMapper = decisionMapper;
        this.kafkaService = kafkaService;
        this.kafkaServiceProducer = kafkaServiceProducer;
    }

    @Override
    public Decision deleteDecision(String customerId, String decisionNameOrId) {

        Decision decision = decisionManager.deleteDecision(customerId, decisionNameOrId);
        DecisionFleetShardClient client = getFleetShardClient(decision);
        client.delete(decision);
        decisionDMNStorage.deleteDMN(customerId, decision);
        return decision;
    }

    @Override
    public DecisionVersion createOrUpdateVersion(String customerId, DecisionRequest decisionRequest) {
        Eventing evt = decisionRequest.getEventing();
        if (evt != null && evt.getKafka() != null && !kafkaServiceProducer.isKafkaServiceDefined()) {
            throw new KafkaServiceNotSupportedException("Kafka service is not supported in this environment.");
        }

        listenerManager.notifyListeners(() -> new BeforeCreateOrUpdateVersionEvent(decisionRequest));
        // TODO - chicken and egg problem here.  The DecisionVersion requires information about the DMN
        // storage location, but the storage requires the DecisionVersion. We therefore have the write
        // to storage happening within the DecisionManager implementation. Ideally the write should happen
        // outside of this as we don't want to write to remote resources within a transaction boundary.
        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(customerId, decisionRequest);

        if (decisionVersion.getKafkaConfig() != null) {
            Credential credential = kafkaService.getCustomerCredential(customerId);
            decisionVersion.getKafkaConfig().setCredential(credential);
        }

        return requestDeployment(customerId, decisionVersion);
    }

    @Override
    public DecisionVersion setCurrentVersion(String customerId, String decisionIdOrName, long version) {
        DecisionVersion decisionVersion = decisionManager.setCurrentVersion(customerId, decisionIdOrName, version);
        return requestDeployment(customerId, decisionVersion);
    }

    @Override
    public ListResult<Decision> listDecisions(String customerId, int page, int pageSize) {
        return decisionManager.listDecisions(customerId, page, pageSize);
    }

    @Override
    public DecisionVersion getBuildingVersion(String customerId, String decisionIdOrName) {
        return decisionManager.getBuildingVersion(customerId, decisionIdOrName);
    }

    @Override
    public DecisionVersion getVersion(String customerId, String decisionIdOrName, long decisionVersion) {
        return decisionManager.getVersion(customerId, decisionIdOrName, decisionVersion);
    }

    @Override
    public DecisionVersion deleteVersion(String customerId, String decisionIdOrName, long version) {
        DecisionVersion decisionVersion = decisionManager.deleteVersion(customerId, decisionIdOrName, version);
        DecisionFleetShardClient client = getFleetShardClient(decisionVersion.getDecision());
        client.delete(decisionVersion);
        return decisionVersion;
    }

    @Override
    public DecisionVersion getCurrentVersion(String customerId, String decisionIdOrName) {
        return decisionManager.getCurrentVersion(customerId, decisionIdOrName);
    }

    @Override
    public ListResult<DecisionVersion> listDecisionVersions(String customerId, String decisionIdOrName, int page, int pageSize) {
        return decisionManager.listDecisionVersions(customerId, decisionIdOrName, page, pageSize);
    }

    @Override
    public ByteArrayOutputStream getDMN(String customerId, String decisionIdOrName, long version) {
        return decisionManager.getDMN(customerId, decisionIdOrName, version);
    }

    @Override
    public DecisionVersion failed(String customerId, String decisionIdOrName, long version, Deployment deployment) {
        DecisionVersion decisionVersion = decisionManager.failed(customerId, decisionIdOrName, version, deployment);
        listenerManager.notifyListeners(() -> new AfterFailedEvent(decisionMapper.mapVersionToDecisionResponse(decisionVersion)));
        return decisionVersion;
    }

    @Override
    public DecisionVersion deployed(String customerId, String decisionIdOrName, long version, Deployment deployment) {
        DecisionVersion decisionVersion = decisionManager.deployed(customerId, decisionIdOrName, version, deployment);
        listenerManager.notifyListeners(() -> new AfterDeployedEvent(decisionMapper.mapVersionToDecisionResponse(decisionVersion)));
        return decisionVersion;
    }

    private DecisionVersion requestDeployment(String customerId, DecisionVersion decisionVersion) {
        DecisionFleetShardClient client = getFleetShardClient(decisionVersion.getDecision());
        try {
            client.deploy(decisionVersion);
            return decisionVersion;
        } catch (Exception e) {
            decisionManager.failed(customerId, decisionVersion.getDecision().getId(), decisionVersion.getVersion(), failedToDeploy());
            throw failedToDeploy(customerId, decisionVersion, e);
        }
    }

    private DecisionFleetManagerException failedToDeploy(String customerId, DecisionVersion decisionVersion, Throwable t) {
        String message = new StringBuilder("Failed to request deployment of Decision with id '")
                .append(decisionVersion.getDecision().getId())
                .append("' at version '")
                .append(decisionVersion.getVersion())
                .append("' for customer '")
                .append(customerId)
                .append("'").toString();
        return new DecisionFleetManagerException(message, t);
    }

    private Deployment failedToDeploy() {
        Deployment deployment = new Deployment();
        deployment.setStatusMessage("Failed to deploy Decision.");
        return deployment;
    }

    private DecisionFleetShardClient getFleetShardClient(Decision decision) {
        DecisionFleetShard fleetShard = fleetShardSelector.selectFleetShardForDeployment(decision);
        return clientFactory.createClientFor(fleetShard);
    }
}
