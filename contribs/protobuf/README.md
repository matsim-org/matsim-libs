## Overview

The protobuf contrib provides a protocol buffer implementation and converter for the MATSim event infrastructure
 
## Usage

TODO

## Build information

Building this contribution with IntelliJ or maven in the command line works out of the box. 

### Eclipse

Eclipse users, however, need to invoke 
    mvn eclipse:eclipse
before eclipse is able to compile it. This step needs to be repeated after every ``git pull''.

A bit more:
	cd contrib/protobuf
	mvn eclipse:clean   # removes pre-existing eclipse-specific settings
	mvn eclipse:eclipse
	mvn clean install

`target/generated-sources` needs to show up as source folder.   This is where `ProtobufEvents.java` is residing, which 
is otherwise denoted as missing at many places.
