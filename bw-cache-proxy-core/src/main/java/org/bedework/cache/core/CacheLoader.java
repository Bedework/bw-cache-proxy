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
package org.bedework.cache.core;

import java.util.ServiceLoader;

/**
 * A simple accessor class for getting a handle on the {@link ICache}.
 *
 * @author eric.wittmann@redhat.com
 */
public class CacheLoader {

    /**
     * @return the cache to be used
     */
    public static final ICache loadCache() throws Exception {
        // Use the java service loader to get the configured 
        for (ICache cache : ServiceLoader.load(ICache.class)) {
            return cache;
        }
        throw new Exception("No ICache configured!");
    }

}
