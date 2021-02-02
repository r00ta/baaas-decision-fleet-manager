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

package org.kie.baaas.mcp.app.dao;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;
import org.kie.baaas.mcp.app.model.DecisionVersion;

/**
 * DAO implementation for working with DecisionVersions.
 */
@ApplicationScoped
@Transactional
public class DecisionVersionDAO implements PanacheRepositoryBase<DecisionVersion, String> {

    public long getNextVersionId(String customerId, String decisionName) {
        Parameters params = Parameters.with("customerId", customerId).and("name", decisionName);
        return find("#DecisionVersion.countByCustomerAndName", params).count() + 1;
    }

    public DecisionVersion findByCustomerAndDecisionName(String customerId, String decisionName, long decisionVersion) {
        Parameters params = Parameters.with("customerId", customerId)
                .and("name", decisionName)
                .and("version", decisionVersion);
        return find("#DecisionVersion.byCustomerAndNameAndVersion", params).firstResult();
    }
}
