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

package org.kie.baaas.mcp.app.controller;

import java.net.URL;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.baaas.mcp.api.decisions.DecisionRequest;
import org.kie.baaas.mcp.api.decisions.DecisionResponse;
import org.kie.baaas.mcp.api.webhook.WebhookRegistrationRequest;
import org.kie.baaas.mcp.api.webhook.WebhookResponse;
import org.kie.baaas.mcp.api.webhook.WebhookResponseList;
import org.kie.baaas.mcp.app.ccp.ClusterControlPlaneClient;
import org.kie.baaas.mcp.app.ccp.ClusterControlPlaneSelector;
import org.kie.baaas.mcp.app.ccp.client.ClusterControlPlaneClientFactory;
import org.kie.baaas.mcp.app.controller.modelmappers.DecisionMapper;
import org.kie.baaas.mcp.app.manager.DecisionLifecycleOrchestrator;
import org.kie.baaas.mcp.app.manager.DecisionManager;
import org.kie.baaas.mcp.app.model.DecisionVersion;
import org.kie.baaas.mcp.app.model.deployment.Deployment;
import org.kie.baaas.mcp.app.vault.VaultService;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.Scenario;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.getAllScenarios;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;

@QuarkusTest
@QuarkusTestResource(OidcWiremockTestResource.class)
public class WebhookResourceTest {

    private static final Logger LOG = LoggerFactory.getLogger(WebhookResourceTest.class);

    @InjectMock
    ClusterControlPlaneClientFactory clientFactory;

    @InjectMock
    ClusterControlPlaneSelector controlPlaneSelector;

    @InjectMock
    DecisionManager decisionManager;

    @Inject
    WebhookResource service;

    @Inject
    DecisionLifecycleOrchestrator decisionLifeCycleOrchestrator;

    @InjectMock
    DecisionMapper decisionMapper;

    @InjectMock
    VaultService vaultService;

    private static WireMockServer wireMockServer;
    private static final String SCENARIO_TIMEOUT = "timeout";
    private static final String TIMEOUT_STATE_1 = "timeout1";
    private static final String TIMEOUT_STATE_2 = "end";
    private static final String SCENARIO_RETRY = "retry";
    private static final String RETRY_STATE_1 = "retry1";
    private static final String RETRY_STATE_2 = "retry2";
    private static final String RETRY_STATE_3 = "retry3";

    @BeforeAll
    public static void init() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        wireMockServer = new WireMockServer(); // or new WireMockServer(WireMockConfiguration.wireMockConfig().notifier(new ConsoleNotifier(true)));
        wireMockServer.start();

        // Configure WireMockServer to react also to the built-in test webhook if wiremock binds to localhost:8080 (coming from the persistence configuration test data)
        stubFor(post(urlEqualTo("/test-builtin-webhook"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"response\":\"ok\"}")));

        // webhook for testing purposes
        stubFor(post(urlEqualTo("/mywebhook"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"response\":\"ok\"}")));
        stubFor(post(urlEqualTo("/mywebhook2"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"response\":\"ok from mywebhook2\"}")));

