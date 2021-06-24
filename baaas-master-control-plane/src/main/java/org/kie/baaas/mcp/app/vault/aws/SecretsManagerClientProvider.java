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

import java.net.URI;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;

public class SecretsManagerClientProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretsManagerClientProvider.class);

    private final SecretsManagerClient client;

    @Inject
    public SecretsManagerClientProvider(AWSVaultConfig config) {
        SecretsManagerClientBuilder builder = SecretsManagerClient.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .credentialsProvider(StaticCredentialsProvider
                        .create(AwsBasicCredentials.create(config.getAccessKeyId(),
                                config.getSecretAccessKey())))
                .region(Region.of(config.getRegion()));
        if (config.getEndpoint().isPresent()) {
            builder.endpointOverride(URI.create(config.getEndpoint().get()));
        }
        LOGGER.info("Configued secrets manager client {}", config);
        this.client = builder.build();
    }

    @ApplicationScoped
    public SecretsManagerClient get() {
        return client;
    }
}
