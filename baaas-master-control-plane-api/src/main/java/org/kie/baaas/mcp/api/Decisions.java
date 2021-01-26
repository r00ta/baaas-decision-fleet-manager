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

package org.kie.baaas.mcp.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.quarkus.runtime.annotations.RegisterForReflection;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "kind",
        "name",
        "description",
        "model",
        "eventing",
        "configuration",
        "tags"
})
@RegisterForReflection
public class Decisions {

    @JsonProperty("kind")
    @NotBlank(message = "Kind cannot be blank")
    @Pattern(regexp = "Decision")
    private String kind;

    @JsonProperty("name")
    @NotBlank(message = "cannot be empty")
    private String name;

    @JsonProperty("description")
    @NotBlank(message = "cannot be empty")
    private String description;

    @Valid
    @JsonProperty("model")
    private Model model;

    @Valid
    @JsonProperty("eventing")
    private Eventing eventing;

    @Valid
    @JsonProperty("configuration")
    private Configuration configuration;

    @Valid
    @JsonProperty("tags")
    private Tags tags;



    @JsonProperty("kind")
    public String getKind() {
        return kind;
    }

    @JsonProperty("kind")
    public void setKind(String kind) {
        this.kind = kind;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
    }

    @JsonProperty("model")
    public Model getModel() {
        return model;
    }

    @JsonProperty("model")
    public void setModel(Model model) {

        this.model = model;
    }

    @JsonProperty("eventing")
    public Eventing getEventing() {
        return eventing;
    }

    @JsonProperty("eventing")
    public void setEventing(Eventing eventing) {
        this.eventing = eventing;
    }

    @JsonProperty("configuration")
    public Configuration getConfiguration() {
        return configuration;
    }

    @JsonProperty("configuration")
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    @JsonProperty("tags")
    public Tags getTags() {
        return tags;
    }

    @JsonProperty("tags")
    public void setTags(Tags tags) {
        this.tags = tags;
    }

    @Override
    public String toString() {
        return "Decisions{" +
                "kind='" + kind + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", model=" + model +
                ", eventing=" + eventing +
                ", configuration=" + configuration +
                ", tags=" + tags +
                '}';
    }
}
