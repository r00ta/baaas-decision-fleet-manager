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
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.kie.baaas.mcp.api.decisions.Decisions;
import org.kie.baaas.mcp.api.decisions.DecisionsResponse;
import org.kie.baaas.mcp.api.eventing.kafka.Kafka;
import org.kie.baaas.mcp.app.dao.DecisionDAO;
import org.kie.baaas.mcp.app.dao.DecisionVersionDAO;
import org.kie.baaas.mcp.app.model.Decision;
import org.kie.baaas.mcp.app.model.DecisionVersion;
import org.kie.baaas.mcp.app.model.DecisionVersionStatus;
import org.kie.baaas.mcp.app.model.eventing.KafkaTopics;
import org.kie.baaas.mcp.app.storage.DecisionDMNStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the core lifecycle for Decisions. Our API should ensure valid Payload and then
 * delegate to the DecisionManager to complete the lifecycle management.
 */
@ApplicationScoped
@Transactional
public class DecisionManager {

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

    /**
     * @param customerId        - The id of the customer for the Decision
     * @param decisionsResponse - The API processed request
     * @return - the updated Decision
     */
    public Decision createOrUpdateVersion(String customerId, DecisionsResponse decisionsResponse) {

        Decision decision = decisionDAO.findByCustomerAndName(customerId, decisionsResponse.getName());
        if (decision == null) {
            return createDecision(customerId, decisionsResponse);
        }
        return updateDecision(customerId, decision, decisionsResponse);
    }

    private DecisionVersion createDecisionVersion(String customerId, DecisionsResponse decisionsResponse) {

        DecisionVersion decisionVersion = new DecisionVersion();
        //TODO - handle DMN hash generation/uploading and path resolution
        // decisionDMNStorage.writeDMN(customerId, decisions);

        decisionVersion.setDmnMd5(decisionsResponse.getResponseModel().getMd5());
        decisionVersion.setDmnLocation("s3://some.dmn.location");
        decisionVersion.setStatus(DecisionVersionStatus.BUILDING);
        decisionVersion.setSubmittedAt(LocalDateTime.now(ZoneOffset.UTC));
        decisionVersion.setVersion(decisionVersionDAO.getNextVersionId(customerId, decisionsResponse.getName()));
        decisionVersion.setConfiguration(decisionsResponse.getConfiguration());
        decisionVersion.setTags(decisionsResponse.getTags());

        if (decisionsResponse.getEventing() != null) {
            Kafka kafka = decisionsResponse.getEventing().getKafka();
            if (kafka != null) {
                KafkaTopics topics = new KafkaTopics();
                topics.setSourceTopic(kafka.getSource());
                topics.setSinkTopic(kafka.getSink());
                decisionVersion.setKafkaTopics(topics);
            }
        }

        return decisionVersion;
    }

    private Decision createDecision(String customerId, DecisionsResponse decisionsResponse) {
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
        return decision;
    }

    private Decision updateDecision(String customerId, Decision decision, DecisionsResponse decisionsResponse) {
        checkForExistingLifecycleOperation(decision);
        decision.setDescription(decisionsResponse.getDescription());

        DecisionVersion decisionVersion = createDecisionVersion(customerId, decisionsResponse);
        decision.addVersion(decisionVersion);
        decision.setNextVersion(decisionVersion);

        LOGGER.info("Updating Decision with name '{}' with new version '{}' for customer with id '{}'", decision.getName(), decisionVersion.getVersion(), customerId);

        return decision;
    }

    private Decision findByCustomerIdAndName(String customerId, String decisionName, boolean fail) {
        Decision decision = decisionDAO.findByCustomerAndName(customerId, decisionName);
        if (decision == null && fail) {
            throw new DecisionLifecycleException("Decision with name '" + decisionName + "' does not exist for customer with id '" + customerId + "'");
        }

        return decision;
    }

