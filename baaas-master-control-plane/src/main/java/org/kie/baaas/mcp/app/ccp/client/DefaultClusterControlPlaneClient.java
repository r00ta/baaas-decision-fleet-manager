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

import java.net.URI;
import java.util.Collection;

import org.kie.baaas.dfs.api.DecisionRequest;
import org.kie.baaas.dfs.api.DecisionRequestSpec;
import org.kie.baaas.dfs.api.DecisionVersionSpec;
import org.kie.baaas.dfs.api.Kafka;
import org.kie.baaas.mcp.app.ccp.ClusterControlPlaneClient;
import org.kie.baaas.mcp.app.config.MasterControlPlaneConfig;
import org.kie.baaas.mcp.app.exceptions.MasterControlPlaneException;
import org.kie.baaas.mcp.app.model.ClusterControlPlane;
import org.kie.baaas.mcp.app.model.Decision;
import org.kie.baaas.mcp.app.model.DecisionVersion;
import org.kie.baaas.mcp.app.model.deployment.Deployment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;

import static java.util.Collections.singleton;

public class DefaultClusterControlPlaneClient implements ClusterControlPlaneClient {

    private static final String CALLBACK_URL_SUFFIX = "/callback/decisions/%s/versions/%s";

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultClusterControlPlaneClient.class);

    private KubernetesClient kubernetesClient;

    private ClusterControlPlane clusterControlPlane;

    private MasterControlPlaneConfig config;

    public DefaultClusterControlPlaneClient(MasterControlPlaneConfig config, KubernetesClient kubernetesClient, ClusterControlPlane clusterControlPlane) {
        this.kubernetesClient = kubernetesClient;
        this.clusterControlPlane = clusterControlPlane;
        this.config = config;
    }

    @Override
    public ClusterControlPlane getClusterControlPlane() {
        return clusterControlPlane;
    }

    private String getNamespace() {

        Namespace namespace = kubernetesClient.namespaces().withName(clusterControlPlane.getNamespace()).get();
        if (namespace == null) {
            throw new MasterControlPlaneException("Cannot locate namespace '" + clusterControlPlane.getNamespace() + "' for Cluster Control Plane.");
        }

        return namespace.getMetadata().getName();
    }

    @Override
    public void deploy(DecisionVersion decisionVersion) {
        String namespace = getNamespace();
        DecisionRequest decisionRequest = from(decisionVersion);
        LOGGER.info("Requesting deployment of Decision with name '{}' at version '{}' in namespace '{}'...", decisionRequest.getMetadata().getName(), decisionVersion.getVersion(), namespace);
        kubernetesClient.customResources(DecisionRequest.class).inNamespace(namespace).createOrReplace(decisionRequest);
    }

    private String getDecisionRequestName(DecisionVersion decisionVersion) {
        return decisionVersion.getDecision().getCustomerId() + "-" + decisionVersion.getDecision().getId();
    }

    private DecisionRequest from(DecisionVersion decisionVersion) {

        ObjectMeta objectMeta = new ObjectMeta();
        objectMeta.setName(getDecisionRequestName(decisionVersion));

        DecisionVersionSpec decisionVersionSpec = new DecisionVersionSpec();
        decisionVersionSpec.setVersion(String.valueOf(decisionVersion.getVersion()));
        decisionVersionSpec.setSource(URI.create(decisionVersion.getDmnLocation()));

        if (decisionVersion.getKafkaTopics() != null) {
            Kafka kafka = new Kafka();
            kafka.setInputTopic(decisionVersion.getKafkaTopics().getSourceTopic());
            kafka.setOutputTopic(decisionVersion.getKafkaTopics().getSinkTopic());
            // The kafka cluster in-use is hard-coded for the demo. The user can only vary the topics that they want to use.
            kafka.setBootstrapServers(config.getKafkaBootstrapServers());
            kafka.setSecretName(config.getKafkaSecretName());
            decisionVersionSpec.setKafka(kafka);
        }

        DecisionRequestSpec decisionRequestSpec = new DecisionRequestSpec();
        decisionRequestSpec.setDefinition(decisionVersionSpec);
        decisionRequestSpec.setName(KubernetesResourceUtil.sanitizeName(decisionVersion.getDecision().getName()).toLowerCase());
        decisionRequestSpec.setCustomerId(decisionVersion.getDecision().getCustomerId());
        decisionRequestSpec.setWebhooks(createCallbackUrl(decisionVersion));

        DecisionRequest decisionRequest = new DecisionRequest();
        decisionRequest.setMetadata(objectMeta);
        decisionRequest.setSpec(decisionRequestSpec);
        return decisionRequest;
    }

    private Collection<URI> createCallbackUrl(DecisionVersion decisionVersion) {
        String path = String.format(CALLBACK_URL_SUFFIX, decisionVersion.getDecision().getId(), decisionVersion.getVersion());
        String callbackPath = config.getApiBaseUrl() + path;
        return singleton(URI.create(callbackPath));
    }

    private Resource<org.kie.baaas.dfs.api.DecisionVersion> getDecisionVersion(Deployment deployment) {

        if (deployment != null) {
            Namespace namespace = getDeploymentNamespace(deployment);
            if (namespace != null) {
                return kubernetesClient.customResources(org.kie.baaas.dfs.api.DecisionVersion.class).inNamespace(namespace.getMetadata().getName()).withName(deployment.getVersionName());
            }
        }

        return null;
    }

    private Namespace getDeploymentNamespace(Deployment deployment) {
        if (deployment != null && deployment.getNamespace() != null) {
            return kubernetesClient.namespaces().withName(deployment.getNamespace()).get();
        }

        return null;
    }

    private Resource<org.kie.baaas.dfs.api.Decision> getDecision(Deployment deployment) {
        if (deployment != null) {
            Namespace namespace = getDeploymentNamespace(deployment);
            if (namespace != null) {
                return kubernetesClient.customResources(org.kie.baaas.dfs.api.Decision.class).inNamespace(namespace.getMetadata().getName()).withName(deployment.getName());
            }
        }

        return null;
    }

    @Override
    public void delete(DecisionVersion decisionVersion) {

        Deployment deployment = decisionVersion.getDeployment();
        org.kie.baaas.dfs.api.DecisionVersion deployedVersion = null;
        Resource<org.kie.baaas.dfs.api.DecisionVersion> resource = getDecisionVersion(deployment);

        if (resource != null) {
            deployedVersion = resource.get();
        }

        if (deployedVersion != null) {
            LOGGER.info("Deleting DecisionVersion '{}' from namespace '{}'", deployment.getVersionName(), deployment.getNamespace());
            resource.delete();
        } else {
            LOGGER.warn("DecisionVersion '{}' does not exist in namespace '{}'. Nothing to delete.", deployment.getVersionName(), deployment.getNamespace());
        }
    }

    @Override
    public void delete(Decision decision) {
        Deployment deployment = decision.getCurrentVersion().getDeployment();
        org.kie.baaas.dfs.api.Decision deployedDecision = null;
        Resource<org.kie.baaas.dfs.api.Decision> resource = getDecision(deployment);
        if (resource != null) {
            deployedDecision = resource.get();
        }

        if (deployedDecision != null) {
            LOGGER.info("Deleting Decision with name '{}' from namespace '{}'...", deployment.getName(), deployment.getNamespace());
            resource.delete();
        }
    }
}
