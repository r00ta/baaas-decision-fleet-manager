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

import org.kie.baaas.dfm.app.managedservices.ManagedServicesClient;
import org.kie.baaas.dfm.app.manager.KafkaService;
import org.kie.baaas.dfm.app.model.eventing.Credential;
import org.kie.baaas.dfm.app.vault.Secret;
import org.kie.baaas.dfm.app.vault.VaultService;

import static org.kie.baaas.dfm.app.managedservices.ManagedServicesClient.CLIENT_ID;
import static org.kie.baaas.dfm.app.managedservices.ManagedServicesClient.CLIENT_SECRET;

public class ManagedKafkaService implements KafkaService {
    private ManagedServicesClient managedServicesClient;
    private VaultService vaultService;
    private static final String CREDENTIALS_NAME = "daas-%s-credentials";

    public ManagedKafkaService(ManagedServicesClient managedServicesClient, VaultService vaultService) {
        this.managedServicesClient = managedServicesClient;
        this.vaultService = vaultService;
    }

    public Credential getCustomerCredential(String customerId) {
        String secretName = String.format(CREDENTIALS_NAME, customerId);
        Secret secret = vaultService.get(secretName);
        if (secret == null) {
            secret = managedServicesClient.createOrReplaceServiceAccount(secretName);
            vaultService.create(secret);
        }
        return toCredential(secret);
    }

    private Credential toCredential(Secret secret) {
        if (secret == null || secret.getValues() == null) {
            return null;
        }
        return new Credential()
                .setClientId(secret.getValues().get(CLIENT_ID))
                .setClientSecret(secret.getValues().get(CLIENT_SECRET));
    }

}
