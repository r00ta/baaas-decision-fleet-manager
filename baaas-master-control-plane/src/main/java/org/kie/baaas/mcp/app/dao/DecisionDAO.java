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

import org.kie.baaas.mcp.app.model.Decision;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;

/**
 * DAO implementation for working with Decision Entities.
 */
@ApplicationScoped
@Transactional
public class DecisionDAO implements PanacheRepositoryBase<Decision, String> {

    public Decision findByCustomerAndName(String customerId, String decisionName) {
        Parameters params = Parameters.with("name", decisionName).and("customerId", customerId);
        return find("#Decision.byCustomerIdAndName", params).firstResult();
    }

    public Decision findByCustomerAndIdOrName(String customerId, String decisionIdOrName) {
        Parameters params = Parameters.with("idOrName", decisionIdOrName).and("customerId", customerId);
        return find("#Decision.byCustomerAndIdOrName", params).firstResult();
    }
}
