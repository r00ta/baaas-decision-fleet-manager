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

package org.kie.baaas.mcp.app.model;

import java.util.Objects;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Models an instance of a registered Cluster Control Plane that the Master Control Plane
 * can use for deployment of Decisions.
 */
@Entity
@Table(name = "CLUSTER_CONTROL_PLANE")
public class ClusterControlPlane {

    @Id
    private int id;

    @Basic
    @Column(nullable = false, name = "kubernetes_api_url")
    private String kubernetesApiUrl;

    @Basic
    @Column(nullable = false, name = "namespace")
    private String namespace;

    @Basic
    @Column(name = "dmn_jit_url", nullable = false)
    private String dmnJitUrl;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getKubernetesApiUrl() {
        return kubernetesApiUrl;
    }

    public void setKubernetesApiUrl(String kubernetesApi) {
        this.kubernetesApiUrl = kubernetesApi;
    }

    public String getDmnJitUrl() {
        return dmnJitUrl;
    }

    public void setDmnJitUrl(String dmnJITUrl) {
        this.dmnJitUrl = dmnJITUrl;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ClusterControlPlane that = (ClusterControlPlane) o;
        return id == that.id && kubernetesApiUrl.equals(that.kubernetesApiUrl) && dmnJitUrl.equals(that.dmnJitUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, kubernetesApiUrl, dmnJitUrl);
    }
}
