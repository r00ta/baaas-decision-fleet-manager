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

package org.kie.baaas.mcp.app.controller.modelmappers;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.kie.baaas.mcp.api.decisions.DecisionResponse;
import org.kie.baaas.mcp.api.decisions.DecisionResponseList;
import org.kie.baaas.mcp.app.model.Decision;
import org.kie.baaas.mcp.app.model.DecisionVersion;

/**
 * Maps between our internal domain model and the DTOs supported by the REST API for BAaaS.
 */
@ApplicationScoped
public class DecisionMapper {

    public DecisionResponse mapVersionToDecisionResponse(DecisionVersion decisionVersion) {
        return null;
    }

    public DecisionResponse mapVersionToDecisionResponse(Decision decision) {
        return null;
    }

    public DecisionResponseList mapVersionsToDecisionResponseList(List<DecisionVersion> decisionVersions) {
        return null;
    }

    public DecisionResponseList mapToDecisionResponseList(List<Decision> decisions) {
        return null;
    }
}
