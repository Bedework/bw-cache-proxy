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
package org.bedework.cache.impl.inmem;

import java.util.HashMap;
import java.util.Map;

import org.bedework.cache.core.ICache;
import org.bedework.cache.core.beans.CacheKeyBean;
import org.bedework.cache.core.beans.HttpResponseBean;

/**
 * A very simple in-memory cache.  This should not be used for anything except
 * debugging the proxy server.
 *
 * @author eric.wittmann@redhat.com
 */
public class InMemoryCache implements ICache {
    
    private Map<CacheKeyBean, CacheEntry> cache = new HashMap<CacheKeyBean, CacheEntry>();

    /**
     * Constructor.
     */
    public InMemoryCache() {
    }

    /**
     * @see org.bedework.cache.core.ICache#getETag(org.bedework.cache.core.beans.CacheKeyBean)
     */
    @Override
    public synchronized String getETag(CacheKeyBean key) {
        return cache.get(key).etag;
    }
    
    /**
     * @see org.bedework.cache.core.ICache#getTimestamp(org.bedework.cache.core.beans.CacheKeyBean)
     */
    @Override
    public synchronized Long getTimestamp(CacheKeyBean key) {
        return cache.get(key).timestamp;
    }

    /**
     * @see org.bedework.cache.core.ICache#getResponse(org.bedework.cache.core.beans.CacheKeyBean)
     */
    @Override
    public synchronized HttpResponseBean getResponse(CacheKeyBean key) {
        return cache.get(key).response;
    }
    
    /**
     * @see org.bedework.cache.core.ICache#updateCache(org.bedework.cache.core.beans.CacheKeyBean, java.lang.String, org.bedework.cache.core.beans.HttpResponseBean)
     */
    @Override
    public synchronized void updateCache(CacheKeyBean key, String etag, HttpResponseBean response) {
        CacheEntry entry = new CacheEntry();
        entry.timestamp = System.currentTimeMillis();
        entry.etag = etag;
        entry.response = response;
        cache.put(key, entry);
    }
    
    private static final class CacheEntry {
        public long timestamp;
        public String etag;
        public HttpResponseBean response;
    }
}
