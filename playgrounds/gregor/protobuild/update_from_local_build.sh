#!/bin/bash

WORKING_DIR=`pwd`

LOCAL_BUILD_ROOT="/Users/laemmel/svn/github/grpc-java/"
LIB_BUILD_DIR="examples/build/distributions/"
LIBS="grpc-examples-0.1.0-SNAPSHOT.tar"

PROTOC_PLUGIN_DIR="compiler/build/binaries/java_pluginExecutable/x86_64/"
PROTOC_PLUGIN="protoc-gen-grpc-java"


TMP="/tmp/grpc-java-matsim"
mkdir $TMP
cp $LOCAL_BUILD_ROOT$LIB_BUILD_DIR$LIBS $TMP/
cd $TMP 
tar -xf $LIBS
cd $TMP/`basename $LIBS .tar`/lib

cp grpc* $WORKING_DIR/../lib/grpc/
cp netty* $WORKING_DIR/../lib/netty/


cd $WORKING_DIR
rm -fr $TMP

cp $LOCAL_BUILD_ROOT$PROTOC_PLUGIN_DIR$PROTOC_PLUGIN ./
