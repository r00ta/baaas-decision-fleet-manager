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

package org.kie.baaas.mcp.app.dao;

import java.net.URL;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.kie.baaas.mcp.app.model.ListResult;
import org.kie.baaas.mcp.app.model.webhook.Webhook;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.kie.baaas.mcp.app.TestConstants.DEFAULT_CUSTOMER_ID;

@QuarkusTest
public class WebhookDAOTest {

    @Inject
    WebhookDAO webhookDAO;

    @Test
    @TestTransaction
    public void listAll_withPagination() {
        Webhook w = create("https://example.com/callback1");
        Webhook w2 = create("https://example.com/callback2");

        webhookDAO.persist(w, w2);

        ListResult<Webhook> webhookListResult = webhookDAO.findByCustomer(DEFAULT_CUSTOMER_ID, 0, 100);
        assertThat(webhookListResult.getPage(), equalTo(0L));
        assertThat(webhookListResult.getSize(), equalTo(3L));
        assertThat(webhookListResult.getTotal(), equalTo(3L));
    }

    @Test
    @TestTransaction
    public void listAll_withPaginationSizeOffset() {
        Webhook w = create("https://example.com/callback1");
        Webhook w2 = create("https://example.com/callback2");

        webhookDAO.persist(w, w2);

        ListResult<Webhook> webhookListResult = webhookDAO.findByCustomer(DEFAULT_CUSTOMER_ID, 0, 2);
        assertThat(webhookListResult.getPage(), equalTo(0L));
        assertThat(webhookListResult.getSize(), equalTo(2L));
        assertThat(webhookListResult.getTotal(), equalTo(3L));

        assertThat(webhookListResult.getItems().get(0).getUrl().toExternalForm(), equalTo("http://localhost:8080/test-builtin-webhook"));
        assertThat(webhookListResult.getItems().get(1).getUrl().toExternalForm(), equalTo("https://example.com/callback1"));
    }

    @Test
    @TestTransaction
    public void listAll_withPaginationPageOffset() {
        Webhook w = create("https://example.com/callback1");
        Webhook w2 = create("https://example.com/callback2");

        webhookDAO.persist(w, w2);

        ListResult<Webhook> webhookListResult = webhookDAO.findByCustomer(DEFAULT_CUSTOMER_ID, 1, 2);
        assertThat(webhookListResult.getPage(), equalTo(1L));
        assertThat(webhookListResult.getSize(), equalTo(1L));
        assertThat(webhookListResult.getTotal(), equalTo(3L));
        assertThat(webhookListResult.getItems().get(0).getUrl().toExternalForm(), equalTo("https://example.com/callback2"));
    }

    private Webhook create(String url) {
        Webhook w = new Webhook();
        try {
            w.setUrl(new URL(url));
            w.setCustomerId(DEFAULT_CUSTOMER_ID);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Webhook with URL '" + url + "'", e);
        }
        return w;
    }
}
