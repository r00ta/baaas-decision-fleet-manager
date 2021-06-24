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

import java.io.ByteArrayOutputStream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.kie.baaas.mcp.api.decisions.DecisionRequest;
import org.kie.baaas.mcp.app.ccp.ClusterControlPlaneClient;
import org.kie.baaas.mcp.app.ccp.ClusterControlPlaneSelector;
import org.kie.baaas.mcp.app.ccp.client.ClusterControlPlaneClientFactory;
import org.kie.baaas.mcp.app.controller.modelmappers.DecisionMapper;
import org.kie.baaas.mcp.app.event.AfterDeployedEvent;
import org.kie.baaas.mcp.app.event.AfterFailedEvent;
import org.kie.baaas.mcp.app.event.BeforeCreateOrUpdateVersionEvent;
import org.kie.baaas.mcp.app.exceptions.MasterControlPlaneException;
import org.kie.baaas.mcp.app.listener.ListenerManager;
import org.kie.baaas.mcp.app.managedservices.ManagedServicesClient;
import org.kie.baaas.mcp.app.model.ClusterControlPlane;
import org.kie.baaas.mcp.app.model.Decision;
import org.kie.baaas.mcp.app.model.DecisionVersion;
import org.kie.baaas.mcp.app.model.ListResult;
import org.kie.baaas.mcp.app.model.deployment.Deployment;
import org.kie.baaas.mcp.app.model.eventing.Credential;
import org.kie.baaas.mcp.app.storage.DecisionDMNStorage;
import org.kie.baaas.mcp.app.vault.Secret;
import org.kie.baaas.mcp.app.vault.VaultService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.kie.baaas.mcp.app.managedservices.ManagedServicesClient.CLIENT_ID;
import static org.kie.baaas.mcp.app.managedservices.ManagedServicesClient.CLIENT_SECRET;

/**
 * This class orchestrates a number of internal components to manage the lifecycle of a Decision.
 * The primary purpose of this class is to ensure that we respect data integrity with regards
 * to Decision Versioning by not allowing transactions to span long-running operations.
 */
@ApplicationScoped
public class DecisionLifecycleOrchestrator implements DecisionLifecycle {

    private static final Logger LOG = LoggerFactory.getLogger(DecisionLifecycleOrchestrator.class);
    private static final String CREDENTIALS_NAME = "daas-%s-credentials";

    private final ClusterControlPlaneClientFactory clientFactory;

    private final ClusterControlPlaneSelector controlPlaneSelector;

    private final DecisionManager decisionManager;

    private final DecisionDMNStorage decisionDMNStorage;

    private final ListenerManager listenerManager;

    private final DecisionMapper decisionMapper;

    private final VaultService vaultService;

    private final ManagedServicesClient managedServicesClient;

    @Inject
    public DecisionLifecycleOrchestrator(ClusterControlPlaneClientFactory clientFactory, ClusterControlPlaneSelector controlPlaneSelector, DecisionManager decisionManager,
            DecisionDMNStorage decisionDMNStorage,
            ListenerManager listenerManager,
            DecisionMapper decisionMapper,
            VaultService vaultService,
            ManagedServicesClient managedServicesClient) {
        this.clientFactory = clientFactory;
        this.controlPlaneSelector = controlPlaneSelector;
        this.decisionManager = decisionManager;
        this.decisionDMNStorage = decisionDMNStorage;
        this.listenerManager = listenerManager;
        this.decisionMapper = decisionMapper;
        this.vaultService = vaultService;
        this.managedServicesClient = managedServicesClient;
    }

    @Override
    public Decision deleteDecision(String customerId, String decisionNameOrId) {

        Decision decision = decisionManager.deleteDecision(customerId, decisionNameOrId);
        ClusterControlPlaneClient client = getControlPlaneClient(decision);
        client.delete(decision);
        decisionDMNStorage.deleteDMN(customerId, decision);
        return decision;
    }

