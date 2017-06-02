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
package org.carewebframework.shell.ancillary;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.carewebframework.shell.elements.ElementBase;

/**
 * Private inner class to support registering allowed parent - child relationships for UI elements.
 */
public class RelatedClassMap {
    
    private final Map<Class<? extends ElementBase>, Set<Class<? extends ElementBase>>> map = new HashMap<>();
    
    /**
     * Returns the set of related classes for the specified class. If no set yet exists, one is
     * created.
     * 
     * @param clazz Class whose related class list is sought.
     * @return The set of classes related to the specified class.
     */
    private Set<Class<? extends ElementBase>> getRelatedClasses(Class<? extends ElementBase> clazz) {
        Set<Class<? extends ElementBase>> set = map.get(clazz);
        
        if (set == null) {
            set = new HashSet<>();
            map.put(clazz, set);
        }
        
        return set;
    }
    
    /**
     * Adds clazz2 as a related class to clazz1.
     * 
     * @param clazz1 The primary class.
     * @param clazz2 Class to be registered.
     */
    public void addRelated(Class<? extends ElementBase> clazz1, Class<? extends ElementBase> clazz2) {
        getRelatedClasses(clazz1).add(clazz2);
    }
    
    /**
     * Returns true if the specified class has any related classes.
     * 
     * @param clazz The primary class.
     * @return True if the specified class has any related classes.
     */
    public boolean hasRelated(Class<? extends ElementBase> clazz) {
        Set<Class<? extends ElementBase>> set = map.get(clazz);
        return set != null && !set.isEmpty();
    }
    
    /**
     * Returns true if class clazz2 or a superclass of clazz2 is related to clazz1.
     * 
     * @param clazz1 The primary class.
     * @param clazz2 The class to test.
     * @return True if class clazz2 or a superclass of clazz2 is related to clazz1.
     */
    public boolean isRelated(Class<? extends ElementBase> clazz1, Class<? extends ElementBase> clazz2) {
        Set<Class<? extends ElementBase>> set = map.get(clazz1);
        
        if (set != null) {
            for (Class<?> clazz : set) {
                if (clazz.isAssignableFrom(clazz2)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
}
