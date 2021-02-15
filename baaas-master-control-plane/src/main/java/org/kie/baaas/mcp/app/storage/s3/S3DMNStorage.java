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

package org.kie.baaas.mcp.app.storage.s3;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.kie.baaas.mcp.api.decisions.DecisionRequest;
import org.kie.baaas.mcp.app.config.MasterControlPlaneConfig;
import org.kie.baaas.mcp.app.manager.DecisionManager;
import org.kie.baaas.mcp.app.model.Decision;
import org.kie.baaas.mcp.app.model.DecisionVersion;
import org.kie.baaas.mcp.app.storage.DMNStorageRequest;
import org.kie.baaas.mcp.app.storage.DecisionDMNStorage;
import org.kie.baaas.mcp.app.storage.DecisionDMNStorageException;
import org.kie.baaas.mcp.app.storage.hash.DMNHashGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import static java.util.Objects.requireNonNull;

@ApplicationScoped
public class S3DMNStorage implements DecisionDMNStorage {

    private final Logger LOGGER = LoggerFactory.getLogger(S3DMNStorage.class);

    // base url = https://s3Endpoint/s3BucketName
    private final String S3_DMN_ENDPOINT = "s3://%s";
    // base file location customers/<customer_id>/<decision_id>/<decision_version>/dmn.xml
    private final String DMN_LOCATION = "customers/%s/%s/%d/dmn.xml";

    private final MasterControlPlaneConfig config;
    private final S3Client s3Client;
    private final DMNHashGenerator hashGenerator;
    private final DecisionManager decisionManager;

    @Inject
    public S3DMNStorage(MasterControlPlaneConfig config, DMNHashGenerator hashGenerator, S3Client s3Client,
                        DecisionManager decisionManager) {

        requireNonNull(config, "config cannot be null");
        requireNonNull(s3Client, "s3Client cannot be null");
        requireNonNull(hashGenerator, "hashGenerator cannot be null");
        requireNonNull(decisionManager, "decisionManager cannot be null");

        this.config = config;
        this.s3Client = s3Client;
        this.hashGenerator = hashGenerator;
        this.decisionManager = decisionManager;
    }

    @Override
    public DMNStorageRequest writeDMN(String customerId, DecisionRequest decisionRequest, DecisionVersion decisionVersion) {

        String dmnLocation = composeDMNLocation(customerId, decisionVersion.getDecision().getId(), decisionVersion.getVersion());

        PutObjectResponse response = s3Client.putObject(
                putObjectRequest(dmnLocation, hashGenerator.generateHash(decisionRequest.getModel().getDmn())),
                RequestBody.fromBytes(decisionRequest.getModel().getDmn().getBytes(StandardCharsets.UTF_8)));

        String dmnURL = composeS3URL(dmnLocation);
        LOGGER.info("DMN file {} successfully written at {}.", decisionRequest.getName(), dmnURL);
        return new DMNStorageRequest(dmnURL, response.eTag());
    }

    @Override
    public void deleteDMN(String customerId, Decision decision) {
        // get objects from bucket
        ListObjectsV2Response objectsInBucket = s3Client
                .listObjectsV2(ListObjectsV2Request
                                       .builder()
                                       .bucket(config.getBucketName())
                                       .build());

        if (objectsInBucket.contents().isEmpty()) {
            throw new DecisionDMNStorageException(
                    String.format("There is no object on bucket %s that matches customer id %s and decision name %s",
                                  config.getBucketName(),
                                  customerId,
                                  decision.getId()));
        }

        objectsInBucket.contents().stream()
                .filter(obj -> obj.key().contains(customerId) && obj.key().contains(decision.getId()))
                .forEach(obj -> {
                    s3Client.deleteObject(deleteObjectRequest(obj.key()));
                    LOGGER.info("Object {} deleted from bucket {}.", obj.key(), config.getBucketName());
                });
    }

    @Override
    public ByteArrayOutputStream readDMN(String customerId, DecisionVersion decisionVersion) {

        String dmnLocation = composeDMNLocation(customerId, decisionVersion.getDecision().getId(), decisionVersion.getVersion());

        try {
            ByteArrayOutputStream dmnBaos = new ByteArrayOutputStream();
            s3Client.getObject(getObjectRequest(dmnLocation), ResponseTransformer.toOutputStream(dmnBaos));
            return dmnBaos;
        } catch (final Exception e) {
            throw new DecisionDMNStorageException("Failed to read decision from S3 Bucket.", e);
        }
    }

    /**
     * Return the full s3 url for the given dmn
     *
     * @param dmnLocation - The full dmn location
     * @return url to access the dmn.
     */
    private String composeS3URL(String dmnLocation) {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format(S3_DMN_ENDPOINT, config.getBucketName()));
        builder.append("/");
        builder.append(dmnLocation);
        return builder.toString();
    }

    /**
     * Return the dmnLocation on S3 Bucket based on given inputs
     *
     * @param customerId      - The customer id associated with the decision
     * @param decisionId      - The decision name
     * @param decisionVersion - The decision version
     * @return full location of the dmn file.
     */
    private String composeDMNLocation(String customerId, String decisionId, long decisionVersion) {
        return String.format(DMN_LOCATION, customerId, decisionId, decisionVersion);
    }

    /**
     * Builds a put object request for the given dmn which will be persisted on S3 Bucket.
     *
     * @param decisionName - The decision name to be persisted
     * @param md5Checksum  - The decision chedksum (MD5)
     * @return PutObjectRequest with dmn file information.
     */
    private PutObjectRequest putObjectRequest(String decisionName, String md5Checksum) {
        return PutObjectRequest.builder()
                .bucket(config.getBucketName())
                .key(decisionName)
                .contentMD5(md5Checksum)
                .contentType("application/xml")
                .build();
    }

    /**
     * Builds a get object request for the given dmn.
     *
     * @param dmnLocation - The decision location on bucket
     * @return GetObjectRequest with dmn file information.
     */
    private GetObjectRequest getObjectRequest(String dmnLocation) {
        return GetObjectRequest
                .builder()
                .bucket(config.getBucketName())
                .key(dmnLocation)
                .build();
    }

    /**
     * Builds a delete object request for the given dmn.
     *
     * @param objectKey - The decision chedksum (MD5)
     * @return DeleteObjectRequest with dmn file information.
     */
    private DeleteObjectRequest deleteObjectRequest(String objectKey) {
        return DeleteObjectRequest
                .builder()
                .bucket(config.getBucketName())
                .key(objectKey)
                .build();
    }
}
