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

package org.kie.baaas.mcp.app.vault;

import java.util.HashMap;
import java.util.Map;

public class Secret {

    private String id;
    private Map<String, String> values;

    public String getId() {
        return id;
    }

    public Secret setId(String id) {
        this.id = id;
        return this;
    }

    public Map<String, String> getValues() {
        return values;
    }

    public Secret setValues(Map<String, String> values) {
        this.values = values;
        return this;
    }

    public Secret value(String key, String value) {
        if (this.values == null) {
            this.values = new HashMap<>();
        }
        this.values.put(key, value);
        return this;
    }
}
