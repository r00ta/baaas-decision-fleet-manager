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

package org.kie.baaas.mcp.api.eventing;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.kie.baaas.mcp.api.eventing.kafka.Kafka;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "kafka"
})
@RegisterForReflection
public class Eventing {

    @JsonProperty("kafka")
    private Kafka kafka;

    @JsonProperty("kafka")
    public Kafka getKafka() {
        return kafka;
    }

    @JsonProperty("kafka")
    public void setKafka(Kafka kafka) {
        this.kafka = kafka;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Eventing eventing = (Eventing) o;
        return Objects.equals(kafka, eventing.kafka);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kafka);
    }

    @Override
    public String toString() {
        return "Eventing{" +
                "kafka=" + kafka +
                '}';
    }
}