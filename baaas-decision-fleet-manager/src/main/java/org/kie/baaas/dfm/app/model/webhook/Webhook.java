package org.kie.baaas.dfm.app.model.webhook;

import java.net.URL;
import java.util.UUID;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@NamedQueries({
        @NamedQuery(name = "Webhook.byCustomerId", query = "from Webhook w where w.customerId=:customerId"),
        @NamedQuery(name = "Webhook.byCustomerIdAndWebhookId", query = "from Webhook w where w.customerId=:customerId and w.id=:id"),
        @NamedQuery(name = "Webhook.byCustomerIdAndUrl", query = "from Webhook w where w.customerId=:customerId and w.url=:url")
})
@Entity
@Table(name = "WEBHOOK")
public class Webhook {

    public static final String URL_PARAM = "url";

    @Id
    private String id = UUID.randomUUID().toString();

    @Basic
    @Column(name = "customer_id", nullable = false, updatable = false)
    private String customerId;

    @Basic
    @Column(nullable = false, updatable = false)
    private URL url;

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public URL getUrl() {
        return url;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public Webhook() {
    }

    public Webhook(String customerId, URL url) {
        this.customerId = customerId;
        this.url = url;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((customerId == null) ? 0 : customerId.hashCode());
        result = prime * result + ((url == null) ? 0 : url.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Webhook other = (Webhook) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (url == null) {
            if (other.url != null)
                return false;
        } else if (!url.equals(other.url))
            return false;
        if (customerId == null) {
            return other.customerId == null;
        } else {
            return customerId.equals(other.customerId);
        }
    }

    @Override
    public String toString() {
        return "Webhook [id=" + id + ", customerId=" + customerId + ", url=" + url + "]";
    }
}
