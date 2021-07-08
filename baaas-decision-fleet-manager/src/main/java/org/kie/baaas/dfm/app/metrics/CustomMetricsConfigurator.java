/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.baaas.dfm.app.metrics;

import java.time.Duration;
import java.util.Arrays;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;

@Singleton
public class CustomMetricsConfigurator {

    @ConfigProperty(name = "baaas.metrics.deployment.name", defaultValue = "baaas-dfm")
    String deploymentName;

    /**
     * Set the Metrics custom tag with baaas app name
     */
    @Produces
    @Singleton
    public MeterFilter configureAllRegistries() {
        return MeterFilter.commonTags(Arrays.asList(
                Tag.of("baaas-deployment-name", deploymentName)));
    }

    /**
     * Enable histogram buckets for http_server_requests_seconds bucket
     * Every new uri will be added on the metrics endpoint as soon it gets called
     */
    @Produces
    @Singleton
    public MeterFilter enableHistogram() {
        return new MeterFilter() {

            @Override
            public MeterFilterReply accept(Meter.Id id) {
                if (id.getTags().contains(Tag.of("uri", "/q"))) {
                    return MeterFilterReply.DENY;
                }
                return MeterFilterReply.ACCEPT;
            }

            @Override
            public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {

                if (id.getName().startsWith("http.server.requests")) {

                    return DistributionStatisticConfig.builder()
                            .percentiles(1, 0.95)
                            .percentilesHistogram(true) // histogram buckets
                            // Requested values on https://issues.redhat.com/browse/BAAAS-91
                            .serviceLevelObjectives(
                                    Duration.ofMillis(1).toNanos(), // 0.001 s
                                    Duration.ofMillis(10).toNanos(), // 0.01 s
                                    Duration.ofMillis(100).toNanos(), // 0.1 s
                                    Duration.ofMillis(500).toNanos(), // 0.5 s
                                    Duration.ofMillis(1000).toNanos(), // 1.0 s
                                    Duration.ofMillis(5000).toNanos()) // 5.0 s
                            .minimumExpectedValue(1000000.0) // 0.001 s
                            .maximumExpectedValue(6.0E9)

                            .build()
                            .merge(config);
                }
                return config;
            }
        };
    }
}
