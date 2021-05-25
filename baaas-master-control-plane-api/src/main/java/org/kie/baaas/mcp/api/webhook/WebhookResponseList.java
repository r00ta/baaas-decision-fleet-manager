package org.kie.baaas.mcp.api.webhook;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class WebhookResponseList {

    @JsonProperty("kind")
    private String kind = "WebhookList";

    @JsonProperty("items")
    private List<WebhookResponse> items = new ArrayList<>();

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public List<WebhookResponse> getItems() {
        return items;
    }

    public void setItems(List<WebhookResponse> items) {
        this.items = items;
    }
}
