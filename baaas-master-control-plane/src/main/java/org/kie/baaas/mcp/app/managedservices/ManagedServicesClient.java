/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.kie.baaas.mcp.app.managedservices;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.kie.baaas.mcp.app.vault.Secret;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openshift.cloud.api.kas.DefaultApi;
import com.openshift.cloud.api.kas.invoker.ApiException;
import com.openshift.cloud.api.kas.invoker.auth.HttpBearerAuth;
import com.openshift.cloud.api.kas.models.ServiceAccount;
import com.openshift.cloud.api.kas.models.ServiceAccountListItem;
import com.openshift.cloud.api.kas.models.ServiceAccountRequest;

@ApplicationScoped
public class ManagedServicesClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagedServicesClient.class);

    public static final String CLIENT_ID = "clientId";
    public static final String CLIENT_SECRET = "clientSecret";

    @Inject
    JsonWebToken token;

    @Inject
    ManagedServicesConfig config;

    public Secret createOrReplaceServiceAccount(String saName) {
        var defaultApi = getDefaultApi();
        try {
            Optional<ServiceAccountListItem> expected = defaultApi.listServiceAccounts()
                    .getItems()
                    .stream()
                    .filter(s -> s.getName().equals(saName))
                    .findFirst();
            ServiceAccount sa;
            if (expected.isPresent()) {
                LOGGER.debug("ServiceAccount {} exists, need to reset the credentials", saName);
                sa = defaultApi.resetServiceAccountCreds(expected.get().getId());
            } else {
                ServiceAccountRequest req = new ServiceAccountRequest();
                req.name(saName);
                req.description("DaaS Managed Service Account " + saName);
                LOGGER.debug("ServiceAccount {} does not exist, need to create it.", saName);
                sa = defaultApi.createServiceAccount(req);
            }
            return new Secret().setId(sa.getName())
                    .value(CLIENT_ID, sa.getClientID())
                    .value(CLIENT_SECRET, sa.getClientSecret());
        } catch (ApiException e) {
            LOGGER.error("Unable to createOrReplace ServiceAccount {}", saName, e);
            throw new ManagedServicesException("Unable to createOrReplace Service Account: " + saName, e);
        }
    }

    private DefaultApi getDefaultApi() {
        HttpBearerAuth bearer = (HttpBearerAuth) config.getClient().getAuthentication("Bearer");
        bearer.setBearerToken(token.getRawToken());
        return new DefaultApi(config.getClient());
    }

}
