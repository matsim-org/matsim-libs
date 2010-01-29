#!/bin/bash
# script to create jaxb bindings for laneDefinitions_v1.1.xsd
../libs/jaxb-2.1.7/bin/xjc.sh -d ../src/main/java/ -b laneDefinitionsBindings_v1.1.jxb xmlschemaBindings.xsd laneDefinitions_v1.1.xsd

