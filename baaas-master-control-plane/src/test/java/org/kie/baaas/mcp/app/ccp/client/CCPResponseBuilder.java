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

package org.kie.baaas.mcp.app.ccp.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.mockwebserver.utils.ResponseProvider;
import okhttp3.Headers;
import okhttp3.mockwebserver.RecordedRequest;

public class CCPResponseBuilder<T> implements ResponseProvider<Object> {

    Class<T> clazz;

    T payload;

    boolean invoked = false;

    public CCPResponseBuilder(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public int getStatusCode(RecordedRequest request) {
        return 200;
    }

    @Override
    public Headers getHeaders() {
        return new Headers.Builder().build();
    }

    @Override
    public void setHeaders(Headers headers) {

    }

    @Override
    public T getBody(RecordedRequest request) {
        this.invoked = true;
        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            payload = objectMapper.readValue(request.getBody().inputStream(), clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public T getPayload() {
        return this.payload;
    }

    public boolean isInvoked() {
        return this.invoked;
    }
}
