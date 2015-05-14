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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * @see org.vertx.java.platform.Verticle#start()
     */
    @Override
    public void start() {
        ProxyServices.init(this.container.config());

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
        final int instanceId = ProxyServices.newInstanceId();

        // Configure the local server
        HttpServer httpServer = vertx.createHttpServer();
        if (localSSL) {
            String localKeystore = localServer.getObject("keystore").getString("path");
            String localKeystorePassword = localServer.getObject("keystore").getString("password");

            httpServer = httpServer.setSSL(true).setKeyStorePath(localKeystore).setKeyStorePassword(localKeystorePassword);
        }

        // Start up the local server
        httpServer.requestHandler(new ProxyHandler(instanceId, client)).listen(localPort);

        if (instanceId == 0) {
            log.info("=====  ==============================  =====");
            log.info("=====  Bedework Caching Proxy Started  =====");
            log.info("=====  ==============================  =====");
            log.info("         Listening on: " + localPort);
            log.info("        Proxying Host: " + remoteHost);
            log.info("                 Port: " + remotePort);
            log.info("       Cache Provider: " + ProxyServices.getCache().getClass().getName());
            log.info("=====  ==============================  =====");
        }
    }
}