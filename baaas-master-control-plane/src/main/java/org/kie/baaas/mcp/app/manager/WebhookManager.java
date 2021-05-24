package org.kie.baaas.mcp.app.manager;

import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.kie.baaas.mcp.api.webhook.WebhookRegistrationRequest;
import org.kie.baaas.mcp.app.dao.WebhookDAO;
import org.kie.baaas.mcp.app.listener.ListenerManager;
import org.kie.baaas.mcp.app.model.webhook.Webhook;
import org.kie.baaas.mcp.app.webhook.AlreadyExistingWebhookException;
import org.kie.baaas.mcp.app.webhook.NotFoundWebhookException;
import org.kie.baaas.mcp.app.webhook.WebhookListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.Startup;

@Startup
@ApplicationScoped
@Transactional
public class WebhookManager {
    private static final Logger LOG = LoggerFactory.getLogger(WebhookManager.class);

    private final WebhookDAO webhookDAO;
    private final ListenerManager listenerManager;
    private final ManagedExecutor executorService;

    @Inject
    public WebhookManager(WebhookDAO webhookDAO, ListenerManager listenerManager, ManagedExecutor executorService) {
        Objects.requireNonNull(webhookDAO, "webhookDAO cannot be null");
        Objects.requireNonNull(listenerManager, "listenerManager cannot be null");
        Objects.requireNonNull(executorService, "executorService cannot be null");

        this.webhookDAO = webhookDAO;
        this.listenerManager = listenerManager;
        this.executorService = executorService;
    }

    @PostConstruct
    public void init() {
        List<Webhook> listAll = listAll();
        LOG.info("init() with {}", listAll);
        for (Webhook e : listAll) {
            listenerManager.addListener(new WebhookListener(e, executorService));
        }
    }

    public List<Webhook> listAll() {
        return webhookDAO.listAll();
    }

    public Webhook registerWebhook(WebhookRegistrationRequest webhookReq) {
        LOG.debug("registerWebhook {}", webhookReq);
        List<Webhook> alreadyExisting = webhookDAO.list("url", webhookReq.getUrl());
        if (alreadyExisting.size() > 0) {
            throw new AlreadyExistingWebhookException("The webhook is already existing: " + alreadyExisting);
        }
        Webhook webhook = new Webhook();
        webhook.setUrl(webhookReq.getUrl());
        listenerManager.addListener(new WebhookListener(webhook, executorService));
        webhookDAO.persist(webhook);
        LOG.info("Persisted new Webhook with id '{}' for URL '{}'", webhook.getId(), webhook.getUrl());
        return webhook;
    }

    /**
     * support the same semantics as on the /decisions resources where a GET or DELETE works with the <id> parameter set to either the URL or id of the webhook
     */
    public void unregisterForWebhook(String lookupRef) {
        LOG.info("unregisterForWebhook {}", lookupRef);
        boolean anyDeleted = false;
        List<Webhook> listById = webhookDAO.list("id", lookupRef);
        for (Webhook e : listById) {
            webhookDAO.delete(e);
            anyDeleted = true;
            LOG.info("Deleted Webhook with id '{}' for URL '{}'", e.getId(), e.getUrl());
        }
        try {
            URL urlRef = new URL(lookupRef);
            List<Webhook> findByUrlOrId = webhookDAO.list("url", urlRef);
            for (Webhook e : findByUrlOrId) {
                webhookDAO.delete(e);
                anyDeleted = true;
                LOG.info("Deleted Webhook with id '{}' for URL '{}'", e.getId(), e.getUrl());
            }
        } catch (Exception e) {
            LOG.info("lookupRef '{}' is not a valid URL, none deleted by URL", lookupRef);
        }
        if (!anyDeleted) {
            throw new NotFoundWebhookException("No webhook found to be deleted for lookupRef: " + lookupRef);
        }
        final Predicate<WebhookListener> lookup = (WebhookListener l) -> l.getWebhook().getUrl().toString().equals(lookupRef) || l.getWebhook().getId().equals(lookupRef);
        List<WebhookListener> toBeRemoved = listenerManager.getListeners()
                .stream()
                .filter(WebhookListener.class::isInstance)
                .map(WebhookListener.class::cast)
                .filter(lookup::test)
                .collect(Collectors.toList());
        toBeRemoved.forEach(listenerManager::removeListener);
    }
}
