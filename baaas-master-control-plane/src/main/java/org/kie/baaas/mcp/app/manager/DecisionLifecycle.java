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

import org.kie.baaas.mcp.api.decisions.DecisionRequest;
import org.kie.baaas.mcp.app.model.Decision;
import org.kie.baaas.mcp.app.model.DecisionVersion;

/**
 * Core interface for working with the lifecycle of a Decision.
 */
public interface DecisionLifecycle {

    /**
     * Delete the specified Decision
     *
     * @param customerId       - The id of the customer that owns the Decision
     * @param decisionNameOrId - The id or name of the Decision
     * @return - The deleted Decision
     */
    Decision deleteDecision(String customerId, String decisionNameOrId);

    /**
     * Creates or updates a Decision for the given customerId
     *
     * @param customerId      - The id of the customer that owns the Decision
     * @param decisionRequest - The API request sent to the BAaaS API
     * @return - The created or updated DecisionVersion
     */
    DecisionVersion createOrUpdateVersion(String customerId, DecisionRequest decisionRequest);

    /**
     * Rollback to a specific version of a Decision.
     *
     * @param customerId       - The id of the customer that owns the Decision
     * @param decisionIdOrName - The id or name of the Decision
     * @param version          - The version to rollback to
     * @return - The DecisionVersion that has been rolled back to
     */
    DecisionVersion rollbackToVersion(String customerId, String decisionIdOrName, long version);

    /**
     * List all Decisions for the specified Customer
     *
     * @param customerId - The id of the customer that owns the Decisions
     * @return - The List of Decisions for this customer (can be empty)
     */
    List<Decision> listDecisions(String customerId);

    /**
     * Return details of a specific Decision Version
     *
     * @param customerId       - The id of the customer that owns the Decision
     * @param decisionIdOrName - The id or name of the Decision
     * @param version          - The version of the Decision
     * @return - The DecisionVersion requested
     */
    DecisionVersion getVersion(String customerId, String decisionIdOrName, long version);

    /**
     * Delete a specific Decision Version
     *
     * @param customerId       - The id of the customer that owns the Decision
     * @param decisionIdOrName - The id or name of the Decision
     * @param version          - The version of the Decision
     * @return - The deleted DecisionVersion
     */
    DecisionVersion deleteVersion(String customerId, String decisionIdOrName, long version);

    /**
     * Returns the current version of a Decision. This will normally be the DecisionVersion
     * in state CURRENT. However if the Decision does not have a version in state CURRENT,
     * this will be the most recent DecisionVersion in state BUILDING or FAILED.
     *
     * @param customerId       - The id of the customer that owns the Decision
     * @param decisionIdOrName - The id or name of the Decision
     * @return - The current Decision Version.
     */
    DecisionVersion getCurrentVersion(String customerId, String decisionIdOrName);

    /**
     * Lists all versions of the specified decision
     *
     * @param customerId       - The id of the customer that owns the decision
     * @param decisionIdOrName - The id or name of the Decision
     * @return - The list of versions for this decision.
     */
    List<DecisionVersion> listDecisionVersions(String customerId, String decisionIdOrName);
}
