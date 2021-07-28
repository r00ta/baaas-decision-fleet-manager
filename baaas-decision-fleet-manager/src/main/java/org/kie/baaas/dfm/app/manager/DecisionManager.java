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
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.kie.baaas.dfm.api.decisions.DecisionRequest;
import org.kie.baaas.dfm.api.eventing.Eventing;
import org.kie.baaas.dfm.app.dao.DecisionDAO;
import org.kie.baaas.dfm.app.dao.DecisionVersionDAO;
import org.kie.baaas.dfm.app.model.Decision;
import org.kie.baaas.dfm.app.model.DecisionVersion;
import org.kie.baaas.dfm.app.model.DecisionVersionStatus;
import org.kie.baaas.dfm.app.model.ListResult;
import org.kie.baaas.dfm.app.model.deployment.Deployment;
import org.kie.baaas.dfm.app.model.eventing.KafkaConfig;
import org.kie.baaas.dfm.app.storage.DMNStorageRequest;
import org.kie.baaas.dfm.app.storage.DecisionDMNStorage;
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
        Objects.requireNonNull(decisionVersionDAO, "decisionVersionDAO cannot be null");
        Objects.requireNonNull(decisionDMNStorage, "decisionDMNStorage cannot be null");
        this.decisionDAO = decisionDAO;
        this.decisionVersionDAO = decisionVersionDAO;
        this.decisionDMNStorage = decisionDMNStorage;
    }

    /**
     * Gets the specified version of the decision for the given customer.
     *
     * @param customerId - The customer id
     * @param decisionIdOrName - The decision id or name
     * @param decisionVersion - The version of the Decision
     * @return - The Decision Version
     */
    @Override
    public DecisionVersion getVersion(String customerId, String decisionIdOrName, long decisionVersion) {
        return findDecisionVersion(customerId, decisionIdOrName, decisionVersion);
    }

    /**
     * Gets the current version of the decision for the given customer and decision id or name
     *
     * @param customerId - the customer id
     * @param decisionIdOrName - the decision id or name
     * @return - The decision version
     */
    @Override
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
     * @param customerId - The customer id
     * @param decisionIdOrName - The decision id or name
     * @return - The list of versions.
     */
    @Override
    public ListResult<DecisionVersion> listDecisionVersions(String customerId, String decisionIdOrName, int page, int pageSize) {
        ListResult<DecisionVersion> versions = decisionVersionDAO.listByCustomerAndDecisionIdOrName(customerId, decisionIdOrName, page, pageSize);
        if (versions.getItems().isEmpty()) {
            throw decisionDoesNotExist(customerId, decisionIdOrName);
        }
        return versions;
    }

    /**
     * Lists all the decisions.
     *
     * @return - The list of decisions.
     */
    public List<Decision> listDecisions() {
        List<DecisionVersion> versions = decisionVersionDAO.listAll();
        return versions.stream().map(DecisionVersion::getDecision).collect(toList());
    }

    /**
     * Lists the currently known Decisions for the specified Customer.
     *
     * @param customerId - The customer id to find decisions for
     * @return - The list of decisions for this customer.
     */
    @Override
    public ListResult<Decision> listDecisions(String customerId, int page, int pageSize) {
        ListResult<DecisionVersion> versions = decisionVersionDAO.listCurrentByCustomerId(customerId, page, pageSize);
        List<Decision> decisions = versions.getItems().stream().map(DecisionVersion::getDecision).collect(toList());
        return new ListResult<>(decisions, versions.getPage(), versions.getTotal());
    }

    @Override
    public DecisionVersion getBuildingVersion(String customerId, String decisionIdOrName) {
        DecisionVersion decisionVersion = decisionVersionDAO.getBuildingVersion(customerId, decisionIdOrName);
        if (decisionVersion == null) {
            throw new NoSuchDecisionVersionException("There is no BUILDING version of Decision with id '" + decisionIdOrName + "' for customer '" + customerId + "'");
        }

        return decisionVersion;
    }

    /**
     * @param customerId - The id of the customer for the Decision
     * @param decisionRequest - The API processed request
     * @return - the updated Decision
     */
    @Override
    public DecisionVersion createOrUpdateVersion(String customerId, DecisionRequest decisionRequest) {
        Decision decision = decisionDAO.findByCustomerAndName(customerId, decisionRequest.getName());
        if (decision == null) {
            return createDecision(customerId, decisionRequest);
        }
        return updateDecision(customerId, decision, decisionRequest);
    }

    /**
     * Callback method invoked when we have failed to deploy the specified version of a Decision.
     *
     * @param customerId - The id of the customer that owns the decision
     * @param decisionIdOrName - The id or the name of the DecisionVersion
     * @param version - The version of the decision deployed
     * @param deployment - The deployment for the DecisionVersion
     * @return - The updated Decision with the result of the failure recorded.
     */
    @Override
    public DecisionVersion failed(String customerId, String decisionIdOrName, long version, Deployment deployment) {
        DecisionVersion decisionVersion = findDecisionVersion(customerId, decisionIdOrName, version);
        Decision decision = decisionVersion.getDecision();
        verifyCorrectVersionForCallback(decision, decisionVersion.getVersion(), DecisionVersionStatus.BUILDING);
        decisionVersion.setDeployment(deployment);

        decisionVersion.setStatus(DecisionVersionStatus.FAILED);
        decision.setNextVersion(null);

        /*
         * In the case of multiple failures in a row, make the most recent failure the CURRENT
         * one.
         */
        if (DecisionVersionStatus.FAILED == decision.getCurrentVersion().getStatus()) {
            if (decision.getCurrentVersion().getVersion() != decisionVersion.getVersion()) {
                decision.setCurrentVersion(decisionVersion);
            }
        }

        LOGGER.info("Marked version '{}' of Decision '{}' as FAILED for customer id '{}", decisionVersion.getVersion(), decision.getName(), customerId);

        return decisionVersion;
    }

    /**
     * Callback method invoked when we have deployed the specified version of a Decision.
     *
     * @param customerId - The id of the customer that owns the decision
     * @param decisionIdOrName - The id or the name of the decisionVersion that has been deployed
     * @param version - The version of the decision deployed
     * @param deployment - The deployment record.
     * @return - The updated Decision with the result of the deployment recorded.
     */
    @Override
    public DecisionVersion deployed(String customerId, String decisionIdOrName, long version, Deployment deployment) {
        DecisionVersion decisionVersion = findDecisionVersion(customerId, decisionIdOrName, version);
        Decision decision = decisionVersion.getDecision();
        verifyCorrectVersionForCallback(decision, decisionVersion.getVersion(), DecisionVersionStatus.BUILDING);

        decisionVersion.setDeployment(deployment);
        decisionVersion.setStatus(DecisionVersionStatus.CURRENT);
        decisionVersion.setPublishedAt(ZonedDateTime.now(ZoneOffset.UTC));

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
     * Begin the process of moving the "current" endpoint to the specified decision version.
     *
     * @param customerId - The customer that owns the decision
     * @param decisionIdOrName - The id or name of the decision
     * @param version - The version to use in the CURRENT endpoint
     * @return - The Decision Version we are attempting to use as current one.
     */
    @Override
    public DecisionVersion setCurrentVersion(String customerId, String decisionIdOrName, long version) {
        DecisionVersion decisionVersion = findDecisionVersion(customerId, decisionIdOrName, version);
        checkForExistingLifecycleOperation(decisionVersion.getDecision());

        if (!DecisionVersionStatus.READY.equals(decisionVersion.getStatus())) {
            throw new DecisionLifecycleException(
                    "Cannot move the current pointer to version '" + version + "' of decision '" + decisionIdOrName + "' as it is in state '" + decisionVersion.getStatus() + "'");
        }

        decisionVersion.setStatus(DecisionVersionStatus.BUILDING);
        decisionVersion.getDecision().setNextVersion(decisionVersion);
        return decisionVersion;
    }

    /**
     * Reads the given dmn version and name from associated customerId
     *
     * @param customerId - The customer that owns the Decision
     * @param decisionNameOrId - The name or the id of the decision to be returned
     * @param version - The version of the decision to be returned
     * @return - The dmn as String from S3 bucket
     */
    @Override
    public ByteArrayOutputStream getDMN(String customerId, String decisionNameOrId, long version) {
        DecisionVersion decisionVersion = findDecisionVersion(customerId, decisionNameOrId, version);
        return decisionDMNStorage.readDMN(customerId, decisionVersion);
    }

    /**
     * Attempts to delete the specified version of a Decision
     *
     * @param decisionId - The id of the Decision to delete the version from
     * @param version - The version of the Decision to delete.
     * @return - The deleted version of the Decision.
     */
    @Override
    public DecisionVersion deleteVersion(String decisionId, long version) {
        Decision decision = decisionDAO.findById(decisionId);
        if (decision == null) {
            throw new NoSuchDecisionException("Decision with id '" + decisionId + "' does not exist.");
        }
        return deleteVersion(decision.getCustomerId(), decisionId, version);
    }

    /**
     * Attempts to delete the specified version of a Decision
     *
     * @param customerId - The customer that owns the Decision
     * @param decisionNameOrId - The name or the id of the Decision to delete the version from
     * @param version - The version of the Decision to delete.
     * @return - The deleted version of the Decision.
     */
    @Override
    public DecisionVersion deleteVersion(String customerId, String decisionNameOrId, long version) {
        DecisionVersion decisionVersion = findDecisionVersion(customerId, decisionNameOrId, version);
        return deleteDecisionVersion(decisionVersion);
    }

    /**
     * Deletes the specified decision fully
     *
     * @param decisionId - The id of the Decision to delete.
     * @return - The deleted decision.
     */
    @Override
    public Decision deleteDecision(String decisionId) {
        Decision decision = decisionDAO.findById(decisionId);
        return deleteDecision(decision, decision != null ? decision.getCustomerId() : "<UNKNOWN>", decisionId);
    }

    /**
     * Deletes the specified decision fully
     *
     * @param customerId - The customer that owns the decision
     * @param decisionNameOrId - The name of the Decision to delete.
     * @return - The deleted decision.
     */
    public Decision deleteDecision(String customerId, String decisionNameOrId) {
        Decision decision = decisionDAO.findByCustomerAndIdOrName(customerId, decisionNameOrId);
        return deleteDecision(decision, customerId, decisionNameOrId);
    }

    private DecisionVersion findDecisionVersion(String customerId, String decisionName, long version) {
        DecisionVersion decisionVersion = decisionVersionDAO.findByCustomerAndDecisionIdOrName(customerId, decisionName, version);
        return findDecisionVersion(decisionVersion, customerId, decisionName, version);
    }

    private DecisionVersion deleteDecisionVersion(DecisionVersion decisionVersion) {
        // Can't delete a version whilst it is current
        if (DecisionVersionStatus.CURRENT == decisionVersion.getStatus()) {
            throw new DecisionLifecycleException("It is not valid to delete the 'CURRENT' version of Decision '" + decisionVersion.getDecision().getId());
        }

        // Deleting a DecisionVersion is a logical delete. They should still appear in history.
        decisionVersion.setStatus(DecisionVersionStatus.DELETED);
        return decisionVersion;
    }

    private DecisionVersion findDecisionVersion(DecisionVersion decisionVersion, String customerId, String decisionNameOrId, long version) {
        if (decisionVersion == null) {
            Decision decision = decisionDAO.findByCustomerAndIdOrName(customerId, decisionNameOrId);
            if (decision == null) {
                throw decisionDoesNotExist(customerId, decisionNameOrId);
            } else {
                throw decisionVersionDoesNotExist(customerId, decisionNameOrId, version);
            }
        }
        return decisionVersion;
    }

    private Decision deleteDecision(Decision decision, String customerId, String decisionNameOrId) {
        if (decision == null) {
            throw decisionDoesNotExist(customerId, decisionNameOrId);
        }

        decisionDAO.delete(decision);
        LOGGER.info("Deleted Decision with name '{}' and customer id '{}'", decisionNameOrId, customerId);
        return decision;
    }

    /*
     * Checks to see that we are not already performing a lifecycle operation for this Decision.
     * Currently we only support sequential lifecycle operations.
     */
    private void checkForExistingLifecycleOperation(Decision decision) {
        if (decision.getNextVersion() != null) {
            if (DecisionVersionStatus.BUILDING.equals(decision.getNextVersion().getStatus())) {
                throw new DecisionLifecycleException(
                        "A lifecycle operation is already in progress for Version '" + decision.getCurrentVersion().getVersion() + "' of Decision '" + decision.getName() + "'");
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

    private DecisionVersion createDecisionVersion(String customerId, DecisionRequest decisionRequest) {
        DecisionVersion decisionVersion = new DecisionVersion();
        decisionVersion.setStatus(DecisionVersionStatus.BUILDING);
        decisionVersion.setSubmittedAt(ZonedDateTime.now(ZoneOffset.UTC));
        decisionVersion.setVersion(decisionVersionDAO.getNextVersionId(customerId, decisionRequest.getName()));
        decisionVersion.setConfiguration(decisionRequest.getConfiguration());
        decisionVersion.setTags(decisionRequest.getTags());
        decisionVersion.setDescription(decisionRequest.getDescription());

        if (decisionRequest.getEventing() != null) {
            Eventing eventing = decisionRequest.getEventing();
            if (eventing != null && eventing.getKafka() != null) {
                KafkaConfig kafkaCfg = new KafkaConfig();
                kafkaCfg.setSourceTopic(eventing.getKafka().getSource());
                kafkaCfg.setSinkTopic(eventing.getKafka().getSink());
                kafkaCfg.setBootstrapServers(eventing.getKafka().getBootstrapServers());
                decisionVersion.setKafkaConfig(kafkaCfg);
            }
        }

        return decisionVersion;
    }

    private DecisionVersion createDecision(String customerId, DecisionRequest decisionRequest) {
        Decision decision = new Decision();
        decision.setCustomerId(customerId);
        decision.setName(decisionRequest.getName());

        DecisionVersion decisionVersion = createDecisionVersion(customerId, decisionRequest);
        decision.addVersion(decisionVersion);
        decision.setNextVersion(decisionVersion);
        decision.setCurrentVersion(decisionVersion);

        //TODO - this is occurring within a transaction. If the write to storage takes a long time, the transaction
        // will be held open.
        DMNStorageRequest dmnStorageRequest = decisionDMNStorage.writeDMN(customerId, decisionRequest, decisionVersion);
        decisionVersion.setDmnMd5(dmnStorageRequest.getMd5Hash());
        decisionVersion.setDmnLocation(dmnStorageRequest.getProviderUrl());

        decisionDAO.persist(decision);
        LOGGER.info("Created new Decision with name '{}' at version '{}' for customer with id '{}'", decision.getName(), decisionVersion.getVersion(), customerId);
        return decisionVersion;
    }

    private DecisionVersion updateDecision(String customerId, Decision decision, DecisionRequest decisionRequest) {
        checkForExistingLifecycleOperation(decision);

        DecisionVersion decisionVersion = createDecisionVersion(customerId, decisionRequest);
        decision.addVersion(decisionVersion);
        decision.setNextVersion(decisionVersion);

        DMNStorageRequest dmnStorageRequest = decisionDMNStorage.writeDMN(customerId, decisionRequest, decisionVersion);
        decisionVersion.setDmnMd5(dmnStorageRequest.getMd5Hash());
        decisionVersion.setDmnLocation(dmnStorageRequest.getProviderUrl());

        decisionDAO.persist(decision);
        LOGGER.info("Updating Decision with name '{}' with new version '{}' for customer with id '{}'", decision.getName(), decisionVersion.getVersion(), customerId);
        return decisionVersion;
    }

    private void verifyCorrectVersionForCallback(org.kie.baaas.dfm.app.model.Decision decision, long version, DecisionVersionStatus operation) {
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
}
