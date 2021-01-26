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
import org.kie.baaas.mcp.validator.xml.BasicXML;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "dmn",
        "url"
})
@RegisterForReflection
public class Model {

    @JsonProperty("dmn")
    // Validate XML
    @Valid
    @BasicXML
    private String dmn;

    // For now a very simplistic url validator
    @Valid
    @JsonProperty("url")
    // just a simplistic validation for now, only https
    @Pattern(regexp = "https://.*\\.dmn")
    private String url;

    @JsonProperty("sha256")
    private String sha256;

    @JsonProperty("md5")
    private String md5;

    public String getDmn() {
        return dmn;
    }

    public void setDmn(String dmn) {
        this.dmn = dmn;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }
}
