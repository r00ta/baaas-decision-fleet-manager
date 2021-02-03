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

package org.kie.baaas.mcp.app.storage.s3;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.kie.baaas.mcp.api.decisions.DecisionsRequest;
import org.kie.baaas.mcp.app.storage.DecisionDMNStorage;
import org.kie.baaas.mcp.app.storage.hash.DMNHashGenerator;

@ApplicationScoped
public class S3DMNStorage implements DecisionDMNStorage {

    // s3 bucket name should be configurable via BAAAS_MCP_S3_BUCKET config property (or similar)
    // AWS credentials will be provided as ENV variables so should work directly with Quarkus

    private final DMNHashGenerator hashGenerator;

    @Inject
    public S3DMNStorage(DMNHashGenerator hashGenerator) {
        this.hashGenerator = hashGenerator;
    }

    @Override
    public void writeDMN(String customerId, DecisionsRequest decisions) {

        String hash = hashGenerator.generateHash(decisions.getModel().getDmn());

        //TODO - Write the DMN into the correct location as specified here:  https://issues.redhat.com/browse/BAAAS-41
        // e.g s3://${baaas-s3-bucket}/customers/<customer_id>/<decision_name>/<decision_version>/dmn.xml
    }

    @Override
    public void deleteDMN(String customerId, String decisionName) {
        //TODO Delete all versions of DMN for the specified decision in the specified customer account
    }

    @Override
    public String readDMN(String customerId, String decisionName, long decisionVersion) {
        //TODO - Fetch the DMN for the specified decision and version for the given customer
        //TODO Is String the best return value here? Is something else better?
        return null;
    }
}
