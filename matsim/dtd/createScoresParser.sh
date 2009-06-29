#!/bin/bash
# script to create jaxb bindings for laneDefinitions_v1.1.xsd
../libs/jaxb-2.1.7/bin/xjc.sh -d ../src/ -b scoresBindings.jxb xmlschemaBindings.xsd scores_v0.1.xsd