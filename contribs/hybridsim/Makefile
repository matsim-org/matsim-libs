#QUICK="-Dmaven.test.skip -Dmaven.javadoc.skip=true -Dsource.skip -Dassembly.skipAssembly=true -DskipTests"
QUICK=-Dmaven.javadoc.skip=true

default:
	cd ../../matsim ; mvn install ${QUICK} -DskipTests
	cd ../protobuf ; mvn clean eclipse:clean eclipse:eclipse install ${QUICK}
	mvn clean eclipse:clean eclipse:eclipse install ${QUICK}
