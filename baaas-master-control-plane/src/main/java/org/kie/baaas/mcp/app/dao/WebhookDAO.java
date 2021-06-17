package org.kie.baaas.mcp.app.dao;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;

import org.kie.baaas.mcp.app.model.ListResult;
import org.kie.baaas.mcp.app.model.webhook.Webhook;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;

import static io.quarkus.panache.common.Sort.ascending;

/**
 * DAO implementation for working with Webhook Entities.
 */
@ApplicationScoped
@Transactional
public class WebhookDAO implements PanacheRepository<Webhook> {

    public ListResult<Webhook> listAll(int page, int size) {
        PanacheQuery<Webhook> pagedQuery = findAll(ascending(Webhook.URL_PARAM)).page(Page.of(page, size));
        List<Webhook> webhooks = pagedQuery.list();
        long count = pagedQuery.count();
        return new ListResult<>(webhooks, page, count);
    }
}
