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

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.baaas.dfm.app.managedservices.ManagedServicesClient;
import org.kie.baaas.dfm.app.managedservices.ManagedServicesException;
import org.kie.baaas.dfm.app.model.eventing.Credential;
import org.kie.baaas.dfm.app.vault.Secret;
import org.kie.baaas.dfm.app.vault.VaultService;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.openshift.cloud.api.kas.invoker.ApiException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ManagedKafkaServiceMockTest {
    @Mock
    VaultService vaultService;

    @Mock
    ManagedServicesClient managedServicesClient;

    @Test
    void testManagedServicesClient_new_service_account() {
        String customerId = "foo";
        String saName = "daas-" + customerId + "-credentials";
        ManagedKafkaService kafkaService = new ManagedKafkaService(managedServicesClient, vaultService);

        Secret secret = new Secret().setId(saName).setValues(Map.of(ManagedServicesClient.CLIENT_ID, "foo", ManagedServicesClient.CLIENT_SECRET, "bar"));
        when(managedServicesClient.createOrReplaceServiceAccount(saName)).thenReturn(secret);

        Credential credential = kafkaService.getCustomerCredential(customerId);
        assertThat(credential, is(notNullValue()));

        verify(vaultService, times(1)).get(eq(saName));
        verify(managedServicesClient, times(1)).createOrReplaceServiceAccount(eq(saName));
        verify(vaultService, times(1)).create(eq(secret));
    }

    @Test
    void testManagedServicesClient_error_creatingSA() {
        String customerId = "foo";
        String saName = "daas-" + customerId + "-credentials";
        ManagedKafkaService kafkaService = new ManagedKafkaService(managedServicesClient, vaultService);

        when(managedServicesClient.createOrReplaceServiceAccount(anyString()))
                .thenThrow(new ManagedServicesException("some error", new ApiException("api error")));

        assertThrows(ManagedServicesException.class, () -> kafkaService.getCustomerCredential(customerId));

        verify(vaultService, times(1)).get(eq(saName));
        verify(managedServicesClient, times(1)).createOrReplaceServiceAccount(eq(saName));
        verify(vaultService, times(0)).create(any(Secret.class));
    }
}
