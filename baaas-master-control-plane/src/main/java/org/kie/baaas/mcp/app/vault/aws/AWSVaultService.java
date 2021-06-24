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

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.kie.baaas.mcp.app.vault.Secret;
import org.kie.baaas.mcp.app.vault.VaultException;
import org.kie.baaas.mcp.app.vault.VaultService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.Json;

import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretResponse;
import software.amazon.awssdk.services.secretsmanager.model.DeleteSecretResponse;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;

@ApplicationScoped
public class AWSVaultService implements VaultService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AWSVaultService.class);

    @Inject
    SecretsManagerClient client;

    @Override
    public void create(Secret secret) {
        try {
            CreateSecretResponse response = client.createSecret(b -> b
                    .name(secret.getId())
                    .secretString(Json.encode(secret.getValues())));
            if (response.sdkHttpResponse().isSuccessful()) {
                LOGGER.debug("Secret {} created in AWS Vault", secret.getId());
            } else {
                String message = "Unable to create secret " + secret.getId() + " in AWS Vault. " + response.sdkHttpResponse().statusText();
                LOGGER.error(message);
                throw new VaultException(message);
            }
        } catch (SecretsManagerException e) {
            LOGGER.error("Unable to create secret {} in AWS Vault", secret.getId(), e);
            throw new VaultException("Unable to create secret " + secret.getId() + " in vault", e);
        }
    }

    @Override
    public Secret get(String name) {
        try {
            GetSecretValueResponse resp = client.getSecretValue(b -> b.secretId(name));
            LOGGER.debug("Secret {} found in AWS Vault", name);
            return new Secret().setId(name).setValues(Json.decodeValue(resp.secretString(), Map.class));
        } catch (ResourceNotFoundException e) {
            LOGGER.info("Secret {} not found in AWS Vault", name);
            return null;
        } catch (SecretsManagerException e) {
            LOGGER.error("Unable to get secret {} from AWS Vault", name, e);
            throw new VaultException("Unable to get secret " + name + " from vault", e);
        }
    }

    @Override
    public String delete(String name) {
        try {
            DeleteSecretResponse resp = client.deleteSecret(b -> b.secretId(name));
            LOGGER.debug("Deleted secret {} from AWS Vault", name);
            return resp.name();
        } catch (ResourceNotFoundException e) {
            LOGGER.info("Secret {} not found in AWS Vault", name);
            return null;
        } catch (SecretsManagerException e) {
            LOGGER.error("Unable to delete secret {} from AWS Vault", name, e);
            throw new VaultException("Unable to delete secret " + name + " from vault", e);
        }
    }

}
