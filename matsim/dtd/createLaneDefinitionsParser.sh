#!/bin/bash
# script to create jaxb bindings for laneDefinitions_v2.0.xsd
if [ "$#" = 1 ]; then
$1/bin/xjc.sh -d ../src/main/java/ -b laneDefinitionsBindings_v2.0.jxb xmlschemaBindings.xsd laneDefinitions_v2.0.xsd
else
  echo "no jaxb basedir given as first argument, cannot execute xjc!"
fi

