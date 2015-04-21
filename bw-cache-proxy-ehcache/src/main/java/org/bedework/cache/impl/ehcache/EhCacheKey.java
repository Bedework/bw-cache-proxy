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

import org.bedework.cache.core.beans.CacheKeyBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

/**
 * Cache key used by the ehcache impl.
 *
 * @author eric.wittmann@redhat.com
 */
public class EhCacheKey implements Serializable {
    private static final long serialVersionUID = 5338267390610485654L;

    private CacheKeyBean key;
    private boolean etag;

    /**
     * Constructor.
     * @param key
     * @param etag
     */
    public EhCacheKey(CacheKeyBean key, boolean etag) {
        this.setKey(key);
        this.setEtag(etag);
    }

    /**
     * Constructor.
     */
    protected EhCacheKey() {
    }

    /**
     * @return the etag
     */
    public boolean isEtag() {
        return etag;
    }

    /**
     * @param etag the etag to set
     */
    public void setEtag(boolean etag) {
        this.etag = etag;
    }

    /**
     * @return the key
     */
    public CacheKeyBean getKey() {
        return key;
    }

    /**
     * @param key the key to set
     */
    public void setKey(CacheKeyBean key) {
        this.key = key;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (etag ? 1231 : 1237);
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        return result;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        EhCacheKey other = (EhCacheKey) obj;
        if (etag != other.etag)
            return false;
        if (key == null) {
            if (other.key != null)
                return false;
        } else if (!key.equals(other.key))
            return false;
        return true;
    }

}
