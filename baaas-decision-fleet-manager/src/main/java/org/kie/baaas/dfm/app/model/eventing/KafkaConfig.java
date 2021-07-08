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

package org.kie.baaas.dfm.app.model.eventing;

import java.util.Objects;

import javax.persistence.Embeddable;
import javax.persistence.Transient;

/**
 * Encapsulates the Kafka Topics that a user can attach to a Decision
 */
@Embeddable
public class KafkaConfig {

    private String sourceTopic;

    private String sinkTopic;

    private String bootstrapServers;

    @Transient
    private Credential credential;

    public String getSourceTopic() {
        return sourceTopic;
    }

    public void setSourceTopic(String inputTopic) {
        this.sourceTopic = inputTopic;
    }

    public String getSinkTopic() {
        return sinkTopic;
    }

    public void setSinkTopic(String outputTopic) {
        this.sinkTopic = outputTopic;
    }

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public Credential getCredential() {
        return credential;
    }

    public void setCredential(Credential credential) {
        this.credential = credential;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof KafkaConfig))
            return false;
        KafkaConfig that = (KafkaConfig) o;
        return Objects.equals(sourceTopic, that.sourceTopic) &&
                Objects.equals(sinkTopic, that.sinkTopic) &&
                Objects.equals(bootstrapServers, that.bootstrapServers) &&
                Objects.equals(credential, that.credential);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceTopic, sinkTopic, bootstrapServers, credential);
    }
}
