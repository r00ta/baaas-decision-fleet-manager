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

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.kie.baaas.dfm.app.manager.KafkaService;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
@TestProfile(InvalidKafkaServiceConfigTestProfile.class)
public class InvalidKafkaServiceTest {
    @Inject
    KafkaService kafkaService;

    @Inject
    KafkaServiceProducer kafkaServiceProducer;

    @Test
    void testGetCustomerCredential_with_invalid_kafka_service() {
        assertThrows(UnsupportedOperationException.class, () -> kafkaService.getCustomerCredential("id"));

        boolean isKafkaDefined = kafkaServiceProducer.isKafkaServiceDefined();
        assertThat(isKafkaDefined, equalTo(false));
    }
}
