#!/usr/bin/env bash
#cd ../../matsim
#mvn clean
#mvn install -DskipTests=true
#
#cd ../contrib
#mvn clean
#mvn install -fae -DskipTests=true

cd ../playgrounds/gregor
mvn clean
mvn -Prelease -DskipTests=true

