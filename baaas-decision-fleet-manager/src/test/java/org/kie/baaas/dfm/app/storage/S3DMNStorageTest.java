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

package org.kie.baaas.dfm.app.storage;

import java.io.ByteArrayOutputStream;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.baaas.dfm.api.decisions.DecisionRequest;
import org.kie.baaas.dfm.api.decisions.Model;
import org.kie.baaas.dfm.app.config.DecisionFleetManagerConfig;
import org.kie.baaas.dfm.app.manager.DecisionManager;
import org.kie.baaas.dfm.app.model.Decision;
import org.kie.baaas.dfm.app.model.DecisionVersion;
import org.kie.baaas.dfm.app.storage.hash.DMNHashGenerator;
import org.kie.baaas.dfm.app.storage.s3.S3DMNStorage;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class S3DMNStorageTest {

    @InjectMocks
    S3DMNStorage s3DMNStorage;

    @Mock
    S3Client s3Client;

    @Mock
    DMNHashGenerator hashGenerator;

    @Mock
    DMNStorageRequest dmnStorageRequest;

    @Mock
    DecisionFleetManagerConfig decisionFleetManagerConfig;

    @Mock
    DecisionManager decisionManager;

    private DecisionRequest createApiRequest() {

        Model model = new Model();
        model.setDmn("<xml test=\"123\">foo</xml>");

        DecisionRequest request = new DecisionRequest();
        request.setDescription("The Best Decision Ever");
        request.setName("robs-first-decision");
        request.setModel(model);
        return request;
    }

    @Test
    public void writeDMNTest() {

        String bucketName = "baaas-storage-dev";
        when(decisionFleetManagerConfig.getBucketName()).thenReturn(bucketName);

        Decision decision = new Decision();
        DecisionVersion decisionVersion = new DecisionVersion();
        decisionVersion.setVersion(1L);
        decisionVersion.setDecision(decision);

        PutObjectResponse response = mock(PutObjectResponse.class);
        PutObjectRequest request = PutObjectRequest.builder()
                .key("customers/customer-id-1/" + decision.getId() + "/1/dmn.xml")
                .contentType("application/xml")
                .bucket(bucketName)
                .build();

        when(response.eTag()).thenReturn("ff576fa78715ffc6f9fa6d32c3bc9b9a");
        when(s3Client.putObject(Mockito.eq(request), any(RequestBody.class)))
                .thenReturn(response);

        dmnStorageRequest = s3DMNStorage.writeDMN("customer-id-1", createApiRequest(), decisionVersion);

        assertThat(dmnStorageRequest.getProviderUrl(), equalTo("s3://" + bucketName + "/customers/customer-id-1/" + decision.getId() + "/1/dmn.xml"));
        assertThat(dmnStorageRequest.getMd5Hash(), equalTo("ff576fa78715ffc6f9fa6d32c3bc9b9a"));
    }

    @Test
    public void deleteDMNTest() {
        List<S3Object> objLIst = new ArrayList<>();
        objLIst.add(S3Object.builder()
                .key("obj1").lastModified(ZonedDateTime.now(ZoneOffset.MAX).toInstant())
                .eTag("obj1-chacksum")
                .storageClass("STANDARD")
                .build());

        objLIst.add(S3Object.builder()
                .key("obj2").lastModified(ZonedDateTime.now(ZoneOffset.MAX).toInstant())
                .eTag("obj2-chacksum")
                .storageClass("STANDARD")
                .build());

        ListObjectsV2Response listObjectsV2Response = ListObjectsV2Response.builder().contents(objLIst).build();

        lenient().when(s3Client.listObjectsV2(ListObjectsV2Request.builder().build())).thenReturn(listObjectsV2Response);

        // TODO finish to mock the delete request.

    }

    @Test
    public void readDMNTest() {
        String id = "1234";
        long version = 1l;
        Decision decision = mock(Decision.class);
        when(decision.getId()).thenReturn(id);
        DecisionVersion decisionVersion = mock(DecisionVersion.class);
        when(decisionVersion.getVersion()).thenReturn(version);
        when(decisionVersion.getDecision()).thenReturn(decision);

        ArgumentCaptor<GetObjectRequest> cap = ArgumentCaptor.forClass(GetObjectRequest.class);
        when(s3Client.getObject(cap.capture(), any(ResponseTransformer.class))).thenReturn(null);

        ByteArrayOutputStream outputStream = s3DMNStorage.readDMN("1", decisionVersion);
        assertThat(outputStream, is(notNullValue()));

        GetObjectRequest getObjectRequest = cap.getValue();
        assertThat(getObjectRequest.key(), equalTo("customers/1/" + id + "/" + version + "/dmn.xml"));
    }
}
