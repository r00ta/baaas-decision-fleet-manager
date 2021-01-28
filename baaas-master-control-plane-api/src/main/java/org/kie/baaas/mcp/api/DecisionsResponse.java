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

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "kind",
        "id",
        "version",
        "href",
        "name",
        "description",
        "model",
        "eventing",
        "configuration",
        "tags",
        "submitted_at",
        "violations"
})
@RegisterForReflection
public class DecisionsResponse {

    @JsonProperty("kind")
    private String kind;

    @JsonProperty("id")
    private long id;

    @JsonProperty("version")
    private String version;

    @JsonProperty("href")
    private String href;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("model")
    private Model model;

    @JsonProperty("eventing")
    private Eventing eventing;

    @JsonProperty("configuration")
    private Configuration configuration;

    @JsonProperty("tags")
    private Tags tags;

    @JsonProperty("submitted_at")
    private String submittedAt;

    @JsonProperty("violations")
    private String violations;

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
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

    public Model getModel() {
        return model;
    }

    public void setModel(Model model) {
        this.model = model;
    }

    public Eventing getEventing() {
        return eventing;
    }

    public void setEventing(Eventing eventing) {
        this.eventing = eventing;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public Tags getTags() {
        return tags;
    }

    public void setTags(Tags tags) {
        this.tags = tags;
    }

    public String getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(String submittedAt) {
        this.submittedAt = submittedAt;
    }

    public String getViolations() {
        return violations;
    }

    public void setViolations(String violations) {
        this.violations = violations;
    }

    @Override
    public String toString() {
        return "DecisionsResponse{" +
                "kind='" + kind + '\'' +
                ", id=" + id +
                ", version='" + version + '\'' +
                ", href='" + href + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", model=" + model +
                ", eventing=" + eventing +
                ", configuration=" + configuration +
                ", tags=" + tags +
                ", submitted_at=" + submittedAt +
                ", violations=" + violations +
                '}';
    }
}

