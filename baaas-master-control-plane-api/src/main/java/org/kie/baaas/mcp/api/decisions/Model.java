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

import java.util.Objects;

import org.kie.baaas.mcp.validators.xml.BasicXML;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "dmn"
})
@RegisterForReflection
public class Model {

    @JsonProperty("dmn")
    @BasicXML
    private String dmn;

    public String getDmn() {
        return dmn;
    }

    public void setDmn(String dmn) {
        this.dmn = dmn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Model model = (Model) o;
        return Objects.equals(dmn, model.dmn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dmn);
    }

    @Override
    public String toString() {
        return "Model{" +
                "dmn='" + dmn + '\'' +
                '}';
    }
}
