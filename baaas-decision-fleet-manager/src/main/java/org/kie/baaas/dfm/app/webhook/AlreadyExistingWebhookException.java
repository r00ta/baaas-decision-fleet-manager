package org.kie.baaas.dfm.app.webhook;

import javax.ws.rs.core.Response;

import org.kie.baaas.dfm.app.exceptions.DecisionFleetManagerException;

/**
 * Models the case when a Webhook is already existings and as such it should not be created as a duplicate
 */
public class AlreadyExistingWebhookException extends DecisionFleetManagerException {

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
