package org.kie.baaas.mcp.api.webhook;

import java.util.ArrayList;
import java.util.List;

import org.kie.baaas.mcp.api.ResponseList;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonInclude(JsonInclude.Include.NON_NULL)
@RegisterForReflection
public class WebhookResponseList extends ResponseList {

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
