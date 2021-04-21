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

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;

import org.kie.baaas.mcp.app.model.DecisionVersion;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;

/**
 * DAO implementation for working with DecisionVersions.
 */
@ApplicationScoped
@Transactional
public class DecisionVersionDAO implements PanacheRepositoryBase<DecisionVersion, String> {

    private static final String CUSTOMER_ID_PARAM = "customerId";

    private Parameters customerIdParams(String customerId) {
        return Parameters.with(CUSTOMER_ID_PARAM, customerId);
    }

    public long getNextVersionId(String customerId, String decisionName) {
        Parameters params = customerIdParams(customerId).and("name", decisionName);
        return find("#DecisionVersion.countByCustomerAndName", params).count() + 1;
    }

    public DecisionVersion getCurrentVersion(String customerId, String decisionIdOrName) {
        Parameters params = customerIdParams(customerId).and("idOrName", decisionIdOrName);
        return find("#DecisionVersion.currentByCustomerAndDecisionIdOrName", params).firstResult();
    }

    public DecisionVersion getBuildingVersion(String customerId, String decisionIdOrName) {
        Parameters params = customerIdParams(customerId).and("idOrName", decisionIdOrName);
        return find("#DecisionVersion.buildingByCustomerAndDecisionIdOrName", params).firstResult();
    }

    public DecisionVersion findByCustomerAndDecisionIdOrName(String customerId, String decisionIdOrName, long decisionVersion) {
        Parameters params = customerIdParams(customerId).and("idOrName", decisionIdOrName).and("version", decisionVersion);
        return find("#DecisionVersion.byCustomerDecisionIdOrNameAndVersion", params).firstResult();
    }

    public List<DecisionVersion> listCurrentByCustomerId(String customerId) {
        return list("#DecisionVersion.currentByCustomer", customerIdParams(customerId));
    }

    public List<DecisionVersion> listByCustomerAndDecisionIdOrName(String customerId, String decisionIdOrName) {
        Parameters params = customerIdParams(customerId).and("idOrName", decisionIdOrName);
        return list("#DecisionVersion.byCustomerAndDecisionIdOrName", params);
    }
}