        // retry scenarios
        stubFor(post(urlEqualTo("/timeout-once-webhook"))
                .inScenario(SCENARIO_TIMEOUT)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"response\":\"ok\"}")
                        .withFixedDelay(12_000))
                .willSetStateTo(TIMEOUT_STATE_1));
        stubFor(post(urlEqualTo("/timeout-once-webhook"))
                .inScenario(SCENARIO_TIMEOUT)
                .whenScenarioStateIs(TIMEOUT_STATE_1)
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"response\":\"ok\"}"))
                .willSetStateTo(TIMEOUT_STATE_2));

        stubFor(post(urlEqualTo("/nonoyes-webhook"))
                .inScenario(SCENARIO_RETRY)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("error.")
                        .withFixedDelay(2_000))
                .willSetStateTo(RETRY_STATE_1));
        stubFor(post(urlEqualTo("/nonoyes-webhook"))
                .inScenario(SCENARIO_RETRY)
                .whenScenarioStateIs(RETRY_STATE_1)
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("error, again.")
                        .withFixedDelay(2_000))
                .willSetStateTo(RETRY_STATE_2));
        stubFor(post(urlEqualTo("/nonoyes-webhook"))
                .inScenario(SCENARIO_RETRY)
                .whenScenarioStateIs(RETRY_STATE_2)
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"response\":\"ok\"}"))
                .willSetStateTo(RETRY_STATE_3));
    }

    @BeforeEach
    public void start() {
        wireMockServer.resetRequests();
    }

    @AfterAll
    public static void shutdown() {
        if (null != wireMockServer) {
            wireMockServer.stop();
        }
    }

    @Test
    public void testBasic() throws Exception {
        WebhookRegistrationRequest webhook = new WebhookRegistrationRequest();
        String webhook1url = wireMockServer.baseUrl() + "/mywebhook";
        webhook.setUrl(new URL(webhook1url));
        final String w1id = given()
                .body(webhook)
                .contentType(ContentType.JSON)
                .when()
                .post("/webhooks")
                .then()
                .statusCode(200)
                .extract()
                .path("id");

        DecisionVersion decisionVersion = new DecisionVersion();
        Mockito.when(decisionManager.createOrUpdateVersion(any(), any())).thenReturn(decisionVersion);
        Mockito.when(decisionManager.deployed(any(), any(), anyLong(), any())).thenReturn(decisionVersion);
        Mockito.when(decisionManager.failed(any(), any(), anyLong(), any())).thenReturn(decisionVersion);
        DecisionResponse decisionResponse = new DecisionResponse();
        decisionResponse.setSubmittedAt(ZonedDateTime.now());
        Mockito.when(decisionMapper.mapVersionToDecisionResponse(any())).thenReturn(decisionResponse);
        Mockito.when(controlPlaneSelector.selectControlPlaneForDeployment(any())).thenReturn(null);
        ClusterControlPlaneClient clientMock = Mockito.mock(ClusterControlPlaneClient.class);
        Mockito.when(clientFactory.createClientFor(any())).thenReturn(clientMock);
        DecisionRequest decisionRequest = new DecisionRequest();
        decisionRequest.setDescription("Mocked DecisionRequest");
        decisionLifeCycleOrchestrator.createOrUpdateVersion("myCustomer", decisionRequest);

        // a total of 1 webhook is currently registered
        await().atMost(5, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(1, postRequestedFor(urlEqualTo("/mywebhook"))));

        // unregister webhook #1 via ID, and register webhook #2
        given()
                .when()
                .delete("/webhooks/{path}", w1id)
                .then()
                .statusCode(200);
        WebhookRegistrationRequest webhook2 = new WebhookRegistrationRequest();
        final String webhook2url = wireMockServer.baseUrl() + "/mywebhook2";
        webhook2.setUrl(new URL(webhook2url));
        given()
                .body(webhook2)
                .contentType(ContentType.JSON)
                .when()
                .post("/webhooks")
                .then()
                .statusCode(200);
        decisionLifeCycleOrchestrator.createOrUpdateVersion("myCustomer", decisionRequest);
        await().atMost(5, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(1, postRequestedFor(urlEqualTo("/mywebhook2"))));

        // unregister webhook #2 via URL, and register again webhook #1
        given()
                .when()
                .delete("/webhooks/{path}", webhook2url)
                .then()
                .statusCode(200);
        given()
                .body(webhook)
                .contentType(ContentType.JSON)
                .when()
                .post("/webhooks")
                .then()
                .statusCode(200);
        decisionLifeCycleOrchestrator.createOrUpdateVersion("myCustomer", decisionRequest);
        await().atMost(5, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(2, postRequestedFor(urlEqualTo("/mywebhook"))));
        verify(1, postRequestedFor(urlEqualTo("/mywebhook2")));

        // one more callback for .deployed()
        decisionLifeCycleOrchestrator.deployed("x", "x", 1L, new Deployment());
        await().atMost(5, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(3, postRequestedFor(urlEqualTo("/mywebhook"))));

        // one more callback for .failed()
        decisionLifeCycleOrchestrator.failed("x", "x", 1L, new Deployment());
        await().atMost(5, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(4, postRequestedFor(urlEqualTo("/mywebhook"))));

        // unregister webhook #1 via URL.
        given()
                .when()
                .delete("/webhooks/{path}", webhook1url)
                .then()
                .statusCode(200);
    }

    @Test
    public void testNotFoundWebhook() {
        given()
                .when()
                .delete("/webhooks/{path}", "http://redhat.com/not-existing-webhook")
                .then()
                .statusCode(404);
    }

    @Test
    public void testAlreadyExistingWebhook() throws Exception {
        WebhookRegistrationRequest webhook = new WebhookRegistrationRequest();
        String webhook1url = "http://localhost:8080/test-builtin-webhook"; // taken from test data
        webhook.setUrl(new URL(webhook1url));
        given()
                .body(webhook)
                .contentType(ContentType.JSON)
                .when()
                .post("/webhooks")
                .then()
                .statusCode(400);
    }

    @Test
    public void testListWebhooks() {
        WebhookResponseList webhooks = given()
                .when()
                .get("/webhooks")
                .then()
                .statusCode(200)
                .extract().as(WebhookResponseList.class);
        assertEquals(1L, webhooks.getSize());
        assertEquals(1L, webhooks.getTotal());
        assertEquals(0L, webhooks.getPage());
        WebhookResponse webhook0 = webhooks.getItems().get(0);
        assertEquals("test-builtin-webhook", webhook0.getId()); // taken from test data
        assertEquals("http://localhost:8080/test-builtin-webhook", webhook0.getUrl().toString());
    }

    @Test
    public void testMetrics() throws Exception {
        WebhookRegistrationRequest webhook = new WebhookRegistrationRequest();
        String webhook1url = wireMockServer.baseUrl() + "/mywebhook";
        webhook.setUrl(new URL(webhook1url));
        final String w1id = given()
                .body(webhook)
                .contentType(ContentType.JSON)
                .when()
                .post("/webhooks")
                .then()
                .statusCode(200)
                .extract()
                .path("id");

        DecisionVersion decisionVersion = new DecisionVersion();
        Mockito.when(decisionManager.createOrUpdateVersion(any(), any())).thenReturn(decisionVersion);
        Mockito.when(controlPlaneSelector.selectControlPlaneForDeployment(any())).thenReturn(null);
        ClusterControlPlaneClient clientMock = Mockito.mock(ClusterControlPlaneClient.class);
        Mockito.when(clientFactory.createClientFor(any())).thenReturn(clientMock);
        DecisionRequest decisionRequest = new DecisionRequest();
        decisionRequest.setDescription("Mocked DecisionRequest");
        decisionLifeCycleOrchestrator.createOrUpdateVersion("myCustomer", decisionRequest);

        // a total of 1 webhook is currently registered
        await().atMost(5, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(1, postRequestedFor(urlEqualTo("/mywebhook"))));

        String qMetrics = given()
                .when()
                .get("/q/metrics")
                .then()
                .statusCode(200)
                .extract().asString();
        assertTrue(qMetrics.contains("daaas_webhook_invocations"));
        assertTrue(qMetrics.contains("daaas_webhook_success"));

        // unregister webhook #1 via URL.
        given()
                .when()
                .delete("/webhooks/{path}", webhook1url)
                .then()
                .statusCode(200);
    }

    @Test
    public void testTimeoutThenOK() throws Exception {
        LOG.warn("⎽⎼⎻⎺⎺⎻⎼⎽⎽⎼⎻⎺⎺⎻⎼⎽⎽⎼⎻⎺⎺⎻⎼⎽ This test scenario will take a little more time to complete, as expected (max 20s) ⎽⎼⎻⎺⎺⎻⎼⎽⎽⎼⎻⎺⎺⎻⎼⎽⎽⎼⎻⎺⎺⎻⎼⎽");
        WebhookRegistrationRequest webhook = new WebhookRegistrationRequest();
        String webhook1url = wireMockServer.baseUrl() + "/timeout-once-webhook";
        webhook.setUrl(new URL(webhook1url));
        given()
                .body(webhook)
                .contentType(ContentType.JSON)
                .when()
                .post("/webhooks")
                .then()
                .statusCode(200);

        DecisionVersion decisionVersion = new DecisionVersion();
        Mockito.when(decisionManager.createOrUpdateVersion(any(), any())).thenReturn(decisionVersion);
        Mockito.when(controlPlaneSelector.selectControlPlaneForDeployment(any())).thenReturn(null);
        ClusterControlPlaneClient clientMock = Mockito.mock(ClusterControlPlaneClient.class);
        Mockito.when(clientFactory.createClientFor(any())).thenReturn(clientMock);
        DecisionRequest decisionRequest = new DecisionRequest();
        decisionRequest.setDescription("Mocked DecisionRequest");
        decisionLifeCycleOrchestrator.createOrUpdateVersion("myCustomer", decisionRequest);

        await().atMost(20, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(2, postRequestedFor(urlEqualTo("/timeout-once-webhook"))));
        assertEquals(TIMEOUT_STATE_2, getAllScenarios().stream().filter(s -> s.getName().equals(SCENARIO_TIMEOUT)).findFirst().get().getState());

        // unregister webhook via URL.
        given()
                .when()
                .delete("/webhooks/{path}", webhook1url)
                .then()
                .statusCode(200);
    }

    @Test
    public void testNoNoYes() throws Exception {
        WebhookRegistrationRequest webhook = new WebhookRegistrationRequest();
        String webhook1url = wireMockServer.baseUrl() + "/nonoyes-webhook";
        webhook.setUrl(new URL(webhook1url));
        given()
                .body(webhook)
                .contentType(ContentType.JSON)
                .when()
                .post("/webhooks")
                .then()
                .statusCode(200);

        DecisionVersion decisionVersion = new DecisionVersion();
        Mockito.when(decisionManager.createOrUpdateVersion(any(), any())).thenReturn(decisionVersion);
        Mockito.when(controlPlaneSelector.selectControlPlaneForDeployment(any())).thenReturn(null);
        ClusterControlPlaneClient clientMock = Mockito.mock(ClusterControlPlaneClient.class);
        Mockito.when(clientFactory.createClientFor(any())).thenReturn(clientMock);
        DecisionRequest decisionRequest = new DecisionRequest();
        decisionRequest.setDescription("Mocked DecisionRequest");
        decisionLifeCycleOrchestrator.createOrUpdateVersion("myCustomer", decisionRequest);

        await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(3, postRequestedFor(urlEqualTo("/nonoyes-webhook"))));
        assertEquals(RETRY_STATE_3, getAllScenarios().stream().filter(s -> s.getName().equals(SCENARIO_RETRY)).findFirst().get().getState());

        // unregister webhook via URL.
        given()
                .when()
                .delete("/webhooks/{path}", webhook1url)
                .then()
                .statusCode(200);
    }
}
