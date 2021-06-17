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

package org.kie.baaas.mcp.app.controller;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.kie.baaas.mcp.api.DMNJIT;
import org.kie.baaas.mcp.api.DMNJITList;
import org.kie.baaas.mcp.app.config.MasterControlPlaneConfig;

import io.quarkus.test.junit.QuarkusTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
public class DMNJITResourceTest {

    @Inject
    MasterControlPlaneConfig config;

    @Inject
    DMNJITResource dmnjitResource;

    @Test
    public void listDmnJits() {

        Response response = dmnjitResource.listDmnJits(0, 100);
        assertThat(response, is(notNullValue()));
        assertThat(response.getStatus(), equalTo(Response.Status.OK.getStatusCode()));

        DMNJITList dmnjitList = response.readEntity(DMNJITList.class);
        assertThat(dmnjitList.getPage(), equalTo(0L));
        assertThat(dmnjitList.getSize(), equalTo(1L));
        assertThat(dmnjitList.getTotal(), equalTo(1L));

        DMNJIT dmnjit = dmnjitList.getItems().get(0);
        assertThat(dmnjit.getUrl().toExternalForm(), equalTo(config.getCcpDmnJitUrl()));
    }
}
