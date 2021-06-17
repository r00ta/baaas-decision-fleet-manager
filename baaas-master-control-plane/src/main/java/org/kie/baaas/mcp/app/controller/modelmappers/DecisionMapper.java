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

package org.kie.baaas.mcp.app.controller.modelmappers;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.kie.baaas.mcp.api.decisions.DecisionResponse;
import org.kie.baaas.mcp.api.decisions.DecisionResponseList;
import org.kie.baaas.mcp.api.decisions.ResponseModel;
import org.kie.baaas.mcp.api.eventing.Eventing;
import org.kie.baaas.mcp.api.eventing.kafka.Kafka;
import org.kie.baaas.mcp.app.model.Decision;
import org.kie.baaas.mcp.app.model.DecisionVersion;
import org.kie.baaas.mcp.app.model.DecisionVersionStatus;
import org.kie.baaas.mcp.app.model.ListResult;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * Maps between our internal domain model and the DTOs supported by the REST API for BAaaS.
 */
@ApplicationScoped
public class DecisionMapper {

    private final HrefGenerator hrefGenerator;

    @Inject
    public DecisionMapper(HrefGenerator hrefGenerator) {
        requireNonNull(hrefGenerator, "hrefGenerator cannot be null");
        this.hrefGenerator = hrefGenerator;
    }

    public DecisionResponse mapVersionToDecisionResponse(DecisionVersion decisionVersion) {

        Decision decision = decisionVersion.getDecision();

        DecisionResponse decisionResponse = new DecisionResponse();
        decisionResponse.setDescription(decision.getDescription());
        decisionResponse.setId(decision.getId());
        decisionResponse.setName(decision.getName());
        decisionResponse.setVersion(decisionVersion.getVersion());
        decisionResponse.setSubmittedAt(decisionVersion.getSubmittedAt());
        decisionResponse.setPublishedAt(decisionVersion.getPublishedAt());
        if (decisionVersion.getDeployment() != null) {
            if (DecisionVersionStatus.CURRENT.equals(decisionVersion.getStatus())) {
                decisionResponse.setCurrentEndpoint(decisionVersion.getDeployment().getCurrentUrl());
            }
            decisionResponse.setUrl(decisionVersion.getDeployment().getVersionUrl()); // to be removed once the modeler is updated to use versionEndpoint and currentEndpoint
            decisionResponse.setVersionEndpoint(decisionVersion.getDeployment().getVersionUrl());
            decisionResponse.setStatusMessage(decisionVersion.getDeployment().getStatusMessage());
        }
        decisionResponse.setStatus(decisionVersion.getStatus().name());
        decisionResponse.setConfiguration(decisionVersion.getConfiguration());
        decisionResponse.setTags(decisionVersion.getTags());
        decisionResponse.setHref(hrefGenerator.generateDecisionHref(decisionVersion));

        ResponseModel responseModel = new ResponseModel();
        responseModel.setMd5(decisionVersion.getDmnMd5());
        responseModel.setHref(hrefGenerator.generateDecisionDMNHref(decisionVersion));
        decisionResponse.setResponseModel(responseModel);

        if (decisionVersion.getKafkaTopics() != null) {

            Kafka kafka = new Kafka();
            kafka.setSink(decisionVersion.getKafkaTopics().getSinkTopic());
            kafka.setSource(decisionVersion.getKafkaTopics().getSourceTopic());

            Eventing eventing = new Eventing();
            eventing.setKafka(kafka);
            decisionResponse.setEventing(eventing);
        }

        return decisionResponse;
    }

    public DecisionResponse mapToDecisionResponse(Decision decision) {
        return mapVersionToDecisionResponse(decision.getCurrentVersion());
    }

    public DecisionResponseList mapVersionsToDecisionResponseList(ListResult<DecisionVersion> decisionVersions) {

        DecisionResponseList responseList = new DecisionResponseList();
        List<DecisionResponse> items = decisionVersions.getItems().stream()
                .map(this::mapVersionToDecisionResponse)
                .collect(toList());
        responseList.setItems(items);
        setPaginationData(decisionVersions, responseList);
        return sort(responseList);
    }

    private DecisionResponseList sort(DecisionResponseList responseList) {
        Comparator<DecisionResponse> comparator = Comparator.comparing(DecisionResponse::getSubmittedAt);
        Collections.sort(responseList.getItems(), comparator.reversed());
        return responseList;
    }

    private void setPaginationData(ListResult<?> listResult, DecisionResponseList responseList) {
        responseList.setPage(listResult.getPage());
        responseList.setSize(listResult.getSize());
        responseList.setTotal(listResult.getTotal());
    }

    public DecisionResponseList mapToDecisionResponseList(ListResult<Decision> decisions) {

        DecisionResponseList responseList = new DecisionResponseList();
        List<DecisionResponse> items = decisions.getItems().stream()
                .map(this::mapToDecisionResponse)
                .collect(toList());
        responseList.setItems(items);
        setPaginationData(decisions, responseList);
        return sort(responseList);
    }
}
