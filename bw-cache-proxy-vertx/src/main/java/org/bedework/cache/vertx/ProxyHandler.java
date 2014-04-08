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

import java.text.MessageFormat;
import java.util.Map.Entry;

import org.bedework.cache.core.beans.CacheKeyBean;
import org.bedework.cache.core.beans.HttpResponseBean;
import org.vertx.java.core.Handler;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServerRequest;

/**
 * The http request handler used by the vert.x implementation of the bedework
 * caching proxy server.
 * 
 * @author eric.wittmann@redhat.com
 */
public class ProxyHandler implements Handler<HttpServerRequest> {
    
    private boolean debugEnabled;
    private int instanceId;
    private HttpClient client;
    private int requestCounter = 0;

    /**
     * Constructor.
     * @param debugEnabled
     * @param instanceId
     * @param client
     */
    public ProxyHandler(boolean debugEnabled, int instanceId, HttpClient client) {
        this.debugEnabled = debugEnabled;
        this.instanceId = instanceId;
        this.client = client;
    }

    /**
     * Write a debug message to standard output if debug is enabled.
     * @param id
     * @param message
     * @param args
     */
    protected final void debug(int id, String message, Object ... args) {
        if (debugEnabled)
            System.out.println("" + instanceId + "-" + id + ":: " + MessageFormat.format(message, args));
    }

    /**
     * @see org.vertx.java.core.Handler#handle(java.lang.Object)
     */
    @Override
    public void handle(final HttpServerRequest request) {
        final int requestId = requestCounter++;
        
        final String uri = request.uri();
        final String method = request.method();
        final CacheKeyBean key = new CacheKeyBean(method, uri);
        
        // Check the cache for the page
        final String requestedETag = request.headers().get("If-None-Match");
        final String cachedETag = ProxyServices.getCache().getETag(key);

        debug(requestId, "Proxying request: " + uri);
        debug(requestId, "    Requested ETag: " + requestedETag);
        debug(requestId, "    Cached ETag:    " + cachedETag);
        final HttpClientRequest clientReq = client.request(method, uri,
                new Handler<HttpClientResponse>() {
                    public void handle(HttpClientResponse clientResp) {
                        debug(requestId, "    Proxying to client");
                        
                        int statusCode = clientResp.statusCode();
                        
                        // If the response is "Not Modifed" then use our cached copy or reply with another 304
                        if (statusCode == 304) {
                            if (requestedETag != null && cachedETag != null && cachedETag.equals(requestedETag)) {
                                sendNotModified(requestId, request);
                            } else if (cachedETag != null) {
                                sendCachedCopy(requestId, request, key);
                            } else {
                                justSend(requestId, clientResp, request);
                            }
                            return;
                        }
                        
                        // If we got a 200-299 response, cache it and return it
                        if (statusCode >= 200 && statusCode < 300) {
                            cacheAndSend(requestId, clientResp, request, key);
                            return;
                        }
                        
                        // If we get any other response, send it without caching
                        justSend(requestId, clientResp, request);
                    }
                });
        clientReq.headers().set(request.headers());
        clientReq.headers().set("Host", client.getHost() + ":" + client.getPort());
        if (cachedETag != null) {
            clientReq.headers().set("If-None-Match", cachedETag);
        } else {
            clientReq.headers().remove("If-None-Match");
        }
        request.dataHandler(new Handler<Buffer>() {
            public void handle(Buffer data) {
                clientReq.write(data);
            }
        });
        request.endHandler(new VoidHandler() {
            public void handle() {
                debug(requestId, "    Ending client request");
                clientReq.end();
            }
        });
    }

    /**
     * Since the ETags matched, send a 304 Not Modified response back to the client.
     * @param requestId 
     * @param request
     */
    protected void sendNotModified(int requestId, HttpServerRequest request) {
        debug(requestId, "    Sending 304 Not Modified");
        request.response().setStatusCode(304);
        request.response().setStatusMessage("Not Modified");
        request.response().end();
    }

    /**
     * Send our cached copy back to the client.
     * @param requestId 
     * @param request
     * @param key
     */
    protected void sendCachedCopy(int requestId, HttpServerRequest request, CacheKeyBean key) {
        debug(requestId, "    Sending cached response.");
        HttpResponseBean response = ProxyServices.getCache().getResponse(key);
        if (response == null) {
            throw new RuntimeException("Missing cache entry: " + request.uri());
        }
        doSendCachedCopy(request, response);
    }

    /**
     * Cache the response and then send it back to the client.
     * @param requestId 
     * @param clientResp
     * @param request
     * @param key
     */
    protected void cacheAndSend(final int requestId, final HttpClientResponse clientResp, final HttpServerRequest request,
            final CacheKeyBean key) {
        debug(requestId, "    Caching and sending response.  Code=" + clientResp.statusCode());
        final Buffer dataToCache = new Buffer();
        final HttpResponseBean response = new HttpResponseBean();
        response.setCode(clientResp.statusCode());
        response.setMessage(clientResp.statusMessage());
        for (Entry<String, String> entry : clientResp.headers()) {
            response.getHeaders().put(entry.getKey(), entry.getValue());
        }
        final String etag = clientResp.headers().get("ETag");
        
        clientResp.dataHandler(new Handler<Buffer>() {
            public void handle(Buffer data) {
                dataToCache.appendBuffer(data);
            }
        });
        clientResp.endHandler(new VoidHandler() {
            public void handle() {
                response.setBody(dataToCache.getBytes());
                if (etag != null) {
                    debug(requestId, "    Actually caching response.");
                    ProxyServices.getCache().updateCache(key, etag, response);
                }
                doSendCachedCopy(request, response);
            }
        });
    }

    /**
     * Proxy the response back to the client without caching it.
     * @param requestId 
     * @param clientResp
     * @param request
     */
    protected void justSend(int requestId, final HttpClientResponse clientResp, final HttpServerRequest request) {
        debug(requestId, "    Sending response (no caching) code=" + clientResp.statusCode());
        request.response().setStatusCode(clientResp.statusCode());
        request.response().setStatusMessage(clientResp.statusMessage());
        request.response().headers().set(clientResp.headers());
        clientResp.dataHandler(new Handler<Buffer>() {
            public void handle(Buffer data) {
                request.response().write(data);
            }
        });
        clientResp.endHandler(new VoidHandler() {
            public void handle() {
                request.response().end();
            }
        });
    }

    /**
     * Does the work of sending the cached response back to the client.  This method
     * is shared by sendCachedCopy and cacheAndSend
     * @param request
     * @param response
     */
    private void doSendCachedCopy(HttpServerRequest request, HttpResponseBean response) {
        request.response().setStatusCode(response.getCode());
        request.response().setStatusMessage(response.getMessage());
        request.response().headers().set(response.getHeaders());
        Buffer buffer = new Buffer(response.getBody());
        request.response().write(buffer);
        request.response().end();
    }

}
