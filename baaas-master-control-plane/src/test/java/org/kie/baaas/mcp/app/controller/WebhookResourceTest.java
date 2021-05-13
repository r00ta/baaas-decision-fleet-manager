package org.kie.baaas.mcp.app.controller;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kie.baaas.mcp.api.decisions.DecisionRequest;
import org.kie.baaas.mcp.api.webhook.WebhookRegistrationRequest;
import org.kie.baaas.mcp.app.ccp.ClusterControlPlaneClient;
import org.kie.baaas.mcp.app.ccp.ClusterControlPlaneSelector;
import org.kie.baaas.mcp.app.ccp.client.ClusterControlPlaneClientFactory;
import org.kie.baaas.mcp.app.manager.DecisionLifecycleOrchestrator;
import org.kie.baaas.mcp.app.manager.DecisionManager;
import org.kie.baaas.mcp.app.model.DecisionVersion;
import org.mockito.Mockito;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;

@QuarkusTest
public class WebhookResourceTest {

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

    private static WireMockServer wireMockServer;

    @BeforeAll
    public static void init() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().notifier(new ConsoleNotifier(true)));
        wireMockServer.start();

        stubFor(post(urlEqualTo("/mywebhook"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"response\":\"ok\"}")));
        stubFor(post(urlEqualTo("/mywebhook2"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"response\":\"ok from mywebhook2\"}")));
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
        webhook.setUrl(new URL(wireMockServer.baseUrl() + "/mywebhook"));
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
    }
}
