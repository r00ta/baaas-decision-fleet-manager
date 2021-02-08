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

package org.kie.baaas.mcp.app.storage.hash;

import java.nio.charset.StandardCharsets;

import javax.enterprise.context.ApplicationScoped;

import software.amazon.awssdk.utils.Md5Utils;

@ApplicationScoped
public class DMNHashGenerator {

    /**
     * MD5 checksum sum calculator, based on aws md5 utils
     * @param plainTextDmn
     * @return md5 checksum as string
     */
    public String generateHash(String plainTextDmn) {
        return Md5Utils.md5AsBase64(plainTextDmn.getBytes(StandardCharsets.UTF_8));
    }
}
