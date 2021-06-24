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

package org.kie.baaas.mcp.app.vault.aws;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class AWSVaultConfig {

    final String region;

    final Optional<String> endpoint;

    final String accessKeyId;

    final String secretAccessKey;

    public AWSVaultConfig(@ConfigProperty(name = "baaas.mcp.secrets-manager.aws.region") String region,
            @ConfigProperty(name = "baaas.mcp.secrets-manager.aws.endpoint-override") Optional<String> endpoint,
            @ConfigProperty(name = "baaas.mcp.secrets-manager.aws.access-key-id") String accessKeyId,
            @ConfigProperty(name = "baaas.mcp.secrets-manager.aws.secret-access-key") String secretAccessKey) {
        this.region = region;
        this.endpoint = endpoint;
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
    }

    public String getRegion() {
        return region;
    }

    public Optional<String> getEndpoint() {
        return endpoint;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public String getSecretAccessKey() {
        return secretAccessKey;
    }

    @Override
    public String toString() {
        return "AWSVaultConfig{" +
                "region='" + getRegion() + '\'' +
                ", endpoint=" + getEndpoint() +
                ", accessKeyId='" + redact(getAccessKeyId()) + '\'' +
                ", secretAccessKey='" + redact(getSecretAccessKey()) + '\'' +
                '}';
    }

    private String redact(String s) {
        if (s == null || s.isBlank()) {
            return "empty";
        }
        int clearChars = s.length() / 4;
        return s.substring(0, s.length() - clearChars)
                .replaceAll(".", "*")
                .concat(s.substring(s.length() - clearChars));
    }
}
