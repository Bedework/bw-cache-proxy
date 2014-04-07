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

package org.bedework.cache.vertx;

import java.text.MessageFormat;

import org.vertx.java.core.Handler;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServerRequest;

/**
 * 
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
        debug(requestId, "Proxying request: " + request.uri());
        final HttpClientRequest clientReq = client.request(request.method(), request.uri(),
                new Handler<HttpClientResponse>() {
                    public void handle(HttpClientResponse clientResp) {
                        debug(requestId, "    Proxying to client");
                        request.response().setStatusCode(clientResp.statusCode());
                        request.response().setStatusMessage(clientResp.statusMessage());
                        request.response().headers().set(clientResp.headers());
                        clientResp.dataHandler(new Handler<Buffer>() {
                            public void handle(Buffer data) {
                                debug(requestId, "    Writing response data");
                                request.response().write(data);
                            }
                        });
                        clientResp.endHandler(new VoidHandler() {
                            public void handle() {
                                debug(requestId, "    Ending server response");
                                request.response().end();
                            }
                        });
                    }
                });
        clientReq.headers().set(request.headers());
        clientReq.headers().set("Host", client.getHost() + ":" + client.getPort());
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
}
