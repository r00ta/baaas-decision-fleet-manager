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

package org.kie.baaas.dfm.app.dao;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;

import org.kie.baaas.dfm.app.model.DecisionVersion;
import org.kie.baaas.dfm.app.model.ListResult;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;

import static java.util.Collections.emptyList;

/**
 * DAO implementation for working with DecisionVersions.
 */
@ApplicationScoped
@Transactional
public class DecisionVersionDAO implements PanacheRepositoryBase<DecisionVersion, String> {

    private static final String CUSTOMER_ID_PARAM = "customerId";

    private static final String ID_PARAM = "id";

    private static final String IDS_PARAM = "ids";

    private static final String ID_OR_NAME_PARAM = "idOrName";

    private static final String NAME_PARAM = "name";

    private static final String VERSION_PARAM = "version";

    private Parameters customerIdParams(String customerId) {
        return Parameters.with(CUSTOMER_ID_PARAM, customerId);
    }

    public long getNextVersionId(String customerId, String decisionName) {
        Parameters params = customerIdParams(customerId).and(NAME_PARAM, decisionName);
        return find("#DecisionVersion.countByCustomerAndName", params).count() + 1;
    }

    public DecisionVersion getCurrentVersion(String customerId, String decisionIdOrName) {
        Parameters params = customerIdParams(customerId).and(ID_OR_NAME_PARAM, decisionIdOrName);
        return find("#DecisionVersion.currentByCustomerAndDecisionIdOrName", params).firstResult();
    }

    public DecisionVersion getBuildingVersion(String customerId, String decisionIdOrName) {
        Parameters params = customerIdParams(customerId).and(ID_OR_NAME_PARAM, decisionIdOrName);
        return find("#DecisionVersion.buildingByCustomerAndDecisionIdOrName", params).firstResult();
    }

    public DecisionVersion findByCustomerAndDecisionIdOrName(String customerId, String decisionIdOrName, long decisionVersion) {
        Parameters params = customerIdParams(customerId).and(ID_OR_NAME_PARAM, decisionIdOrName).and(VERSION_PARAM, decisionVersion);
        return find("#DecisionVersion.byCustomerDecisionIdOrNameAndVersion", params).firstResult();
    }

    public List<DecisionVersion> listAll() {
        return find("#DecisionVersion.listAll").list();
    }

    public ListResult<DecisionVersion> listCurrentByCustomerId(String customerId, int page, int pageSize) {
        Parameters p = customerIdParams(customerId);
        return executePagedQuery("DecisionVersion.countCurrentByCustomer", "DecisionVersion.listCurrentIdsByCustomer", "DecisionVersion.listCurrentByCustomer", p, page, pageSize);
    }

    private void addParamsToNamedQuery(Parameters params, TypedQuery<?> namedQuery) {
        params.map().forEach((key, value) -> namedQuery.setParameter(key, value.toString()));
    }

    private List<String> executeIdsQuery(String idsQuery, Parameters params, int firstResult, int maxResults) {
        TypedQuery<String> namedQuery = getEntityManager().createNamedQuery(idsQuery, String.class);
        addParamsToNamedQuery(params, namedQuery);
        return namedQuery.setMaxResults(maxResults).setFirstResult(firstResult).getResultList();
    }

    private Long executeCountQuery(String countQuery, Parameters parameters) {
        TypedQuery<Long> namedQuery = getEntityManager().createNamedQuery(countQuery, Long.class);
        addParamsToNamedQuery(parameters, namedQuery);
        return namedQuery.getSingleResult();
    }

    /*
     * Executes a list query with paged results. Unfortunately we can't rely on Panaches in-built Paging
     * due the fetched joins in our queries for DecisionVersion entities (in one query we also fetch the Decision, Config, Tags) etc
     * 
     * So the way we do paged queries for these entities breaks down as follows:
     * - Count the total number of entities
     * - Determine the ids of the entities that will be on the page the user is requesting
     * - Select the entities by id, performing the fetched join of related entities.
     */
    private ListResult<DecisionVersion> executePagedQuery(String countQuery, String idsQuery, String versionsQuery, Parameters params, int page, int pageSize) {

        Long totalVersions = executeCountQuery(countQuery, params);
        if (totalVersions == 0L) {
            return new ListResult<>(emptyList(), page, totalVersions);
        }
        int firstResult = getFirstResult(page, pageSize);

        List<String> ids = executeIdsQuery(idsQuery, params, firstResult, pageSize);
        // Panache expects a # prefix on Named Queries
        List<DecisionVersion> versions = list("#" + versionsQuery, Parameters.with(IDS_PARAM, ids));
        return new ListResult<>(versions, page, totalVersions);
    }

    public ListResult<DecisionVersion> listByCustomerAndDecisionIdOrName(String customerId, String decisionIdOrName, int page, int pageSize) {
        Parameters p = customerIdParams(customerId).and(ID_OR_NAME_PARAM, decisionIdOrName);
        return executePagedQuery("DecisionVersion.countByIdOrName", "DecisionVersion.listIdsByIdOrName", "DecisionVersion.listByIdOrName", p, page, pageSize);
    }

    private int getFirstResult(int requestedPage, int requestedPageSize) {
        if (requestedPage <= 0) {
            return 0;
        }

        return requestedPage * requestedPageSize;
    }
}
