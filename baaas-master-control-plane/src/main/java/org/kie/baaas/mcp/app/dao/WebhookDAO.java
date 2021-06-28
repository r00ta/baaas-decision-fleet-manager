package org.kie.baaas.mcp.app.dao;

import java.net.URL;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;

import org.kie.baaas.mcp.app.model.ListResult;
import org.kie.baaas.mcp.app.model.webhook.Webhook;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;

/**
 * DAO implementation for working with Webhook Entities.
 */
@ApplicationScoped
@Transactional
public class WebhookDAO implements PanacheRepository<Webhook> {

    public ListResult<Webhook> findByCustomer(String customerId, int page, int size) {
        Parameters params = Parameters.with("customerId", customerId);
        long count = find("#Webhook.byCustomerId", params).count();
        List<Webhook> webhooks = find("#Webhook.byCustomerId", params).page(page, size).list();
        return new ListResult<Webhook>(webhooks, page, count);
    }

    public List<Webhook> findByCustomerIdAndWebhookId(String customerId, String webhookId) {
        Parameters params = Parameters.with("customerId", customerId).and("id", webhookId);
        return find("#Webhook.byCustomerIdAndWebhookId", params).list();
    }

    public List<Webhook> findByCustomerIdAndUrl(String customerId, URL url) {
        Parameters params = Parameters.with("customerId", customerId).and("url", url);
        return find("#Webhook.byCustomerIdAndUrl", params).list();
    }
}
