/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.baaas.dfm.app.controller;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;
import org.kie.baaas.dfm.api.decisions.DecisionRequest;
import org.kie.baaas.dfm.api.eventing.Eventing;
import org.kie.baaas.dfm.api.eventing.kafka.Kafka;
import org.kie.baaas.dfm.app.controller.modelmappers.DecisionMapper;
import org.kie.baaas.dfm.app.manager.DecisionLifecycleOrchestrator;
import org.kie.baaas.dfm.app.resolvers.CustomerIdResolver;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.security.TestSecurity;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.kie.baaas.dfm.app.TestConstants.DEFAULT_CUSTOMER_ID;

@QuarkusTest
class DecisionResourceRestTest {

    @InjectMock
    CustomerIdResolver customerIdResolver;
    @InjectMock
    DecisionLifecycleOrchestrator decisionLifecycle;
    @InjectMock
    DecisionMapper decisionMapper;

    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    @Test
    void testBasicValidation() {
        DecisionRequest request = new DecisionRequest();

        List<String> errors = given()
                .when()
                .body(request)
                .contentType(MediaType.APPLICATION_JSON)
                .post("/decisions")
                .then()
                .statusCode(400)
                .extract().body().as(ArrayList.class);

        assertThat(errors, hasSize(3));
        request.setName("example-request");

        errors = given()
                .when()
                .body(request)
                .contentType(MediaType.APPLICATION_JSON)
                .post("/decisions")
                .then()
                .statusCode(400)
                .extract().body().as(ArrayList.class);
        assertThat(errors, hasSize(2));

        request.setKind("some kind");
        errors = given()
                .when()
                .body(request)
                .contentType(MediaType.APPLICATION_JSON)
                .post("/decisions")
                .then()
                .statusCode(400)
                .extract().body().as(ArrayList.class);
        assertThat(errors, hasSize(2));

        request.setKind("Decision");
        errors = given()
                .when()
                .body(request)
                .contentType(MediaType.APPLICATION_JSON)
                .post("/decisions")
                .then()
                .statusCode(400)
                .extract().body().as(ArrayList.class);
        assertThat(errors, hasSize(1));

        request.setDescription("the description");

        given()
                .when()
                .body(request)
                .contentType(MediaType.APPLICATION_JSON)
                .post("/decisions")
                .then()
                .statusCode(201);

    }

    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    @Test
    void testEventingValidation() {
        DecisionRequest request = new DecisionRequest();
        request.setName("example-request");
        request.setDescription("the description");
        request.setKind("Decision");
        request.setEventing(new Eventing());
        request.getEventing().setKafka(new Kafka());

        List<String> errors = given()
                .when()
                .body(request)
                .contentType(MediaType.APPLICATION_JSON)
                .post("/decisions")
                .then()
                .statusCode(400)
                .extract().body().as(ArrayList.class);
        assertThat(errors, hasSize(3));

        request.getEventing().getKafka().setSource("the source");
        errors = given()
                .when()
                .body(request)
                .contentType(MediaType.APPLICATION_JSON)
                .post("/decisions")
                .then()
                .statusCode(400)
                .extract().body().as(ArrayList.class);
        assertThat(errors, hasSize(2));

        request.getEventing().getKafka().setSink("the sink");
        errors = given()
                .when()
                .body(request)
                .contentType(MediaType.APPLICATION_JSON)
                .post("/decisions")
                .then()
                .statusCode(400)
                .extract().body().as(ArrayList.class);
        assertThat(errors, hasSize(1));

        request.getEventing().getKafka().setBootstrapServers("example:9002");
        given()
                .when()
                .body(request)
                .contentType(MediaType.APPLICATION_JSON)
                .post("/decisions")
                .then()
                .statusCode(201);
    }
}
