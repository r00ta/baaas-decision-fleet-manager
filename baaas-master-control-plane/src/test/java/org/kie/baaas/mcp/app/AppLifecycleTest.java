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

package org.kie.baaas.mcp.app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.baaas.mcp.app.dao.DecisionFleetShardDAO;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.quarkus.runtime.StartupEvent;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AppLifecycleTest {

    @Mock
    private DecisionFleetShardDAO decisionFleetShardDAO;

    @InjectMocks
    private AppLifecycle appLifecycle;

    @Test
    public void onStart() {
        appLifecycle.onStart(new StartupEvent());
        verify(decisionFleetShardDAO).init();
    }
}
