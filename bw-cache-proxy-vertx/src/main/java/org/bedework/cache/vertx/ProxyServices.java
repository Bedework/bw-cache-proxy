/* ********************************************************************
    Licensed to Jasig under one or more contributor license
    agreements. See the NOTICE file distributed with this work
    for additional information regarding copyright ownership.
    Jasig licenses this file to you under the Apache License,
    Version 2.0 (the "License"); you may not use this file
    except in compliance with the License. You may obtain a
    copy of the License at:

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on
    an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied. See the License for the
    specific language governing permissions and limitations
    under the License.
*/
package org.bedework.cache.vertx;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bedework.cache.core.ICache;
import org.vertx.java.core.json.JsonObject;

/**
 * Stores singleton instances of the services used by the (potentially multiple)
 * instances of {@link ProxyVerticle}.
 *
 * @author eric.wittmann@redhat.com
 */
public class ProxyServices {
    
    private static ICache cache;

    /**
     * Initialize and configure the services.
     * @param config 
     */
    public static synchronized final void init(JsonObject config) {
        if (cache != null)
            return;
        
        System.out.println("Initializing services.");
        JsonObject cacheConfig = config.getObject("cache");
        String providerClassName = cacheConfig.getString("provider");
        Class<?> providerClass = null;
        try {
            providerClass = Class.forName(providerClassName);
        } catch (Exception e) {
            // Try to get the class using the context classloader 
            try {
                providerClass = Thread.currentThread().getContextClassLoader().loadClass(providerClassName);
            } catch (ClassNotFoundException e1) {
                throw new RuntimeException(e1);
            }
        }
        try {
            Map<String, String> providerConfig = new HashMap<String, String>();
            Set<String> fieldNames = cacheConfig.getFieldNames();
            for (String fieldName : fieldNames) {
                String fieldValue = cacheConfig.getString(fieldName);
                providerConfig.put(fieldName, fieldValue);
            }
            try {
                Constructor<?> ctor = providerClass.getConstructor(Map.class);
                cache = (ICache) ctor.newInstance(providerConfig);
            } catch (NoSuchMethodException | IllegalArgumentException | InvocationTargetException e) {
                cache = (ICache) providerClass.newInstance();
            }
        } catch (InstantiationException | IllegalAccessException | SecurityException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * @return the cache service
     */
    public static final ICache getCache() {
        return cache;
    }

}
