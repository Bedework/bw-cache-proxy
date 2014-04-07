/*
 * Copyright 2013 JBoss Inc
 *
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
 */

package org.bedework.cache.core;

import org.bedework.cache.core.beans.HttpResponseBean;

/**
 * Interface that must be implemented by specific cache implementations.  This
 * interface is used by the proxy in order to actually cache the web page and
 * ETag information.
 *
 * @author eric.wittmann@redhat.com
 */
public interface ICache {
    
    /**
     * Gets the ETag associated with the cached response for the given cache key.
     * @param key a cache key
     * @return the ETag of the http response cached at the given key, or null if not cached
     */
    public String getETag(String key);
    
    /**
     * Gets the cached http response for the given cache key.
     * @param key the cache key
     * @return a cached http response or null if not cached
     */
    public HttpResponseBean getResponse(String key);

}
