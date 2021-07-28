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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Version;

@NamedQueries({
        @NamedQuery(name = "Decision.byCustomerIdAndName", query = "from Decision where customerId=:customerId and name=:name"),
        @NamedQuery(name = "Decision.byCustomerAndIdOrName", query = "from Decision where customerId=:customerId and (name=:idOrName or id=:idOrName)"),
        @NamedQuery(name = "Decision.decisionCountByCustomerId", query = "select count(d.id) from Decision d where d.customerId=:customerId")
})
@Entity
@Table(name = "DECISION")
public class Decision {

    @Id
    private String id = UUID.randomUUID().toString();

    @Basic
    @Column(name = "customer_id", nullable = false, updatable = false)
    private String customerId;

    @Basic
    @Column(nullable = false, updatable = false)
    private String name;

    @JoinColumn(name = "current_version_id")
    @OneToOne(optional = false, fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private DecisionVersion currentVersion;

    @JoinColumn(name = "next_version_id")
    @OneToOne(optional = true, fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private DecisionVersion nextVersion;

    @OrderBy("submittedAt ASC")
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "decision", orphanRemoval = true, cascade = CascadeType.ALL)
    private List<DecisionVersion> versions = new ArrayList<>();

    @Version
    @Column(name = "lock_version", nullable = false)
    private int lockVersion = 0;

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getName() {
        return name;
    }

    public DecisionVersion getCurrentVersion() {
        return currentVersion;
    }

    public DecisionVersion getNextVersion() {
        return nextVersion;
    }

    public List<DecisionVersion> getVersions() {
        return versions;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCurrentVersion(DecisionVersion currentVersion) {
        this.currentVersion = currentVersion;
    }

    public void setNextVersion(DecisionVersion nextVersion) {
        this.nextVersion = nextVersion;
    }

    public void setVersions(List<DecisionVersion> versions) {
        this.versions = versions;
    }

    public void addVersion(DecisionVersion decisionVersion) {
        if (decisionVersion != null) {
            versions.add(decisionVersion);
            decisionVersion.setDecision(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Decision decision = (Decision) o;
        return id.equals(decision.id) && customerId.equals(decision.customerId) && name.equals(decision.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, customerId, name);
    }
}
