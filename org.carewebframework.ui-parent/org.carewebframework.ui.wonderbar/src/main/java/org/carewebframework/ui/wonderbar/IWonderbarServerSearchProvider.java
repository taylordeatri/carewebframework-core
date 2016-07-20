/*
 * #%L
 * carewebframework
 * %%
 * Copyright (C) 2008 - 2016 Regenstrief Institute, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * This Source Code Form is also subject to the terms of the Health-Related
 * Additional Disclaimer of Warranty and Limitation of Liability available at
 *
 *      http://www.carewebframework.org/licensing/disclaimer.
 *
 * #L%
 */
package org.carewebframework.ui.wonderbar;

import java.util.List;

/**
 * Interface for server-based search providers.
 * 
 * @param <T> Type returned by search provider.
 */
public interface IWonderbarServerSearchProvider<T> extends IWonderbarSearchProvider<T> {
    
    /**
     * Return the list of items that match the given search string. maxItems is given as
     * informational and can be used for performance reasons. The returned list will be truncated to
     * maxItems regardless of the # of items returned.
     * 
     * @param search The search term.
     * @param maxItems The max # of hits that will be returned in the search.
     * @param hits List to receive matched items.
     * @return True if all matching items were returned, or false if maxItems was exceeded.
     */
    boolean getSearchResults(String search, int maxItems, List<T> hits);
}
