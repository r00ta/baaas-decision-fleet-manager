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

package org.kie.baaas.mcp.api;

import java.net.URL;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Represents instances of the DMN JIT that can be invoked by the user to test their
 * DMN as part of the Decision Authoring lifecycle.
 */
@JsonPropertyOrder({
        "kind",
        "url"
})
@RegisterForReflection
public class DMNJIT {

    @JsonProperty("kind")
    private final String kind = "DMNJIT";

    @JsonProperty("url")
    private URL url;

    public String getKind() {
        return kind;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }
}
