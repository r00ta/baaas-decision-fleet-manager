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

package org.kie.baaas.mcp.app.managedservices;

import java.time.ZonedDateTime;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.openshift.cloud.api.kas.models.ServiceAccount;
import com.openshift.cloud.api.kas.models.ServiceAccountList;
import com.openshift.cloud.api.kas.models.ServiceAccountListItem;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.Assert.fail;

public class WireMockManagedServices implements QuarkusTestResourceLifecycleManager {

    private static final WireMockServer MOCK_SERVER = new WireMockServer(options().dynamicPort());;

    ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public Map<String, String> start() {
        MOCK_SERVER.start();

        MOCK_SERVER.stubFor(get(urlEqualTo("/api/managed-services-api/v1/serviceaccounts"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withBody(getSAList())));

        MOCK_SERVER.stubFor(post(urlEqualTo("/api/managed-services-api/v1/serviceaccounts"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withBody(getSA("foo"))));

        MOCK_SERVER.stubFor(post(urlEqualTo("/api/managed-services-api/v1/serviceaccounts/id-5/reset-credentials"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withBody(getSA("5"))));

        MOCK_SERVER.stubFor(post(urlEqualTo("/api/managed-services-api/v1/serviceaccounts/id-9/reset-credentials"))
                .willReturn(aResponse()
                        .withStatus(500)));

        return Map.of("baaas.mcp.managed-services.endpoint", MOCK_SERVER.baseUrl(), "mock.managed-services.port", String.valueOf(MOCK_SERVER.port()));
    }

    @Override
    public void stop() {
        if (MOCK_SERVER != null) {
            MOCK_SERVER.stop();
        }
    }

    private String getSAList() {
        ServiceAccountList list = new ServiceAccountList();
        for (int i = 1; i <= 10; i++) {
            ServiceAccountListItem item = new ServiceAccountListItem();
            item.setId("id-" + i);
            item.setName("name-" + i);
            item.setDescription("description-" + i);
            item.setCreatedAt(ZonedDateTime.now().toOffsetDateTime());
            item.setOwner("owner-" + i);
            list.addItemsItem(item);
        }
        return serialize(list);
    }

    private String getSA(String id) {
        ServiceAccount sa = new ServiceAccount();
        sa.setId("id-" + id);
        sa.setName("name-" + id);
        sa.setDescription("description-" + id);
        sa.setCreatedAt(ZonedDateTime.now().toOffsetDateTime());
        sa.setOwner("owner-" + id);
        sa.setClientID("client-" + id);
        sa.setClientSecret("secret-" + id);
        return serialize(sa);

    }

    private String serialize(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            fail("Unexpected serialization error. " + e.getMessage());
        }
        return null;
    }
}
