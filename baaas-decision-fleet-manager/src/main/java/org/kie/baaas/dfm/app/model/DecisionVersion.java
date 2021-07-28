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

package org.kie.baaas.dfm.app.model;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Basic;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Version;

import org.kie.baaas.dfm.app.model.deployment.Deployment;
import org.kie.baaas.dfm.app.model.eventing.KafkaConfig;

/**
 * Encapsulates a version of a Decision.
 */
@NamedQueries({
        @NamedQuery(name = "DecisionVersion.countByCustomerAndName", query = "from DecisionVersion dv where dv.decision.customerId=:customerId and dv.decision.name=:name"),
        @NamedQuery(name = "DecisionVersion.countCurrentByCustomer", query = "select count(d.currentVersion.id) from Decision d where d.customerId=:customerId"),
        @NamedQuery(name = "DecisionVersion.listCurrentIdsByCustomer", query = "select d.currentVersion.id from Decision d where d.customerId=:customerId order by d.name"),
        @NamedQuery(name = "DecisionVersion.countByIdOrName",
                query = "select count(dv.id) from DecisionVersion dv where dv.decision.customerId=:customerId and (dv.decision.name=:idOrName or dv.decision.id=:idOrName)"),
        @NamedQuery(name = "DecisionVersion.listIdsByIdOrName",
                query = "select dv.id from DecisionVersion dv where dv.decision.customerId=:customerId and (dv.decision.name=:idOrName or dv.decision.id=:idOrName)"),
        @NamedQuery(name = "DecisionVersion.listByIdOrName",
                query = "select dv from DecisionVersion dv left join fetch dv.tags left join fetch dv.configuration join fetch dv.decision where dv.id in (:ids) order by dv.submittedAt desc"),
        @NamedQuery(name = "DecisionVersion.listCurrentByCustomer",
                query = "select dv from DecisionVersion dv left join fetch dv.tags left join fetch dv.configuration join fetch dv.decision where dv.id in (:ids) order by dv.decision.name"),
        @NamedQuery(name = "DecisionVersion.currentByCustomerAndDecisionIdOrName",
                query = "select dv from DecisionVersion dv left join fetch dv.tags left join fetch dv.configuration join fetch dv.decision where dv.id=dv.decision.currentVersion.id and dv.decision.customerId=:customerId and (dv.decision.id=:idOrName or dv.decision.name=:idOrName)"),
        @NamedQuery(name = "DecisionVersion.buildingByCustomerAndDecisionIdOrName",
                query = "select dv from DecisionVersion dv left join fetch dv.tags left join fetch dv.configuration join fetch dv.decision where dv.id=dv.decision.nextVersion.id and dv.decision.customerId=:customerId and (dv.decision.id=:idOrName or dv.decision.name=:idOrName)"),
        @NamedQuery(name = "DecisionVersion.byCustomerDecisionIdOrNameAndVersion",
                query = "select dv from DecisionVersion dv left join fetch dv.tags left join fetch dv.configuration join fetch dv.decision where dv.version=:version and dv.decision.customerId=:customerId and (dv.decision.id=:idOrName or dv.decision.name=:idOrName)"),
        @NamedQuery(name = "DecisionVersion.listAll",
                query = "select dv from DecisionVersion dv left join fetch dv.tags left join fetch dv.configuration join fetch dv.decision order by dv.decision.name")
})
@Entity
@Table(name = "DECISION_VERSION")
public class DecisionVersion {

    @Id
    private String id = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY)
    private Decision decision;

    @Basic
    @Column(updatable = false, nullable = false, name = "dmn_location")
    private String dmnLocation;

    @Basic
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DecisionVersionStatus status;

    @Basic
    @Column(nullable = false)
    private String description;

    @Column(updatable = false, nullable = false)
    private long version;

    @Column(name = "submitted_at", updatable = false, nullable = false, columnDefinition = "TIMESTAMP")
    private ZonedDateTime submittedAt;

    @Column(name = "published_at", columnDefinition = "TIMESTAMP")
    private ZonedDateTime publishedAt;

    @Version
    @Column(name = "lock_version", nullable = false)
    private int lockVersion = 0;

    @Basic
    @Column(name = "dmn_md5", updatable = false)
    private String dmnMd5;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "DECISION_VERSION_TAG",
            joinColumns = @JoinColumn(name = "decision_version_id"))
    @MapKeyColumn(name = "name")
    @Column(name = "value", updatable = false, nullable = false)
    private Map<String, String> tags = new HashMap<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "DECISION_VERSION_CONFIG",
            joinColumns = @JoinColumn(name = "decision_version_id"))
    @MapKeyColumn(name = "name")
    @Column(name = "value", nullable = false, updatable = false)
    private Map<String, String> configuration = new HashMap<>();

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "sourceTopic", column = @Column(name = "kafka_source_topic", updatable = false)),
            @AttributeOverride(name = "sinkTopic", column = @Column(name = "kafka_sink_topic", updatable = false)),
            @AttributeOverride(name = "bootstrapServers", column = @Column(name = "kafka_bootstrap_servers", updatable = false))
    })
    private KafkaConfig kafkaConfig;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "namespace", column = @Column(name = "dfs_namespace")),
            @AttributeOverride(name = "name", column = @Column(name = "dfs_name")),
            @AttributeOverride(name = "versionName", column = @Column(name = "dfs_version_name")),
            @AttributeOverride(name = "versionUrl", column = @Column(name = "version_url")),
            @AttributeOverride(name = "currentUrl", column = @Column(name = "current_url")),
            @AttributeOverride(name = "statusMessage", column = @Column(name = "status_message"))
    })
    private Deployment deployment;

    public Deployment getDeployment() {
        return deployment;
    }

    public void setDeployment(Deployment deployment) {
        this.deployment = deployment;
    }

    public KafkaConfig getKafkaConfig() {
        return kafkaConfig;
    }

    public void setKafkaConfig(KafkaConfig kafkaConfig) {
        this.kafkaConfig = kafkaConfig;
    }

    public String getId() {
        return id;
    }

    public Decision getDecision() {
        return decision;
    }

    public String getDmnLocation() {
        return dmnLocation;
    }

    public DecisionVersionStatus getStatus() {
        return status;
    }

    public long getVersion() {
        return version;
    }

    public ZonedDateTime getSubmittedAt() {
        return submittedAt;
    }

    public ZonedDateTime getPublishedAt() {
        return publishedAt;
    }

    public String getDmnMd5() {
        return dmnMd5;
    }

    public void setDecision(Decision decision) {
        this.decision = decision;
    }

    public void setDmnLocation(String dmnLocation) {
        this.dmnLocation = dmnLocation;
    }

    public void setStatus(DecisionVersionStatus status) {
        this.status = status;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public void setSubmittedAt(ZonedDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public void setPublishedAt(ZonedDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public void setDmnMd5(String dmnMd5) {
        this.dmnMd5 = dmnMd5;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    public Map<String, String> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, String> configuration) {
        this.configuration = configuration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DecisionVersion that = (DecisionVersion) o;
        return version == that.version && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, version);
    }
}
