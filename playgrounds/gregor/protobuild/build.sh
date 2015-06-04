echo "works with Mac only"
echo "for other OSs you need to get, build, and install grpc-java by yourself"
protoc -I ../src.main.resources/proto --grpc_out=../src/main/java/ --plugin=protoc-gen-grpc=protoc-gen-grpc-java-0.7.0-osx-x86_64.exe ../src.main.resources/proto/MATSimInterface.proto 
protoc -I ../src.main.resources/proto --java_out=../src/main/java/ ../src.main.resources/proto/MATSimInterface.proto 
echo "done"
