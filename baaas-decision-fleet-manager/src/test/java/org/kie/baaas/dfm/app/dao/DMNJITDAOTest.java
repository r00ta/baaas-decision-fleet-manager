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

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.kie.baaas.dfm.api.DMNJIT;
import org.kie.baaas.dfm.app.config.DecisionFleetManagerConfig;
import org.kie.baaas.dfm.app.model.ListResult;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
public class DMNJITDAOTest {

    @Inject
    DMNJITDAO dmnjitdao;

    @Inject
    DecisionFleetManagerConfig config;

    @TestTransaction
    @Test
    public void listAll() {
        ListResult<DMNJIT> listResult = dmnjitdao.listAll(0, 100);
        assertThat(listResult.getSize(), equalTo(1L));
        assertThat(listResult.getTotal(), equalTo(1L));
        assertThat(listResult.getPage(), equalTo(0L));

        DMNJIT dmnjit = listResult.getItems().get(0);
        assertThat(dmnjit.getUrl().toExternalForm(), equalTo(config.getDfsDmnJitUrl()));
    }
}
