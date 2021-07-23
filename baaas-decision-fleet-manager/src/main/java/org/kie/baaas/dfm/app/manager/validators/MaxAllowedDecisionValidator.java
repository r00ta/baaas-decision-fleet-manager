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

package org.kie.baaas.dfm.app.manager.validators;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.kie.baaas.dfm.api.decisions.DecisionRequest;
import org.kie.baaas.dfm.app.config.DecisionFleetManagerConfig;
import org.kie.baaas.dfm.app.dao.DecisionDAO;
import org.kie.baaas.dfm.app.resolvers.CustomerIdResolver;

import io.quarkus.security.identity.SecurityIdentity;

/**
 * Custom Validator for DecisionRequest validation.
 * It validates the number of decisions a customer has created with the max. allowed number of decisions.
 *
 * @see WithinDecisionLimits
 */
@ApplicationScoped
public class MaxAllowedDecisionValidator implements ConstraintValidator<WithinDecisionLimits, DecisionRequest> {
    @Inject
    DecisionDAO decisionDAO;

    @Inject
    CustomerIdResolver resolver;

    @Inject
    SecurityIdentity identity;

    @Inject
    DecisionFleetManagerConfig config;

    private boolean isDecisionCountWithinLimit(long numOfDecision) {
        return config.getMaxAllowedDecisions() == -1 || numOfDecision < config.getMaxAllowedDecisions();
    }

    @Override
    public boolean isValid(DecisionRequest decisionRequest, ConstraintValidatorContext constraintValidatorContext) {
        if (decisionRequest == null) {
            return false;
        }
        if (config.getMaxAllowedDecisions() == -1) {
            // Decision limits not enforced in this environment. Request is valid
            return true;
        }
        long decisionCount = decisionDAO.getDecisionCountByCustomerId(resolver.getCustomerId(identity.getPrincipal()));
        return isDecisionCountWithinLimit(decisionCount);
    }
}
