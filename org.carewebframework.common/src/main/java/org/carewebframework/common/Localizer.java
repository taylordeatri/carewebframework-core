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
package org.carewebframework.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Provides localization support.
 */
public class Localizer {
    
    public interface IMessageSource {
        
        /**
         * Retrieve a message for the specified locale given its id.
         * 
         * @param id The message identifier.
         * @param locale The locale.
         * @param args Optional message arguments.
         * @return A fully formatted message, or null if none was found.
         */
        String getMessage(String id, Locale locale, Object... args);
        
    }
    
    public interface ILocaleFinder {
        
        /**
         * Returns the default locale when none is specified.
         * 
         * @return The default locale
         */
        Locale getLocale();
    }
    
    private static final Log log = LogFactory.getLog(Localizer.class);
    
    private static final List<IMessageSource> messageSources = new ArrayList<>();
    
    private static ILocaleFinder localeFinder = new ILocaleFinder() {
        
        @Override
        public Locale getLocale() {
            return Locale.getDefault();
        }
        
    };
    
    /**
     * Registers a message source for resolving messages.
     * 
     * @param messageSource The message source.
     */
    public static void registerMessageSource(IMessageSource messageSource) {
        messageSources.add(messageSource);
    }
    
    /**
     * Returns a formatted message given a label identifier. Recognizes line continuation with
     * backslash characters.
     * 
     * @param id A label identifier.
     * @param locale The locale. If null, uses the default locale.
     * @param args Optional replaceable parameters.
     * @return The formatted label.
     */
    public static String getMessage(String id, Locale locale, Object... args) {
        locale = locale == null ? getDefaultLocale() : locale;
        
        for (IMessageSource messageSource : messageSources) {
            try {
                return messageSource.getMessage(id, locale, args).replace("\\\n", "");
            } catch (Exception e) {
                // Ignore and try next message source.
            }
        }
        // Failing resolution, just return null.
        log.warn("Label not found for identifier: " + id);
        return null;
    }
    
    /**
     * Returns the default locale.
     * 
     * @return The default locale.
     */
    public static Locale getDefaultLocale() {
        return localeFinder.getLocale();
    }
    
    /**
     * Sets the locale finder used to determine the default locale.
     * 
     * @param localeFinder An ILocaleFinder implementation.
     */
    public static void setLocaleFinder(ILocaleFinder localeFinder) {
        Localizer.localeFinder = localeFinder;
    }
    
    /**
     * Enforce static class.
     */
    private Localizer() {
    }
    
}
