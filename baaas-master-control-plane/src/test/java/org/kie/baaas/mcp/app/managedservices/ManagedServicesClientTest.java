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

import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.baaas.mcp.app.vault.Secret;

import com.github.tomakehurst.wiremock.client.WireMock;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.RestAssured;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.kie.baaas.mcp.app.managedservices.ManagedServicesClient.*;
import static org.kie.baaas.mcp.app.managedservices.ManagedServicesClient.CLIENT_SECRET;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@QuarkusTest
@QuarkusTestResource(WireMockManagedServices.class)
class ManagedServicesClientTest {

    @Inject
    ManagedServicesClient client;

    @InjectMock
    JsonWebToken token;

    @ConfigProperty(name = "mock.managed-services.port")
    Integer port;

    private WireMock wireMock;

    @BeforeAll
    static void init() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @BeforeEach
    void setUp() {
        wireMock = new WireMock(port);
        wireMock.resetRequests();
    }

    @Test
    void testCreateNewServiceAccount() {

        Secret secret = client.createOrReplaceServiceAccount("foo");

        assertThat(secret, is(notNullValue()));
        assertThat(secret.getId(), is("name-foo"));
        assertThat(secret.getValues().get(CLIENT_ID), is("client-foo"));
        assertThat(secret.getValues().get(CLIENT_SECRET), is("secret-foo"));
        verify(token, times(1)).getRawToken();
        wireMock.verifyThat(1, getRequestedFor(urlEqualTo("/api/managed-services-api/v1/serviceaccounts")));
        wireMock.verifyThat(1, postRequestedFor(urlEqualTo("/api/managed-services-api/v1/serviceaccounts")));
    }

    @Test
    void testResetServiceAccountCredentials() {

        Secret secret = client.createOrReplaceServiceAccount("name-5");

        assertThat(secret, is(notNullValue()));
        assertThat(secret.getId(), is("name-5"));
        assertThat(secret.getValues().get(CLIENT_ID), is("client-5"));
        assertThat(secret.getValues().get(CLIENT_SECRET), is("secret-5"));
        verify(token, times(1)).getRawToken();
        wireMock.verifyThat(1, getRequestedFor(urlEqualTo("/api/managed-services-api/v1/serviceaccounts")));
        wireMock.verifyThat(1, postRequestedFor(urlEqualTo("/api/managed-services-api/v1/serviceaccounts/id-5/reset-credentials")));
    }

    @Test
    void testErrorResetServiceAccountCredentials() {
        assertThrows(ManagedServicesException.class,
                () -> client.createOrReplaceServiceAccount("name-9"),
                "Unable to createOrReplace Service Account: name-9");

        wireMock.verifyThat(1, getRequestedFor(urlEqualTo("/api/managed-services-api/v1/serviceaccounts")));
        wireMock.verifyThat(1, postRequestedFor(urlEqualTo("/api/managed-services-api/v1/serviceaccounts/id-9/reset-credentials")));
    }
}
