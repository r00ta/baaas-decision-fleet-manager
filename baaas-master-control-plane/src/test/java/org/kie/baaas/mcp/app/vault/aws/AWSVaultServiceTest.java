package org.kie.baaas.mcp.app.vault.aws;

import java.util.Map;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.kie.baaas.mcp.app.vault.Secret;
import org.kie.baaas.mcp.app.vault.VaultException;
import org.kie.baaas.mcp.app.vault.VaultService;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretResponse;
import software.amazon.awssdk.services.secretsmanager.model.DeleteSecretResponse;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.kie.baaas.mcp.app.managedservices.ManagedServicesClient.CLIENT_ID;
import static org.kie.baaas.mcp.app.managedservices.ManagedServicesClient.CLIENT_SECRET;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class AWSVaultServiceTest {

    @Inject
    VaultService vault;

    @InjectMock
    SecretsManagerClient client;

    @Test
    void testCreate() {
        var secret = new Secret()
                .setId("foo")
                .setValues(Map.of(CLIENT_ID, "svc-acc-001", CLIENT_SECRET, "super-secret-pwd"));

        CreateSecretResponse.Builder builder = CreateSecretResponse.builder().name(secret.getId());
        builder.sdkHttpResponse(SdkHttpResponse.builder().statusCode(200).build());
        when(client.createSecret(any(Consumer.class))).thenReturn(builder.build());

        vault.create(secret);

        verify(client, times(1)).createSecret(any(Consumer.class));
    }

    @Test
    void testCreateWithException() {
        var secret = new Secret().setId("foo").setValues(Map.of(CLIENT_ID, "svc-acc-001", CLIENT_SECRET, "super-secret-pwd"));
        when(client.createSecret(any(Consumer.class))).thenThrow(SecretsManagerException.builder().message("some error").build());

        assertThrows(VaultException.class, () -> vault.create(secret), "Unable to create secret foo in vault");
    }

    @Test
    void testGetNotFound() {
        when(client.getSecretValue(any(Consumer.class)))
                .thenThrow(ResourceNotFoundException.builder().message("not found").build());
        assertThat(vault.get("foo"), nullValue());
    }

    @Test
    void testGetWithException() {
        when(client.getSecretValue(any(Consumer.class)))
                .thenThrow(SecretsManagerException.builder().message("some error").build());
        assertThrows(VaultException.class, () -> vault.get("foo"), "Unable to get secret foo from vault");
    }

    @Test
    void testGet() {
        var secretValue = "{\"clientId\": \"svc-acc-001\", \"clientSecret\": \"super-secret-pwd\"}";
        GetSecretValueResponse response = GetSecretValueResponse.builder().name("foo").secretString(secretValue).build();
        when(client.getSecretValue(any(Consumer.class))).thenReturn(response);

        Secret secret = vault.get("foo");

        assertThat(secret, notNullValue());
        assertThat(secret.getId(), is("foo"));
        assertThat(secret.getValues().get(CLIENT_ID), is("svc-acc-001"));
        assertThat(secret.getValues().get(CLIENT_SECRET), is("super-secret-pwd"));
    }

    @Test
    void testDelete() {
        var name = "foo";
        var response = DeleteSecretResponse.builder().name(name).build();
        when(client.deleteSecret(any(Consumer.class))).thenReturn(response);

        String result = vault.delete(name);

        assertThat(result, notNullValue());
        assertThat(result, is(name));
    }

    @Test
    void testDeleteNotFound() {
        when(client.deleteSecret(any(Consumer.class)))
                .thenThrow(ResourceNotFoundException.builder().message("not found").build());
        assertThat(vault.delete("foo"), nullValue());
    }

    @Test
    void testDeleteWithException() {
        when(client.deleteSecret(any(Consumer.class)))
                .thenThrow(SecretsManagerException.builder().message("some error").build());
        assertThrows(VaultException.class, () -> vault.delete("foo"), "Unable to delete secret foo from vault");
    }
}
