bw-cache-proxy
==============

A reverse proxy cache built for Bedework.


Getting Started (From Maven)
============================

It's really easy to get started with the Bedework Caching Proxy Server.  All
you need to do is:

	$ cd bw-cache-proxy
	$ mvn clean install
	$ cd bw-cache-proxy-vertx
	$ mvn vertx:runMod

This will start up the Bedework Caching Proxy Server using a sample configuration
included here:

	bw-cache-proxy-vertx/src/main/config

You can change which configuration is used by modifying the following file:

	bw-cache-proxy-vertx/pom.xml


Getting Started (Using vert.x)
==============================

Instead of running straight from the source repository, you can instead run the
module using vert.x.  To do this, follow these steps:

TBD

