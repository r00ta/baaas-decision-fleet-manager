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

package org.kie.baaas.mcp.app.storage;

import java.util.Objects;

/**
 * Encapsulates the result of writing DMN to the configured provider storage.
 */
public class DMNStorageRequest {

    private final String providerUrl;

    private final String md5Hash;

    public DMNStorageRequest(String providerUrl, String md5Hash) {
        Objects.requireNonNull(providerUrl, "providerUrl cannot be null");
        Objects.requireNonNull(md5Hash, "md5Hash cannot be null");

        this.providerUrl = providerUrl;
        this.md5Hash = md5Hash;
    }

    /**
     * Returns the provider specific URL where the DMN has been stored.
     *
     * @return - The URL at which the provider has stored the DMN
     */
    public String getProviderUrl() {
        return providerUrl;
    }

    /**
     * The MD5 hash of the DMN that was written into storage.
     *
     * @return - The MD5 hash of the DMN written to storage.
     */
    public String getMd5Hash() {
        return md5Hash;
    }
}
