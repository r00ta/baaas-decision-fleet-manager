package org.kie.baaas.dfm.app.webhook;

import javax.ws.rs.core.Response;

import org.kie.baaas.dfm.app.exceptions.DecisionFleetManagerException;

/**
 * Models a not-found Webhook case, for instance when trying to delete a Webhook via lookupRef
 */
public class NotFoundWebhookException extends DecisionFleetManagerException {

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
