package org.kie.baaas.mcp.api.webhook;

import java.net.URL;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class WebhookRegistrationRequest {

    @JsonProperty("url")
    private URL url;

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }
}
