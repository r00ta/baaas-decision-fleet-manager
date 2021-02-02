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

package org.kie.baaas.mcp.app.model.eventing;

import java.util.Objects;

import javax.persistence.Embeddable;

/**
 * Encapsulates the Kafka Topics that a user can attach to a Decision
 */
@Embeddable
public class KafkaTopics {

    private String sourceTopic;

    private String sinkTopic;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        KafkaTopics that = (KafkaTopics) o;
        return Objects.equals(sourceTopic, that.sourceTopic) && Objects.equals(sinkTopic, that.sinkTopic);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceTopic, sinkTopic);
    }
}