    private void verifyCorrectVersionForCallback(Decision decision, long version, DecisionVersionStatus operation) {
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
     * @param customerId   - The id of the customer that owns the decision
     * @param decisionName - The name of the decision
     * @param version      - The version of the decision
     * @return - The updated Decision with the result of the failure recorded.
     */
    public Decision failed(String customerId, String decisionName, long version) {

        Decision decision = findByCustomerIdAndName(customerId, decisionName, true);
        verifyCorrectVersionForCallback(decision, version, DecisionVersionStatus.FAILED);
        decision.getNextVersion().setStatus(DecisionVersionStatus.FAILED);
        decision.setNextVersion(null);

        LOGGER.info("Marked version '{}' of Decision '{}' as FAILED for customer id '{}", version, decisionName, customerId);

        return decision;
    }

    /**
     * Callback method invoked when we have deployed the specified version of a Decision.
     *
     * @param customerId   - The id of the customer that owns the decision
     * @param decisionName - The name of the decision
     * @param version      - The version of the decision
     * @return - The updated Decision with the result of the deployment recorded.
     */
    public Decision deployed(String customerId, String decisionName, long version) {
        Decision decision = findByCustomerIdAndName(customerId, decisionName, true);
        verifyCorrectVersionForCallback(decision, version, DecisionVersionStatus.CURRENT);

        DecisionVersion nextVersion = decision.getNextVersion();
        nextVersion.setStatus(DecisionVersionStatus.CURRENT);

        DecisionVersion currentVersion = decision.getCurrentVersion();
        if (currentVersion.getVersion() != nextVersion.getVersion()) {
            currentVersion.setStatus(DecisionVersionStatus.READY);
        }

        decision.setCurrentVersion(nextVersion);
        decision.setNextVersion(null);

        LOGGER.info("Marked version '{}' of Decision '{}' as CURRENT for customer id '{}", version, decisionName, customerId);

        return decision;
    }

    public Decisions rollbackToVersion(String customerId, Decisions decisions) {
        //TODO - rollback
        return null;
    }

    /**
     * Attempts to delete the specified version of a Decision
     *
     * @param customerId        - The customer that owns the Decision
     * @param decisionsResponse - The API request encapsulating the delete request
     * @return - The deleted version of the Decision.
     */
    public DecisionVersion deleteVersion(String customerId, DecisionsResponse decisionsResponse) {

        DecisionVersion decisionVersion = decisionVersionDAO.findByCustomerAndDecisionName(customerId, decisionsResponse.getName(), decisionsResponse.getVersion());
        if (decisionVersion == null) {
            String message = new StringBuilder("Version '")
                    .append(decisionsResponse.getVersion())
                    .append("' of Decision '")
                    .append(decisionsResponse.getName())
                    .append("' does not exist for customer id '")
                    .append(customerId)
                    .append("'")
                    .toString();
            throw new DecisionLifecycleException(message);
        }

        // Can't delete a version whilst it is current
        if (DecisionVersionStatus.CURRENT == decisionVersion.getStatus()) {
            throw new DecisionLifecycleException("It is not valid to delete the 'CURRENT' version of Decision '" + decisionsResponse.getName() + "' for customer id '" + customerId + "'");
        }

        // Can't delete a version whilst we are building it
        if (DecisionVersionStatus.BUILDING == decisionVersion.getStatus()) {
            throw new DecisionLifecycleException("It is not valid to delete a 'BUILDING' version of Decision '" + decisionsResponse.getName() + "' for customer id '" + customerId + "'");
        }

        // Deleting a DecisionVersion is a logical delete. They should still appear in history.
        decisionVersion.setStatus(DecisionVersionStatus.DELETED);
        return decisionVersion;
    }

    /**
     * Deletes the specified decision fully
     *
     * @param customerId - The customer that owns the decision
     * @param decisions  - The API request identifying the Decision to delete
     * @return - The deleted decision.
     */
    public Decision deleteDecision(String customerId, Decisions decisions) {

        Decision decision = findByCustomerIdAndName(customerId, decisions.getName(), true);
        decisionDAO.delete(decision);

        LOGGER.info("Deleted Decision with name '{}' and customer id '{}'", decisions.getName(), customerId);
        return decision;
    }
}