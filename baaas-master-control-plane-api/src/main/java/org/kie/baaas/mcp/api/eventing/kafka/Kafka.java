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

package org.kie.baaas.mcp.api.eventing.kafka;

import java.util.Objects;

import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Kafka object that defines a input and output stream
 * for eventing.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@RegisterForReflection
public class Kafka {

    /**
     * The input source that identifies a Kafka Stream to be used for
     * Decision input
     */
    @JsonProperty("source")
    @NotBlank
    private String source;
    /**
     * The output sink that identifies a Kafka Stream to be used for
     * Decision Output
     */
    @JsonProperty("sink")
    @NotBlank
    private String sink;

    @JsonProperty("bootstrap_servers")
    @NotBlank
    private String bootstrapServers;

    public String getSource() {
        return source;
    }

    public Kafka setSource(String source) {
        this.source = source;
        return this;
    }

    public String getSink() {
        return sink;
    }

    public Kafka setSink(String sink) {
        this.sink = sink;
        return this;
    }

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public Kafka setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Kafka))
            return false;
        Kafka kafka = (Kafka) o;
        return Objects.equals(source, kafka.source) &&
                Objects.equals(sink, kafka.sink) &&
                Objects.equals(bootstrapServers, kafka.bootstrapServers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, sink, bootstrapServers);
    }

    @Override
    public String toString() {
        return "Kafka{" +
                "source='" + source + '\'' +
                ", sink='" + sink + '\'' +
                ", bootstrapServers='" + bootstrapServers + '\'' +
                '}';
    }
}
