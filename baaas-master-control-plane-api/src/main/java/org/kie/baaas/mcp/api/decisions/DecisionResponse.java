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

import java.time.ZonedDateTime;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonInclude(JsonInclude.Include.NON_NULL)
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

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
    @JsonProperty("submitted_at")
    private ZonedDateTime submittedAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
    @JsonProperty("published_at")
    private ZonedDateTime publishedAt;

    @JsonProperty("status")
    private String status;

    @JsonProperty("status_message")
    private String statusMessage;

    @JsonProperty("version_endpoint")
    private String versionEndpoint;

    @JsonProperty("current_endpoint")
    private String currentEndpoint;

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

    public ZonedDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(ZonedDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public ZonedDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(ZonedDateTime publishedAt) {
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

    public String getVersionEndpoint() {
        return versionEndpoint;
    }

    public void setVersionEndpoint(String versionEndpoint) {
        this.versionEndpoint = versionEndpoint;
    }

    public String getCurrentEndpoint() {
        return currentEndpoint;
    }

    public void setCurrentEndpoint(String currentEndpoint) {
        this.currentEndpoint = currentEndpoint;
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
