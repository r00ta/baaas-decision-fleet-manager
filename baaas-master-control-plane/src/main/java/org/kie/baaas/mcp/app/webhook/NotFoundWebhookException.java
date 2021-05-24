package org.kie.baaas.mcp.app.webhook;

import javax.ws.rs.core.Response;

import org.kie.baaas.mcp.app.exceptions.MasterControlPlaneException;

/**
 * Models a not-found Webhook case, for instance when trying to delete a Webhook via lookupRef
 */
public class NotFoundWebhookException extends MasterControlPlaneException {

    public NotFoundWebhookException(String message) {
        super(message);
    }

    public NotFoundWebhookException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public int getStatusCode() {
        return Response.Status.NOT_FOUND.getStatusCode();
    }

}
