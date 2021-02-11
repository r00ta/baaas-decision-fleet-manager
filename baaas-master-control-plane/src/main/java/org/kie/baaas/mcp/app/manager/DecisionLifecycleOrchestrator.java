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

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.kie.baaas.mcp.api.decisions.DecisionRequest;
import org.kie.baaas.mcp.app.ccp.ClusterControlPlaneClient;
import org.kie.baaas.mcp.app.ccp.ClusterControlPlaneSelector;
import org.kie.baaas.mcp.app.ccp.client.ClusterControlPlaneClientFactory;
import org.kie.baaas.mcp.app.exceptions.MasterControlPlaneException;
import org.kie.baaas.mcp.app.model.ClusterControlPlane;
import org.kie.baaas.mcp.app.model.Decision;
import org.kie.baaas.mcp.app.model.DecisionVersion;
import org.kie.baaas.mcp.app.model.deployment.Deployment;
import org.kie.baaas.mcp.app.storage.DecisionDMNStorage;

/**
 * This class orchestrates a number of internal components to manage the lifecycle of a Decision.
 * The primary purpose of this class is to ensure that we respect data integrity with regards
 * to Decision Versioning by not allowing transactions to span long-running operations.
 */
@ApplicationScoped
public class DecisionLifecycleOrchestrator implements DecisionLifecycle {

    private ClusterControlPlaneClientFactory clientFactory;

    private ClusterControlPlaneSelector controlPlaneSelector;

    private DecisionManager decisionManager;

    private DecisionDMNStorage decisionDMNStorage;

    @Inject
    public DecisionLifecycleOrchestrator(ClusterControlPlaneClientFactory clientFactory, ClusterControlPlaneSelector controlPlaneSelector, DecisionManager decisionManager, DecisionDMNStorage decisionDMNStorage) {
        this.clientFactory = clientFactory;
        this.controlPlaneSelector = controlPlaneSelector;
        this.decisionManager = decisionManager;
        this.decisionDMNStorage = decisionDMNStorage;
    }

    @Override
    public Decision deleteDecision(String customerId, String decisionNameOrId) {

        Decision decision = decisionManager.deleteDecision(customerId, decisionNameOrId);
        ClusterControlPlaneClient client = getControlPlaneClient(decision);
        client.delete(decision);
        decisionDMNStorage.deleteDMN(customerId, decisionNameOrId);

        return decision;
    }

    @Override
    public DecisionVersion createOrUpdateVersion(String customerId, DecisionRequest decisionRequest) {

        // TODO - chicken and egg problem here.  The DecisionVersion requires information about the DMN
        // storage location, but the storage requires the DecisionVersion.
        DecisionVersion decisionVersion = decisionManager.createOrUpdateVersion(customerId, decisionRequest);
        ClusterControlPlaneClient client = getControlPlaneClient(decisionVersion.getDecision());
        try {
            client.deploy(decisionVersion);
            return decisionVersion;
        } catch (Exception e) {
            decisionManager.failed(customerId, decisionVersion.getDecision().getId(), decisionVersion.getVersion(), failedToDeploy());
            throw new MasterControlPlaneException("Failed to request deployment of Decision to control plane", e);
        }
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
    public DecisionVersion rollbackToVersion(String customerId, String decisionIdOrName, long version) {
        DecisionVersion decisionVersion = decisionManager.rollbackToVersion(customerId, decisionIdOrName, version);
        ClusterControlPlaneClient client = getControlPlaneClient(decisionVersion.getDecision());
        try {
            client.rollback(decisionVersion);
            return decisionVersion;
        } catch (Exception e) {
            decisionManager.failed(customerId, decisionVersion.getDecision().getId(), decisionVersion.getVersion(), failedToDeploy());
            throw new MasterControlPlaneException("Failed to request rollback for Decision '" + decisionVersion.getDecision().getName() + "' on Control Plane '" + client.getClusterControlPlane().getKubernetesApiUrl() + "'", e);
        }
    }

    @Override
    public List<Decision> listDecisions(String customerId) {
        return decisionManager.listDecisions(customerId);
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
    public List<DecisionVersion> listDecisionVersions(String customerId, String decisionIdOrName) {
        return decisionManager.listDecisionVersions(customerId, decisionIdOrName);
    }
}
