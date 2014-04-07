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
    software distributed under the License is distributedimport org.bedework.cache.core.beans.HttpResponseBean;
ONS OF ANY
    KIND, either express or implied. See the License for the
    specific language governing permissions and limitations
    under the License.
*/
package org.bedework.cache.core;

import org.bedework.cache.core.beans.CacheKeyBean;
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
    public String getETag(CacheKeyBean key);
    
    /**
     * Gets the cached http response for the given cache key.
     * @param key the cache key
     * @return a cached http response or null if not cached
     */
    public HttpResponseBean getResponse(CacheKeyBean key);
    
    /**
     * Updates the cache with a new entry.
     * @param key
     * @param etag
     * @param response
     */
    public void updateCache(CacheKeyBean key, String etag, HttpResponseBean response);

}
