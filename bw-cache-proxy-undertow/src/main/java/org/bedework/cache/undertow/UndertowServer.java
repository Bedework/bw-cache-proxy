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
package org.bedework.cache.undertow;

import io.undertow.Undertow;

/**
 * An undertow based bw-cache-proxy server.
 * 
 * @author eric.wittmann@redhat.com
 */
public class UndertowServer {
    
    private static final RequestHandler handler = new RequestHandler();

    /**
     * Main entry point.
     * @param args
     */
    public static void main(final String[] args) {
        Undertow server = Undertow.builder().addHttpListener(8080, "localhost").setHandler(handler).build();
        server.start();
    }

}
