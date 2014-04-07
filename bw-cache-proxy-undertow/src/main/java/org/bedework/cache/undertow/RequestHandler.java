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

package org.bedework.cache.undertow;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map.Entry;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;

/**
 * Primary http exchange handler.
 * 
 * @author eric.wittmann@redhat.com
 */
public class RequestHandler implements HttpHandler {

    private AsyncHttpClient client = new AsyncHttpClient();

    /**
     * Constructor.
     */
    public RequestHandler() {
    }

    /**
     * @see io.undertow.server.HttpHandler#handleRequest(io.undertow.server.HttpServerExchange)
     */
    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }

        String path = exchange.getRequestPath();
        System.out.println("Handling path: " + path);
        client.prepareGet("http://www.reddit.com/" + path).execute(new AsyncCompletionHandler<Response>() {
            @Override
            public Response onCompleted(Response response) throws Exception {
                exchange.setResponseCode(response.getStatusCode());
                for (Entry<String, List<String>> entry : response.getHeaders().entrySet()) {
                    if (!entry.getValue().isEmpty()) {
                        exchange.getResponseHeaders().put(new HttpString(entry.getKey()),
                                entry.getValue().iterator().next());
                    }
                }
                exchange.getResponseSender().send(response.getResponseBodyAsByteBuffer());
                return response;
            }

            @Override
            public void onThrowable(Throwable t) {
                t.printStackTrace();
                exchange.setResponseCode(500);
                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                StringWriter data = new StringWriter();
                t.printStackTrace(new PrintWriter(data));
                exchange.getResponseSender().send(data.getBuffer().toString());
            }
        }).get();
    }

}
