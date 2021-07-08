package org.kie.baaas.dfm.app.manager;

import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.kie.baaas.dfm.api.webhook.WebhookRegistrationRequest;
import org.kie.baaas.dfm.app.dao.WebhookDAO;
import org.kie.baaas.dfm.app.listener.ListenerManager;
import org.kie.baaas.dfm.app.model.ListResult;
import org.kie.baaas.dfm.app.model.webhook.Webhook;
import org.kie.baaas.dfm.app.webhook.AlreadyExistingWebhookException;
import org.kie.baaas.dfm.app.webhook.NotFoundWebhookException;
import org.kie.baaas.dfm.app.webhook.WebhookListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.runtime.Startup;

@Startup
@ApplicationScoped
@Transactional
public class WebhookManager {
    private static final Logger LOG = LoggerFactory.getLogger(WebhookManager.class);

    private final WebhookDAO webhookDAO;
    private final ListenerManager listenerManager;
    private final ManagedExecutor executorService;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;

    @ConfigProperty(name = "baaas.webhook.delivery-retries", defaultValue = "3")
    Integer MAX_RETRY;
    @ConfigProperty(name = "baaas.webhook.delivery-timeout", defaultValue = "10")
    Integer TIMEOUT;

    @Inject
    public WebhookManager(WebhookDAO webhookDAO, ListenerManager listenerManager, ManagedExecutor executorService, MeterRegistry meterRegistry, ObjectMapper objectMapper) {
        Objects.requireNonNull(webhookDAO, "webhookDAO cannot be null");
        Objects.requireNonNull(listenerManager, "listenerManager cannot be null");
        Objects.requireNonNull(executorService, "executorService cannot be null");
        Objects.requireNonNull(meterRegistry, "meterRegistry cannot be null");
        Objects.requireNonNull(objectMapper, "objectMapper cannot be null");

        this.webhookDAO = webhookDAO;
        this.listenerManager = listenerManager;
        this.executorService = executorService;
        this.meterRegistry = meterRegistry;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        List<Webhook> listAll = listAll();
        LOG.info("init() with {}", listAll);
        for (Webhook e : listAll) {
            listenerManager.addListener(new WebhookListener(e, executorService, meterRegistry, objectMapper, MAX_RETRY, TIMEOUT));
        }
    }

    public List<Webhook> listAll() {
        return webhookDAO.listAll();
    }

    public ListResult<Webhook> listCustomerWebhooks(String customerId, int page, int size) {
        return webhookDAO.findByCustomer(customerId, page, size);
    }

    public Webhook registerWebhook(String customerId, WebhookRegistrationRequest webhookReq) {
        LOG.debug("registerWebhook {}", webhookReq);
        List<Webhook> alreadyExisting = webhookDAO.findByCustomerIdAndUrl(customerId, webhookReq.getUrl());
        if (alreadyExisting.size() > 0) {
            throw new AlreadyExistingWebhookException("The webhook is already existing: " + alreadyExisting);
        }
        Webhook webhook = new Webhook(customerId, webhookReq.getUrl());
        listenerManager.addListener(new WebhookListener(webhook, executorService, meterRegistry, objectMapper, MAX_RETRY, TIMEOUT));
        webhookDAO.persist(webhook);
        LOG.info("Persisted new Webhook with id '{}' for URL '{}'", webhook.getId(), webhook.getUrl());
        return webhook;
    }

    /**
     * support the same semantics as on the /decisions resources where a GET or DELETE works with the <id> parameter set to either the URL or id of the webhook
     */
    public void unregisterForWebhook(String customerId, String lookupRef) {
        LOG.info("unregisterForWebhook {}", lookupRef);
        boolean anyDeleted = false;

        List<Webhook> listById = webhookDAO.findByCustomerIdAndWebhookId(customerId, lookupRef);
        for (Webhook e : listById) {
            webhookDAO.delete(e);
            anyDeleted = true;
            LOG.info("Deleted Webhook with id '{}' for URL '{}'", e.getId(), e.getUrl());
        }
        try {
            URL urlRef = new URL(lookupRef);
            List<Webhook> findByUrlOrId = webhookDAO.findByCustomerIdAndUrl(customerId, urlRef);
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
