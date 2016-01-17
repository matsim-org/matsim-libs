#!/usr/bin/env bash
echo "works with Mac and 64bit Linux only"
echo "for other OSs you need to get, build, and install grpc-java by yourself"
case "$OSTYPE" in
  darwin*)  echo "OSX detected"; PLUGIN="protoc-gen-grpc-java-osx" ;;
  linux*)   echo "LINUX detected" ; PLUGIN="protoc-gen-grpc-java-linux-x86-64";;
   *)        echo "unsupported: $OSTYPE" && exit -2;;
esac
protoc -I ../src.main.resources/proto --grpc_out=../src/main/java/ --plugin=protoc-gen-grpc=$PLUGIN ../src.main.resources/proto/MATSimInterface.proto
protoc -I ../src.main.resources/proto --java_out=../src/main/java/ ../src.main.resources/proto/MATSimInterface.proto 
echo "done"
