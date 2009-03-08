#!/bin/bash
# script to create jaxb bindings for signalSystemsConfig_v1.1.xsd
../libs/jaxb-2.1.7/bin/xjc.sh -d ../src/ -b signalSystemsConfigBindings_v1.1.jxb xmlschemaBindings.xsd signalSystemsConfig_v1.1.xsd
