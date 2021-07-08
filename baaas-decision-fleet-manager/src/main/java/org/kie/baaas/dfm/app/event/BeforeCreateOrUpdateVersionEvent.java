package org.kie.baaas.dfm.app.event;

import org.kie.baaas.dfm.api.decisions.DecisionRequest;
import org.kie.baaas.dfm.app.listener.EventWithId;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class BeforeCreateOrUpdateVersionEvent extends EventWithId {

    @JsonProperty("decisionRequest")
    private DecisionRequest decisionRequest;

    public DecisionRequest getDecisionRequest() {
        return decisionRequest;
    }

    public void setDecisionRequest(DecisionRequest decisionRequest) {
        this.decisionRequest = decisionRequest;
    }

    public BeforeCreateOrUpdateVersionEvent(DecisionRequest decisionRequest) {
        this.decisionRequest = decisionRequest;
    }

    public BeforeCreateOrUpdateVersionEvent() {
        // Jackson.
    }

    @Override
    public String toString() {
        return "BeforeCreateOrUpdateVersionEvent [decisionRequest=" + decisionRequest + "]";
    }
}
