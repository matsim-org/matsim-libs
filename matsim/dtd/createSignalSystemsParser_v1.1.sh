#!/bin/bash
# script to create jaxb bindings for signalSystems_v1.1.xsd
../libs/jaxb-2.1.7/bin/xjc.sh -d ../src/ -b signalSystemsBindings_v1.1.jxb xmlschemaBindings.xsd signalSystems_v1.1.xsd

