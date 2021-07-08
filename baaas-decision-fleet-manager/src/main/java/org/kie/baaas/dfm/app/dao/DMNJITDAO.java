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

package org.kie.baaas.dfm.app.dao;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.kie.baaas.dfm.api.DMNJIT;
import org.kie.baaas.dfm.app.exceptions.DecisionFleetManagerException;
import org.kie.baaas.dfm.app.model.DecisionFleetShard;
import org.kie.baaas.dfm.app.model.ListResult;

import static java.util.stream.Collectors.toList;

@ApplicationScoped
public class DMNJITDAO {

    private final DecisionFleetShardDAO decisionFleetShardDAO;

    @Inject
    public DMNJITDAO(DecisionFleetShardDAO decisionFleetShardDAO) {
        Objects.requireNonNull(decisionFleetShardDAO, "fleetShardDAO cannot be null.");
        this.decisionFleetShardDAO = decisionFleetShardDAO;
    }

    @Transactional
    public ListResult<DMNJIT> listAll(int page, int size) {
        ListResult<DecisionFleetShard> fleetShards = decisionFleetShardDAO.listAll(page, size);
        List<DMNJIT> jits = fleetShards.getItems().stream().map(this::createJitForShard).collect(toList());
        return new ListResult<>(jits, fleetShards.getPage(), fleetShards.getTotal());
    }

    private DMNJIT createJitForShard(DecisionFleetShard fleetShard) {
        try {
            URL dmnJitUrl = new URL(fleetShard.getDmnJitUrl());
            DMNJIT dmnjit = new DMNJIT();
            dmnjit.setUrl(dmnJitUrl);
            return dmnjit;
        } catch (MalformedURLException e) {
            throw new DecisionFleetManagerException("The DMN JIT URL '" + fleetShard.getDmnJitUrl() + "' is malformed.", e);
        }
    }
}