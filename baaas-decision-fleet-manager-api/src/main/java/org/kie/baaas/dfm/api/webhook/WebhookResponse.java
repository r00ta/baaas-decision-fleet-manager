package org.kie.baaas.dfm.api.webhook;

import java.net.URL;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class WebhookResponse {

    @JsonProperty("kind")
    private final String kind = "WebHook";

    @JsonProperty("id")
    private String id;

    @JsonProperty("href")
    private String href;

    @JsonProperty("url")
    private URL url;

    public String getKind() {
        return kind;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public static WebhookResponse from(String id, URL url) {
        WebhookResponse result = new WebhookResponse();
        result.setId(id);
        result.setUrl(url);
        result.setHref(String.format("/webhooks/%s", id));
        return result;
    }
}
