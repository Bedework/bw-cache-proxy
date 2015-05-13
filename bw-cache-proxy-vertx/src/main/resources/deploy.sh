#!/bin/sh

# Primitive deploy script which assumes the source is located in a quickstart with a vert.x
# located at the same level.

# Should be in proxy-vertx/src/main/resources
resourceDir=`dirname $0`

bwcache="$resourceDir/../../../../../bw-cache-proxy"

vertx="$resourceDir/../../../../../vert.x"

vertxlib="$vertx/mods/org.bedework~bw-cache-proxy-vertx~3.10-SNAPSHOT/lib/"

cp $bwcache/bw-cache-proxy-vertx/target/bw-cache-proxy-vertx-3.10-SNAPSHOT.jar $vertxlib
cp $bwcache/bw-cache-proxy-core/target/bw-cache-proxy-core-3.10-SNAPSHOT.jar $vertxlib
cp $bwcache/bw-cache-proxy-ehcache/target/bw-cache-proxy-ehcache-3.10-SNAPSHOT.jar $vertxlib
cp $bwcache/bw-cache-proxy-inmem/target/bw-cache-proxy-inmem-3.10-SNAPSHOT.jar $vertxlib

rm vert.x.zip
zip -r vert.x.zip vert.x
