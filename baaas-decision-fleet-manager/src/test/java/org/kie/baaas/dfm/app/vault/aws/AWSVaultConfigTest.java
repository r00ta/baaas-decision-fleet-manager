package org.kie.baaas.dfm.app.vault.aws;

import java.util.Optional;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@QuarkusTest
class AWSVaultConfigTest {

    @Inject
    AWSVaultConfig awsVaultConfig;

    @Test
    void testConfig() {
        assertThat(awsVaultConfig.getEndpoint().get(), is("http://notused:9999"));
        assertThat(awsVaultConfig.getRegion(), is("us-east-1"));
        assertThat(awsVaultConfig.getAccessKeyId(), is("test-key"));
        assertThat(awsVaultConfig.getSecretAccessKey(), is("test-secret"));
    }

    @Test
    void testToString() {
        assertThat(awsVaultConfig.toString(),
                is("AWSVaultConfig{region='us-east-1', endpoint=Optional[http://notused:9999], accessKeyId='******ey', secretAccessKey='*********et'}"));

        AWSVaultConfig config = new AWSVaultConfig("some region", Optional.empty(), null, null);
        assertThat(config.toString(),
                is("AWSVaultConfig{region='some region', endpoint=Optional.empty, accessKeyId='empty', secretAccessKey='empty'}"));
    }

}
