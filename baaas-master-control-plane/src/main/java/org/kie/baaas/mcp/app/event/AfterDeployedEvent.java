package org.kie.baaas.mcp.app.event;

import org.kie.baaas.mcp.api.decisions.DecisionResponse;
import org.kie.baaas.mcp.app.listener.EventWithId;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class AfterDeployedEvent extends EventWithId {

    @JsonProperty("decisionResponse")
    private DecisionResponse decisionResponse;

    public DecisionResponse getDecisionResponse() {
        return decisionResponse;
    }

    public void setDecisionResponse(DecisionResponse decisionResponse) {
        this.decisionResponse = decisionResponse;
    }

    public AfterDeployedEvent(DecisionResponse decisionResponse) {
        this.decisionResponse = decisionResponse;
    }

    public AfterDeployedEvent() {
        // Jackson.
    }

    @Override
    public String toString() {
        return "AfterDeployedEvent [decisionResponse=" + decisionResponse + "]";
    }
}
