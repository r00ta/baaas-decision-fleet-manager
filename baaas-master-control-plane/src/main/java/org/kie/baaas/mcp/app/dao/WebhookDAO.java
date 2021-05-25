package org.kie.baaas.mcp.app.dao;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;

import org.kie.baaas.mcp.app.model.webhook.Webhook;

import io.quarkus.hibernate.orm.panache.PanacheRepository;

/**
 * DAO implementation for working with Webhook Entities.
 */
@ApplicationScoped
@Transactional
public class WebhookDAO implements PanacheRepository<Webhook> {

    // nothing more atm.

}
