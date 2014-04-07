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

import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.json.JsonObject;
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

        JsonObject proxyTo = this.container.config().getObject("proxy-to");
        JsonObject localServer = this.container.config().getObject("local-server");

        final String remoteHost = proxyTo.getString("host");
        final int remotePort = proxyTo.getInteger("port");
        final boolean remoteSSL = proxyTo.getBoolean("ssl");

        final int localPort = localServer.getInteger("port");
        final boolean localSSL = localServer.getBoolean("ssl");

        // Configure the HTTP client used to fetch data from the proxied server
        final HttpClient client = vertx.createHttpClient().setHost(remoteHost).setPort(remotePort);
        if (remoteSSL) {
            JsonObject keystore = proxyTo.getObject("keystore");
            if (keystore != null) {
                String remoteKeystore = keystore.getString("path");
                String remoteKeystorePassword = keystore.getString("password");
                client.setSSL(true).setKeyStorePath(remoteKeystore).setKeyStorePassword(remoteKeystorePassword);
            } else {
                client.setSSL(true).setTrustAll(true);
            }
        }
        final int instanceId = instanceCounter++;

        // Configure the local server
        HttpServer httpServer = vertx.createHttpServer();
        if (localSSL) {
            String localKeystore = localServer.getObject("keystore").getString("path");
            String localKeystorePassword = localServer.getObject("keystore").getString("password");
            
            httpServer = httpServer.setSSL(true).setKeyStorePath(localKeystore).setKeyStorePassword(localKeystorePassword);
        }
        
        
        httpServer.requestHandler(new ProxyHandler(debugEnabled, instanceId, client)).listen(localPort);
        System.out.println("=====  ==============================  =====");
        System.out.println("=====  Bedework Caching Proxy Started  =====");
        System.out.println("=====  ==============================  =====");
        System.out.println("       Listening on:  " + localPort);
        System.out.println("       Proxying Host: " + remoteHost);
        System.out.println("                Port: " + remotePort);
        System.out.println("=====  ==============================  =====");
    }
}