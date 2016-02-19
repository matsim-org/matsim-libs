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
\t cd contrib/protobuf
\t mvn eclipse:clean   # removes pre-existing eclipse-specific settings
\t mvn eclipse:eclipse
\t mvn clean install

It has worked when `target/generated-sources` shows up as source folder.   This is there `ProtobufEvents.java` is residing, which 
is otherwise denoted as missing at many places.
