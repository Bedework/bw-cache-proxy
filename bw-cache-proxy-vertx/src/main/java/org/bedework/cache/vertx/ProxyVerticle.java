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
import org.vertx.java.platform.Verticle;

/**
 * The main entry point into the vert.x implementation of the Bedework
 * caching proxy server.
 * 
 * @author eric.wittmann@redhat.com
 */
public class ProxyVerticle extends Verticle {
    
    private static int instanceCounter = 0;
    private int requestCounter = 0;

    /**
     * @see org.vertx.java.platform.Verticle#start()
     */
    @Override
    public void start() {
        final boolean debugEnabled = this.container.config().getBoolean("debug");
        final String remoteHost = this.container.config().getString("remote-host");
        final int remotePort = this.container.config().getInteger("remote-port");
        final int localPort = this.container.config().getInteger("local-port");
        
        final HttpClient client = vertx.createHttpClient().setHost(remoteHost).setPort(remotePort);
        final int instanceId = instanceCounter++;

        vertx.createHttpServer().requestHandler(new Handler<HttpServerRequest>() {

            protected final void debug(int id, String message, Object ... args) {
                if (debugEnabled)
                    System.out.println("" + instanceId + "-" + id + ":: " + MessageFormat.format(message, args));
            }

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
                clientReq.headers().set("Host", remoteHost + ":" + remotePort);
                request.dataHandler(new Handler<Buffer>() {
                    public void handle(Buffer data) {
                        debug(requestId, "    Proxying data");
                        clientReq.write(data);
                    }
                });
                request.endHandler(new VoidHandler() {
                    public void handle() {
                        debug(requestId, "    Ending client request");
                        clientReq.end();
                    }
                });
                debug(requestId, "Handled");
            }
        }).listen(localPort);
        System.out.println("=====  ==============================  =====");
        System.out.println("=====  Bedework Caching Proxy Started  =====");
        System.out.println("=====  ==============================  =====");
        System.out.println("       Listening on:  " + localPort);
        System.out.println("       Proxying Host: " + remoteHost);
        System.out.println("                Port: " + remotePort);
        System.out.println("=====  ==============================  =====");
    }
}