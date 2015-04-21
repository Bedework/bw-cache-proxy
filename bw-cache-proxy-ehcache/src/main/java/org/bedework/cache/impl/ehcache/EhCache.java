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
package org.bedework.cache.impl.ehcache;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.bedework.cache.core.ICache;
import org.bedework.cache.core.beans.CacheKeyBean;
import org.bedework.cache.core.beans.HttpResponseBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * An implementation of a cache that uses ehcache.
 *
 * @author eric.wittmann@redhat.com
 */
public class EhCache implements ICache {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private CacheManager manager;

    /**
     * Constructor.
     * @param properties
     */
    public EhCache(Map<String, String> properties) {
        log.info("Starting ehcache provider.");
        try {
            String cacheConfigPath = properties.get("ehcache-config");
            if (cacheConfigPath != null) {
                log.info("Loading ehcache config from: " + cacheConfigPath);
                manager = CacheManager.create(cacheConfigPath);
            } else {
                manager = CacheManager.create();
            }
        } catch (CacheException | IllegalStateException e) {
            log.error("Exception:", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * @see org.bedework.cache.core.ICache#getETag(org.bedework.cache.core.beans.CacheKeyBean)
     */
    @Override
    public String getETag(CacheKeyBean key) {
        EhCacheKey etagKey = new EhCacheKey(key, true);
        Cache cache = manager.getCache("proxyCache");
        cache.acquireReadLockOnKey(etagKey);

        try {
            Element element = cache.get(etagKey);
            if (element != null) {
                return (String) element.getObjectValue();
            } else {
                return null;
            }
        } finally {
            cache.releaseReadLockOnKey(etagKey);
        }
    }

    /**
     * @see org.bedework.cache.core.ICache#getResponse(org.bedework.cache.core.beans.CacheKeyBean)
     */
    @Override
    public HttpResponseBean getResponse(CacheKeyBean key) {
        EhCacheKey etagKey = new EhCacheKey(key, true);
        EhCacheKey cacheKey = new EhCacheKey(key, false);
        Cache cache = manager.getCache("proxyCache");
        cache.acquireReadLockOnKey(etagKey);

        try {
            Element element = cache.get(cacheKey);
            if (element != null) {
                return (HttpResponseBean) element.getObjectValue();
            } else {
                return null;
            }
        } finally {
            cache.releaseReadLockOnKey(etagKey);
        }
    }

    /**
     * @see org.bedework.cache.core.ICache#updateCache(org.bedework.cache.core.beans.CacheKeyBean, java.lang.String, org.bedework.cache.core.beans.HttpResponseBean)
     */
    @Override
    public void updateCache(CacheKeyBean key, String etag, HttpResponseBean response) {
        EhCacheKey etagKey = new EhCacheKey(key, true);
        EhCacheKey responseKey = new EhCacheKey(key, false);
        Cache cache = manager.getCache("proxyCache");
        cache.acquireWriteLockOnKey(etagKey);

        try {
            Element etagElem = new Element(etagKey, etag);
            Element responseElem = new Element(responseKey, response);
            cache.put(etagElem);
            cache.put(responseElem);
        } finally {
            cache.releaseWriteLockOnKey(etagKey);
        }
    }

}
