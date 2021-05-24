package org.kie.baaas.mcp.app.manager;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.kie.baaas.mcp.app.listener.ListenerManager;
import org.kie.baaas.mcp.app.webhook.WebhookListener;

import io.quarkus.test.junit.QuarkusTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class WebhookManagerTest {

    @Inject
    WebhookManager webhookManager;

    @Inject
    ListenerManager listenerManager;

    @Test
    public void testWebhookMgrInit() {
        assertTrue(webhookManager.listAll().size() > 0);
        assertTrue(listenerManager.hasListeners());
        assertEquals(1, listenerManager.getListeners().size());
        WebhookListener listener0 = (WebhookListener) listenerManager.getListeners().stream().findFirst().get();
        assertEquals("http://localhost:8080/test-builtin-webhook", listener0.getWebhook().getUrl().toString());
    }
}
