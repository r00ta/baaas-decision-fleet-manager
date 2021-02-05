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

import java.time.LocalDateTime;
import java.util.Objects;

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
        "url",
        "status",
        "status_message",
        "submitted_at",
        "published_at"
})
@RegisterForReflection
public class DecisionResponse extends DecisionBase {

    @JsonProperty("kind")
    private final String kind = "Decision";

    @JsonProperty("id")
    private String id;

    @JsonProperty("version")
    private long version;

    @JsonProperty("href")
    private String href;

    @JsonProperty("model")
    private ResponseModel responseModel;

    @JsonProperty("submitted_at")
    private LocalDateTime submittedAt;

    @JsonProperty("published_at")
    private LocalDateTime publishedAt;

    @JsonProperty("status")
    private String status;

    @JsonProperty("status_message")
    private String statusMessage;

    @JsonProperty("url")
    private String url;

    @Override
    public String getKind() {
        return kind;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public ResponseModel getResponseModel() {
        return responseModel;
    }

    public void setResponseModel(ResponseModel responseModel) {
        this.responseModel = responseModel;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DecisionResponse that = (DecisionResponse) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "DecisionsResponse{" +
                "kind='" + kind + '\'' +
                ", id=" + id +
                ", version=" + version +
                ", href='" + href + '\'' +
                ", responseModel=" + responseModel +
                ", submittedAt='" + submittedAt + '\'' +
                '}';
    }
}

