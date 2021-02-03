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

package org.kie.baaas.mcp.app.storage;

import org.kie.baaas.mcp.api.Decisions;

public interface DecisionDMNStorage {

    public void writeDMN(String customerId, Decisions decisions);

    public void deleteDMN(String customerId, String decisionName);

    public String readDMN(String customerId, String decisionName, long decisionVersion);
}
