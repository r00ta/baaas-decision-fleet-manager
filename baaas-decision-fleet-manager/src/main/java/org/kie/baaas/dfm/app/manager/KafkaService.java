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

package org.kie.baaas.dfm.app.manager;

import org.kie.baaas.dfm.app.model.eventing.Credential;

public interface KafkaService {
    /**
     * This method connects to Kafka service, and creates/replaces service account.
     * The credential of the service account is collected and stored in a vault.
     */
    Credential getCustomerCredential(String customerId);
}
