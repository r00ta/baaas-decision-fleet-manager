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

package org.kie.baaas.mcp.api.decisions;

import java.util.HashMap;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.kie.baaas.mcp.api.eventing.Eventing;

@RegisterForReflection
public abstract class DecisionBase {

    @JsonProperty("kind")
    @NotEmpty(message = "Kind cannot be blank")
    @Pattern(regexp = "Decision")
    private String kind;

    @NotEmpty
    @JsonProperty("name")
    private String name;

    @NotEmpty
    @JsonProperty("description")
    private String description;

    @Valid
    @JsonProperty("eventing")
    private Eventing eventing;

    @Valid
    @JsonProperty("configuration")
    private Map<String, String> configuration = new HashMap<>();

    @Valid
    @JsonProperty("tags")
    private Map<String, String> tags = new HashMap<>();

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Eventing getEventing() {
        return eventing;
    }

    public void setEventing(Eventing eventing) {
        this.eventing = eventing;
    }

    public Map<String, String> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, String> configuration) {
        this.configuration = configuration;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    @Override
    public String toString() {
        return "Decisions{" +
                "kind='" + kind + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", eventing=" + eventing +
                ", configuration=" + configuration +
                ", tags=" + tags +
                '}';
    }
}
