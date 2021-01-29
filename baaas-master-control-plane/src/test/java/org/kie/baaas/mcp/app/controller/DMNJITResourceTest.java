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

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.baaas.mcp.api.DMNJIT;
import org.kie.baaas.mcp.api.DMNJITList;
import org.kie.baaas.mcp.app.dao.DMNJITDAO;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DMNJITResourceTest {

    @Mock
    private DMNJITDAO dmnjitdao;

    @Mock
    private DMNJIT dmnjit;

    @InjectMocks
    private DMNJITResource dmnjitResource;

    @Test
    public void listDmnJits() {

        when(dmnjitdao.findOne()).thenReturn(dmnjit);

        Response response = dmnjitResource.listDmnJits();
        assertThat(response, is(notNullValue()));
        assertThat(response.getStatus(), equalTo(Response.Status.OK.getStatusCode()));

        DMNJITList dmnjitList = response.readEntity(DMNJITList.class);
        assertThat(dmnjitList.getItems(), contains(dmnjit));
    }
}
