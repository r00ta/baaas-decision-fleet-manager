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

package org.kie.baaas.mcp.app.model;

import java.util.List;

/**
 * Encapsulates the result of calling a DAO method than returns a list. It not only includes
 * the results of the query, but also details on the total number of items, the page and number of items
 * on the page.
 * 
 * @param <T> - The type of the entity returned in the list.
 */
public class ListResult<T> {

    private final List<T> items;

    private final long total;

    private final long page;

    public ListResult(List<T> items, long page, long total) {
        this.items = items;
        this.page = page;
        this.total = total;
    }

    public long getPage() {
        return page;
    }

    public long getSize() {
        return this.items == null ? 0 : items.size();
    }

    public long getTotal() {
        return total;
    }

    public List<T> getItems() {
        return items;
    }
}
