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
package org.kie.baaas.mcp.validators;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.kie.baaas.mcp.api.decisions.DecisionBase;
import org.kie.baaas.mcp.api.decisions.DecisionRequest;
import org.kie.baaas.mcp.api.decisions.Model;
import org.kie.baaas.mcp.validators.xml.BasicXML;

import io.quarkus.test.junit.QuarkusTest;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@QuarkusTest
public class BasicXMLValidatorTest {

    DecisionRequest myDecision;

    @Inject
    Validator validator;

    @BasicXML
    String myValidXml = "<xml><test>\"hello\"</test></xml>";

    @BasicXML
    String myInvalidXML = "<xml><test>\"hello\"</test>";

    @BeforeAll
    public void createBasePayload() {
        myDecision = new DecisionRequest();
        myDecision.setKind("Decision");
        myDecision.setName("Quarkus Test");
        myDecision.setDescription("some test");
        myDecision.setModel(new Model());
    }

    @Test
    public void testValidXML() {
        myDecision.getModel().setDmn(myValidXml);
        Set<ConstraintViolation<DecisionBase>> violations = validator.validate(myDecision);
        Assertions.assertTrue(violations.isEmpty());
    }

    @Test
    public void testInvalidXML() {
        myDecision.getModel().setDmn(myInvalidXML);
        Set<ConstraintViolation<DecisionBase>> violations = validator.validate(myDecision);
        Assertions.assertFalse(violations.isEmpty());
        Assertions.assertEquals("[XML document structures must start and end within the same entity.]",
                violations.stream()
                        .map(violation -> violation.getMessage())
                        .collect(Collectors.toList()).toString());
    }
}
