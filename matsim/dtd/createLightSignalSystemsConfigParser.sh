#!/bin/bash
# script to create jaxb bindings for lightSignalSystemsConfig_v1.0.xsd
../libs/jaxb-2.1.7/bin/xjc.sh -d ../src/ -b lightSignalSystemsConfigBindings.jxb lightSignalSystemsConfig_v1.0.xsd
