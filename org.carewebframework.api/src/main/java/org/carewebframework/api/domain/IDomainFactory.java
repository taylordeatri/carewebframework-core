/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 *
 * This Source Code Form is also subject to the terms of the Health-Related Additional
 * Disclaimer of Warranty and Limitation of Liability available at
 * http://www.carewebframework.org/licensing/disclaimer.
 */
package org.carewebframework.api.domain;

import java.util.List;

/**
 * Interface for a domain object factory.
 */
public interface IDomainFactory {
    
    /**
     * Creates a new instance of an object of this domain.
     * 
     * @param clazz Class of object to create.
     * @return The new domain object instance.
     */
    <T extends IDomainObject> T newObject(Class<T> clazz);
    
    /**
     * Fetches an object, identified by its unique id, from the underlying data store.
     *
     * @param clazz Class of object to create.
     * @param id Unique id of the object.
     * @return The requested object.
     */
    <T extends IDomainObject> T fetchObject(Class<T> clazz, String id);
    
    /**
     * Fetches an object, identified by its unique id, from the underlying data store.
     *
     * @param clazz Class of object to create.
     * @param key Unique key associated with the object.
     * @param table Table containing the requested object.
     * @return The requested object.
     */
    <T extends IDomainObject> T fetchObject(Class<T> clazz, String key, String table);
    
    /**
     * Fetches multiple domain objects as specified by an array of identifier values.
     *
     * @param clazz Class of object to create.
     * @param ids An array of unique identifiers.
     * @return A list of domain objects in the same order as requested in the ids parameter.
     */
    <T extends IDomainObject> List<T> fetchObjects(Class<T> clazz, String[] ids);
    
    /**
     * Returns the alias for the domain class.
     *
     * @param clazz Domain class whose alias is sought.
     * @return The alias for the domain class, or null if not found.
     */
    String getAlias(Class<? extends IDomainObject> clazz);
}