    private DecisionVersion requestDeployment(String customerId, DecisionVersion decisionVersion) {
        ClusterControlPlaneClient client = getControlPlaneClient(decisionVersion.getDecision());
        try {
            client.deploy(decisionVersion);
            return decisionVersion;
        } catch (Exception e) {
            decisionManager.failed(customerId, decisionVersion.getDecision().getId(), decisionVersion.getVersion(), failedToDeploy());
            throw failedToDeploy(customerId, decisionVersion, e);
        }
    }

    private MasterControlPlaneException failedToDeploy(String customerId, DecisionVersion decisionVersion, Throwable t) {
        String message = new StringBuilder("Failed to request deployment of Decision with id '")
                .append(decisionVersion.getDecision().getId())
                .append("' at version '")
                .append(decisionVersion.getVersion())
                .append("' for customer '")
                .append(customerId)
                .append("'").toString();
        return new MasterControlPlaneException(message, t);
    }

    @Override
    public DecisionVersion createOrUpdateVersion(String customerId, DecisionRequest decisionRequest) {
        listenerManager.notifyListeners(() -> new BeforeCreateOrUpdateVersionEvent(decisionRequest));
        // TODO - chicken and egg problem here.  The DecisionVersion requires information about the DMN
        // storage location, but the storage requires the DecisionVersion. We therefore have the write
        // to storage happening within the DecisionManager implementation. Ideally the write should happen
        // outside of this as we don't want to write to remote resources within a transaction boundary.
        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(customerId, decisionRequest);

        if (decisionVersion.getKafkaConfig() != null) {
            Credential credential = getCustomerCredential(customerId, decisionRequest);
            decisionVersion.getKafkaConfig().setCredential(credential);
        }
        return requestDeployment(customerId, decisionVersion);
    }

    private Deployment failedToDeploy() {
        Deployment deployment = new Deployment();
        deployment.setStatusMessage("Failed to deploy Decision.");
        return deployment;
    }

    private ClusterControlPlaneClient getControlPlaneClient(Decision decision) {
        ClusterControlPlane clusterControlPlane = controlPlaneSelector.selectControlPlaneForDeployment(decision);
        return clientFactory.createClientFor(clusterControlPlane);
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
        ClusterControlPlaneClient client = getControlPlaneClient(decisionVersion.getDecision());
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

    public DecisionVersion failed(String customerId, String decisionIdOrName, long version, Deployment deployment) {
        DecisionVersion decisionVersion = decisionManager.failed(customerId, decisionIdOrName, version, deployment);
        listenerManager.notifyListeners(() -> new AfterFailedEvent(decisionMapper.mapVersionToDecisionResponse(decisionVersion)));
        return decisionVersion;
    }

    public DecisionVersion deployed(String customerId, String decisionIdOrName, long version, Deployment deployment) {
        DecisionVersion decisionVersion = decisionManager.deployed(customerId, decisionIdOrName, version, deployment);
        listenerManager.notifyListeners(() -> new AfterDeployedEvent(decisionMapper.mapVersionToDecisionResponse(decisionVersion)));
        return decisionVersion;
    }

    private Credential getCustomerCredential(String customerId, DecisionRequest decisionRequest) {
        if (decisionRequest.getEventing() != null) {
            String secretName = String.format(CREDENTIALS_NAME, customerId);
            Secret secret = vaultService.get(secretName);
            if (secret == null) {
                secret = managedServicesClient.createOrReplaceServiceAccount(secretName);
                vaultService.create(secret);
            }
            return toCredential(secret);
        }
        return null;
    }

    private Credential toCredential(Secret secret) {
        if (secret == null || secret.getValues() == null) {
            return null;
        }
        return new Credential()
                .setClientId(secret.getValues().get(CLIENT_ID))
                .setClientSecret(secret.getValues().get(CLIENT_SECRET));

    }
}
