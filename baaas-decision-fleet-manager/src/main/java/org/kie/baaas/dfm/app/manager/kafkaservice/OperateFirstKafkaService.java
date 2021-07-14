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

package org.kie.baaas.dfm.app.manager.kafkaservice;

import org.kie.baaas.dfm.app.manager.KafkaService;
import org.kie.baaas.dfm.app.model.eventing.Credential;

public class OperateFirstKafkaService implements KafkaService {
    @Override
    public Credential getCustomerCredential(String customerId) {
        throw new KafkaServiceNotSupportedException("OperateFirstKafkaService is currently not defined.");
    }
}
