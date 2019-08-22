## Overview

Hybrid simulation 
 
## Usage

TODO

## Build information

Building this contribution with IntelliJ or maven in the command line works out of the box. 

### IntelliJ
If `HybridSimProto.java` is denoted as missing, right-click on the protobuffolder in IntelliJ and go to "Maven" -> "Generate Sources and Update Folders". Maybe do a Maven -> "Reimport" before to update the dependencies.

After that, `HybridSimProto.java` should show up in `target/generated-sources` 

### eclipse

Eclipse users, however, need to invoke 
	
	mvn eclipse:eclipse

before eclipse is able to compile it. This step needs to be repeated after every ``git pull''.

See the protobuf README.md for a bit more info.
