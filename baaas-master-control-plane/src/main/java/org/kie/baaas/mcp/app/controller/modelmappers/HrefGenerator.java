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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.UriBuilder;

import org.kie.baaas.mcp.app.config.MasterControlPlaneConfig;
import org.kie.baaas.mcp.app.exceptions.MasterControlPlaneException;
import org.kie.baaas.mcp.app.model.DecisionVersion;

import static java.util.Objects.requireNonNull;

/**
 * Generates href style links for HATEOAS style navigation of the BAaaS API.
 */
@ApplicationScoped
public class HrefGenerator {

    private static final String DECISION_HREF_SUFFIX = "/decisions/{0}/versions/{1}";

    private static final String DECISION_DMN_HREF_SUFFIX = "/dmn";

    private final MasterControlPlaneConfig config;

    @Inject
    public HrefGenerator(MasterControlPlaneConfig config) {
        requireNonNull(config, "config cannot be null");
        this.config = config;
    }

    public String generateDecisionHref(DecisionVersion decisionVersion) {

        String id = decisionVersion.getDecision().getId();
        long version = decisionVersion.getVersion();

        try {
            return UriBuilder.fromUri(config.getApiBaseUrl().toURI()).path(DECISION_HREF_SUFFIX).build(id, version).toURL().toExternalForm();
        } catch (Exception e) {
            throw new MasterControlPlaneException("Failed to build HATEOAS href for DecisionVersion with version '" + version + "' for Decision '" + id + "'", e);
        }
    }

    public String generateDecisionDMNHref(DecisionVersion decisionVersion) {
        return generateDecisionHref(decisionVersion) + DECISION_DMN_HREF_SUFFIX;
    }
}
