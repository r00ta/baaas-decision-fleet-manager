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

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.kie.baaas.mcp.api.decisions.DecisionRequest;
import org.kie.baaas.mcp.api.eventing.kafka.Kafka;
import org.kie.baaas.mcp.app.dao.DecisionDAO;
import org.kie.baaas.mcp.app.dao.DecisionVersionDAO;
import org.kie.baaas.mcp.app.model.Decision;
import org.kie.baaas.mcp.app.model.DecisionVersion;
import org.kie.baaas.mcp.app.model.DecisionVersionStatus;
import org.kie.baaas.mcp.app.model.deployment.Deployment;
import org.kie.baaas.mcp.app.model.eventing.KafkaTopics;
import org.kie.baaas.mcp.app.storage.DMNStorageRequest;
import org.kie.baaas.mcp.app.storage.DecisionDMNStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toList;

/**
 * Manages the core lifecycle for Decisions. Our API should ensure valid Payload and then
 * delegate to the DecisionManager to complete the lifecycle management.
 */
@ApplicationScoped
@Transactional
public class DecisionManager implements DecisionLifecycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(DecisionManager.class);

    private final DecisionDAO decisionDAO;

    private final DecisionVersionDAO decisionVersionDAO;

    private final DecisionDMNStorage decisionDMNStorage;

    @Inject
    public DecisionManager(DecisionDAO decisionDAO, DecisionVersionDAO decisionVersionDAO, DecisionDMNStorage decisionDMNStorage) {
        Objects.requireNonNull(decisionDAO, "decisionDAO cannot be null");
        Objects.requireNonNull(decisionDAO, "decisionVersionDAO cannot be null");
        this.decisionDAO = decisionDAO;
        this.decisionVersionDAO = decisionVersionDAO;
        this.decisionDMNStorage = decisionDMNStorage;
    }

    /*
        Checks to see that we are not already performing a lifecycle operation for this Decision.
        Currently we only support sequential lifecycle operations.
     */
    private void checkForExistingLifecycleOperation(Decision decision) {
        if (decision.getNextVersion() != null) {
            if (DecisionVersionStatus.BUILDING.equals(decision.getNextVersion().getStatus())) {
                throw new DecisionLifecycleException("A lifecycle operation is already in progress for Version '" + decision.getCurrentVersion().getVersion() + "' of Decision '" + decision.getName() + "'");
            }
        }
    }

    private NoSuchDecisionVersionException decisionVersionDoesNotExist(String customerId, String idOrName, long version) {
        String message = String.format("Version '%s' of Decision with id or name '%s' does not exist for customer '%s", customerId, idOrName, version);
        throw new NoSuchDecisionVersionException(message);
    }

    private NoSuchDecisionException decisionDoesNotExist(String customerId, String idOrName) {
        String message = String.format("Decision with id or name '%s' does not exist for customer '%s'", idOrName, customerId);
        return new NoSuchDecisionException(message);
    }

    /**
     * Gets the specified version of the decision for the given customer.
     *
     * @param customerId       - The customer id
     * @param decisionIdOrName - The decision id or name
     * @param decisionVersion  - The version of the Decision
     * @return - The Decision Version
     */
    public DecisionVersion getVersion(String customerId, String decisionIdOrName, long decisionVersion) {
        return findDecisionVersion(customerId, decisionIdOrName, decisionVersion);
    }

    /**
     * Gets the current version of the decision for the given customer and decision id or name
     *
     * @param customerId       - the customer id
     * @param decisionIdOrName - the decision id or name
     * @return - The decision version
     */
    public DecisionVersion getCurrentVersion(String customerId, String decisionIdOrName) {
        DecisionVersion decisionVersion = decisionVersionDAO.getCurrentVersion(customerId, decisionIdOrName);
        if (decisionVersion == null) {
            throw decisionDoesNotExist(customerId, decisionIdOrName);
        }
        return decisionVersion;
    }

    /**
     * Lists the known versions of a Decision for the specified customer and Decision id or name
     *
     * @param customerId       - The customer id
     * @param decisionIdOrName - The decision id or name
     * @return - The list of versions.
     */
    public List<DecisionVersion> listDecisionVersions(String customerId, String decisionIdOrName) {

        List<DecisionVersion> versions = decisionVersionDAO.listByCustomerAndDecisionIdOrName(customerId, decisionIdOrName);
        if (versions.isEmpty()) {
            throw decisionDoesNotExist(customerId, decisionIdOrName);
        }
        return versions;
    }

    /**
     * Lists the currently known Decisions for the specified Customer.
     *
     * @param customerId - The customer id to find decisions for
     * @return - The list of decisions for this customer.
     */
    public List<Decision> listDecisions(String customerId) {
        return decisionVersionDAO.listCurrentByCustomerId(customerId).stream().map((version) -> version.getDecision()).collect(toList());
    }

    /**
     * @param customerId      - The id of the customer for the Decision
     * @param decisionRequest - The API processed request
     * @return - the updated Decision
     */
    public DecisionVersion createOrUpdateVersion(String customerId, DecisionRequest decisionRequest) {

        Decision decision = decisionDAO.findByCustomerAndName(customerId, decisionRequest.getName());
        if (decision == null) {
            return createDecision(customerId, decisionRequest);
        }
        return updateDecision(customerId, decision, decisionRequest);
    }

    private DecisionVersion createDecisionVersion(String customerId, DecisionRequest decisionRequest) {

        DecisionVersion decisionVersion = new DecisionVersion();

        //TODO - this is occuring within a transaction. If the write to storage takes a long time, the transaction
        // will be held open.

        DMNStorageRequest dmnStorageRequest = decisionDMNStorage.writeDMN(customerId, decisionRequest);
        decisionVersion.setDmnMd5(dmnStorageRequest.getMd5Hash());
        decisionVersion.setDmnLocation(dmnStorageRequest.getProviderUrl());

        decisionVersion.setStatus(DecisionVersionStatus.BUILDING);
        decisionVersion.setSubmittedAt(LocalDateTime.now(ZoneOffset.UTC));
        decisionVersion.setVersion(decisionVersionDAO.getNextVersionId(customerId, decisionRequest.getName()));
        decisionVersion.setConfiguration(decisionRequest.getConfiguration());
        decisionVersion.setTags(decisionRequest.getTags());

        if (decisionRequest.getEventing() != null) {
            Kafka kafka = decisionRequest.getEventing().getKafka();
            if (kafka != null) {
                KafkaTopics topics = new KafkaTopics();
                topics.setSourceTopic(kafka.getSource());
                topics.setSinkTopic(kafka.getSink());
                decisionVersion.setKafkaTopics(topics);
            }
        }

        return decisionVersion;
    }

    private DecisionVersion createDecision(String customerId, DecisionRequest decisionsResponse) {
        Decision decision = new Decision();
        decision.setCustomerId(customerId);
        decision.setName(decisionsResponse.getName());
        decision.setDescription(decisionsResponse.getDescription());

        DecisionVersion decisionVersion = createDecisionVersion(customerId, decisionsResponse);

        decision.addVersion(decisionVersion);
        decision.setNextVersion(decisionVersion);
        decision.setCurrentVersion(decisionVersion);

        decisionDAO.persist(decision);
        LOGGER.info("Created new Decision with name '{}' at version '{}' for customer with id '{}'", decision.getName(), decisionVersion.getVersion(), customerId);
        return decisionVersion;
    }

    private DecisionVersion updateDecision(String customerId, Decision decision, DecisionRequest decisionRequest) {
        checkForExistingLifecycleOperation(decision);
        decision.setDescription(decisionRequest.getDescription());

        DecisionVersion decisionVersion = createDecisionVersion(customerId, decisionRequest);
        decision.addVersion(decisionVersion);
        decision.setNextVersion(decisionVersion);

        LOGGER.info("Updating Decision with name '{}' with new version '{}' for customer with id '{}'", decision.getName(), decisionVersion.getVersion(), customerId);

        return decisionVersion;
    }

    private void verifyCorrectVersionForCallback(org.kie.baaas.mcp.app.model.Decision decision, long version, DecisionVersionStatus operation) {
        if (decision.getNextVersion().getVersion() != version) {
            String message = new StringBuilder()
                    .append("Attempting to mark incorrect version '")
                    .append(version)
                    .append("' as '")
                    .append(operation)
                    .append("' for Decision with name '")
                    .append(decision.getName())
                    .append("' for customer id '")
                    .append(decision.getCustomerId())
                    .append("'")
                    .toString();
            throw new DecisionLifecycleException(message);
        }
    }

    /**
     * Callback method invoked when we have failed to deploy the specified version of a Decision.
     *
     * @param customerId       - The id of the customer that owns the decision
     * @param decisionIdOrName - The id of the DecisionVersion
     * @param version          - The version of the decision deployed
     * @param deployment       - The deployment for the DecisionVersion
     * @return - The updated Decision with the result of the failure recorded.
     */
    public DecisionVersion failed(String customerId, String decisionIdOrName, long version, Deployment deployment) {

        DecisionVersion decisionVersion = findDecisionVersion(customerId, decisionIdOrName, version);
        Decision decision = decisionVersion.getDecision();
        verifyCorrectVersionForCallback(decision, decisionVersion.getVersion(), DecisionVersionStatus.BUILDING);
        decisionVersion.setDeployment(deployment);

        decisionVersion.setStatus(DecisionVersionStatus.FAILED);
        decision.setNextVersion(null);

        LOGGER.info("Marked version '{}' of Decision '{}' as FAILED for customer id '{}", decisionVersion.getVersion(), decision.getName(), customerId);

        return decisionVersion;
    }

    /**
     * Callback method invoked when we have deployed the specified version of a Decision.
     *
     * @param customerId       - The id of the customer that owns the decision
     * @param decisionIdOrName - The id of the decisionVersion that has been deployed
     * @param version          - The version of the decision deployed
     * @param deployment       - The deployment record.
     * @return - The updated Decision with the result of the deployment recorded.
     */
    public DecisionVersion deployed(String customerId, String decisionIdOrName, long version, Deployment deployment) {

        DecisionVersion decisionVersion = findDecisionVersion(customerId, decisionIdOrName, version);
        Decision decision = decisionVersion.getDecision();
        verifyCorrectVersionForCallback(decision, decisionVersion.getVersion(), DecisionVersionStatus.BUILDING);

        decisionVersion.setDeployment(deployment);
        decisionVersion.setStatus(DecisionVersionStatus.CURRENT);
        decisionVersion.setPublishedAt(LocalDateTime.now());

        DecisionVersion currentVersion = decision.getCurrentVersion();
        if (currentVersion.getVersion() != decisionVersion.getVersion()) {
            currentVersion.setStatus(DecisionVersionStatus.READY);
        }

        decision.setCurrentVersion(decisionVersion);
        decision.setNextVersion(null);

        LOGGER.info("Marked version '{}' of Decision '{}' as CURRENT for customer id '{}", decisionVersion.getVersion(), decision.getName(), customerId);

        return decisionVersion;
    }

    /**
     * Begin the process of rolling back to the specified version of the decision.
     *
     * @param customerId       - The customer that owns the decision
     * @param decisionIdOrName - The id or name of the decision
     * @param version          - The version to rollback too
     * @return - The Decision Version we are attempting to rollback to.
     */
    public DecisionVersion rollbackToVersion(String customerId, String decisionIdOrName, long version) {

        DecisionVersion decisionVersion = findDecisionVersion(customerId, decisionIdOrName, version);
        checkForExistingLifecycleOperation(decisionVersion.getDecision());

        if (!DecisionVersionStatus.READY.equals(decisionVersion.getStatus())) {
            throw new DecisionLifecycleException("Cannot rollback to version '" + version + "' of decision '" + decisionIdOrName + "' as it is in state '" + decisionVersion.getStatus() + "'");
        }

        decisionVersion.setStatus(DecisionVersionStatus.BUILDING);
        decisionVersion.getDecision().setNextVersion(decisionVersion);
        return decisionVersion;
    }

    /**
     * Attempts to delete the specified version of a Decision
     *
     * @param customerId   - The customer that owns the Decision
     * @param decisionName - The name of the Decision to delete the version from
     * @param version      - The version of the Decision to delete.
     * @return - The deleted version of the Decision.
     */
    public DecisionVersion deleteVersion(String customerId, String decisionName, long version) {

        DecisionVersion decisionVersion = findDecisionVersion(customerId, decisionName, version);

        // Can't delete a version whilst it is current
        if (DecisionVersionStatus.CURRENT == decisionVersion.getStatus()) {
            throw new DecisionLifecycleException("It is not valid to delete the 'CURRENT' version of Decision '" + decisionName + "' for customer id '" + customerId + "'");
        }

        // Can't delete a version whilst we are building it
        if (DecisionVersionStatus.BUILDING == decisionVersion.getStatus()) {
            throw new DecisionLifecycleException("It is not valid to delete a 'BUILDING' version of Decision '" + decisionName + "' for customer id '" + customerId + "'");
        }

        // Deleting a DecisionVersion is a logical delete. They should still appear in history.
        decisionVersion.setStatus(DecisionVersionStatus.DELETED);
        return decisionVersion;
    }

    private DecisionVersion findDecisionVersion(String customerId, String decisionName, long version) {
        DecisionVersion decisionVersion = decisionVersionDAO.findByCustomerAndDecisionIdOrName(customerId, decisionName, version);
        if (decisionVersion == null) {
            Decision decision = decisionDAO.findByCustomerAndIdOrName(customerId, decisionName);
            if (decision == null) {
                throw decisionDoesNotExist(customerId, decisionName);
            } else {
                throw decisionVersionDoesNotExist(customerId, decisionName, version);
            }
        }
        return decisionVersion;
    }

    /**
     * Deletes the specified decision fully
     *
     * @param customerId       - The customer that owns the decision
     * @param decisionNameOrId - The name of the Decision to delete.
     * @return - The deleted decision.
     */
    public Decision deleteDecision(String customerId, String decisionNameOrId) {

        Decision decision = decisionDAO.findByCustomerAndIdOrName(customerId, decisionNameOrId);
        if (decision == null) {
            throw decisionDoesNotExist(customerId, decisionNameOrId);
        }

        decisionDAO.delete(decision);
        LOGGER.info("Deleted Decision with name '{}' and customer id '{}'", decisionNameOrId, customerId);
        return decision;
    }
}