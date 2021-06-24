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

import org.kie.baaas.mcp.api.decisions.DecisionRequest;
import org.kie.baaas.mcp.app.model.Decision;
import org.kie.baaas.mcp.app.model.DecisionVersion;
import org.kie.baaas.mcp.app.model.ListResult;
import org.kie.baaas.mcp.app.model.deployment.Deployment;

/**
 * Core interface for working with the lifecycle of a Decision.
 */
public interface DecisionLifecycle {

    /**
     * Delete the specified Decision
     *
     * @param customerId - The id of the customer that owns the Decision
     * @param decisionNameOrId - The id or name of the Decision
     * @return - The deleted Decision
     */
    Decision deleteDecision(String customerId, String decisionNameOrId);

    /**
     * Creates or updates a Decision for the given customerId
     *
     * @param customerId - The id of the customer that owns the Decision
     * @param decisionRequest - The API request sent to the BAaaS API
     * @return - The created or updated DecisionVersion
     */
    DecisionVersion createOrUpdateVersion(String customerId, DecisionRequest decisionRequest);

    /**
     * Sets new current version of a Decision.
     *
     * @param customerId - The id of the customer that owns the Decision
     * @param decisionIdOrName - The id or name of the Decision
     * @param version - The new current version
     * @return - The DecisionVersion that has been deployed
     */
    DecisionVersion setCurrentVersion(String customerId, String decisionIdOrName, long version);

    /**
     * List all Decisions for the specified Customer
     *
     * @param customerId - The id of the customer that owns the Decisions
     * @return - The List of Decisions for this customer (can be empty)
     */
    ListResult<Decision> listDecisions(String customerId, int page, int pageSize);

    /**
     * Gets the building version of the specified decision owned by the customer id.
     *
     * @param customerId - The customer id that owns the decision
     * @param decisionIdOrName - The decision id or name to get the building version for
     * @return - The building version of the Decision.
     */
    DecisionVersion getBuildingVersion(String customerId, String decisionIdOrName);

    /**
     * Return details of a specific Decision Version
     *
     * @param customerId - The id of the customer that owns the Decision
     * @param decisionIdOrName - The id or name of the Decision
     * @param version - The version of the Decision
     * @return - The DecisionVersion requested
     */
    DecisionVersion getVersion(String customerId, String decisionIdOrName, long version);

    /**
     * Delete a specific Decision Version
     *
     * @param customerId - The id of the customer that owns the Decision
     * @param decisionIdOrName - The id or name of the Decision
     * @param version - The version of the Decision
     * @return - The deleted DecisionVersion
     */
    DecisionVersion deleteVersion(String customerId, String decisionIdOrName, long version);

    /**
     * Returns the current version of a Decision. This will normally be the DecisionVersion
     * in state CURRENT. However if the Decision does not have a version in state CURRENT,
     * this will be the most recent DecisionVersion in state BUILDING or FAILED.
     *
     * @param customerId - The id of the customer that owns the Decision
     * @param decisionIdOrName - The id or name of the Decision
     * @return - The current Decision Version.
     */
    DecisionVersion getCurrentVersion(String customerId, String decisionIdOrName);

    /**
     * Lists all versions of the specified decision
     *
     * @param customerId - The id of the customer that owns the decision
     * @param decisionIdOrName - The id or name of the Decision
     * @return - The list of versions for this decision.
     */
    ListResult<DecisionVersion> listDecisionVersions(String customerId, String decisionIdOrName, int page, int pageSize);

    /**
     * Retrieve the requested dmn file from S3 Bucket as ByteArrayOutputStream
     *
     * @param customerId - The id of the customer that owns the Decision
     * @param decisionIdOrName - The id or name of the Decision
     * @param version - The version of the Decision
     * @return the request dmn file as ByteArrayOutputStream
     */
    ByteArrayOutputStream getDMN(String customerId, String decisionIdOrName, long version);

    /**
     * Callback method invoked when we have failed to deploy the specified version of a Decision.
     *
     * @param customerId - The id of the customer that owns the decision
     * @param decisionIdOrName - The id of the DecisionVersion
     * @param version - The version of the decision deployed
     * @param deployment - The deployment for the DecisionVersion
     * @return - The updated Decision with the result of the failure recorded.
     */
    DecisionVersion failed(String customerId, String decisionIdOrName, long version, Deployment deployment);

    /**
     * Callback method invoked when we have deployed the specified version of a Decision.
     *
     * @param customerId - The id of the customer that owns the decision
     * @param decisionIdOrName - The id of the decisionVersion that has been deployed
     * @param version - The version of the decision deployed
     * @param deployment - The deployment record.
     * @return - The updated Decision with the result of the deployment recorded.
     */
    DecisionVersion deployed(String customerId, String decisionIdOrName, long version, Deployment deployment);
}
