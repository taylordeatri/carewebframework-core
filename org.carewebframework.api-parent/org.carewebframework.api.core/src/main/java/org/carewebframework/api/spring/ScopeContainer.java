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
package org.carewebframework.api.spring;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;

/**
 * IOC container for the custom scopes.
 */
public class ScopeContainer implements Scope {
    
    private static final Log log = LogFactory.getLog(ScopeContainer.class);
    
    private final Map<String, Object> beans = new HashMap<>();
    
    private final Map<String, Runnable> destructionCallbacks = new HashMap<>();
    
    private String conversationId;
    
    @Override
    public Object remove(String key) {
        synchronized (this) {
            destructionCallbacks.remove(key);
            return beans.remove(key);
        }
    }
    
    @Override
    public Object get(String name, ObjectFactory<?> objectFactory) {
        synchronized (this) {
            Object bean = beans.get(name);
            
            if (bean == null) {
                bean = objectFactory.getObject();
                beans.put(name, bean);
            }
            
            return bean;
        }
    }
    
    /**
     * Register a bean destruction callback.
     * 
     * @param name Bean name.
     * @param callback Callback.
     */
    @Override
    public void registerDestructionCallback(String name, Runnable callback) {
        synchronized (this) {
            destructionCallbacks.put(name, callback);
        }
    }
    
    /**
     * For orphan containers.
     */
    @Override
    protected void finalize() throws Throwable {
        destroy();
        super.finalize();
    }
    
    /**
     * Calls all registered destruction callbacks and removes all bean references from the
     * container.
     */
    public void destroy() {
        for (Entry<String, Runnable> entry : destructionCallbacks.entrySet()) {
            try {
                entry.getValue().run();
            } catch (Throwable t) {
                log.error("Error during destruction callback for bean " + entry.getKey(), t);
            }
        }
        
        beans.clear();
        destructionCallbacks.clear();
    }
    
    @Override
    public Object resolveContextualObject(String key) {
        return null;
    }
    
    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
    
    @Override
    public String getConversationId() {
        return conversationId;
    }
}
