package org.kie.baaas.mcp.app.webhook;

import javax.ws.rs.core.Response;

import org.kie.baaas.mcp.app.exceptions.MasterControlPlaneException;

/**
 * Models the case when a Webhook is already existings and as such it should not be created as a duplicate
 */
public class AlreadyExistingWebhookException extends MasterControlPlaneException {

    public AlreadyExistingWebhookException(String message) {
        super(message);
    }

    public AlreadyExistingWebhookException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public int getStatusCode() {
        return Response.Status.BAD_REQUEST.getStatusCode();
    }

}
