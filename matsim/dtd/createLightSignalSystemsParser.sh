#!/bin/bash
# script to create jaxb bindings for lightSignalSystems_v1.0.xsd
../libs/jaxb-2.1.7/bin/xjc.sh -d ../src/ -b lightSignalSystemsBindings.jxb xmlschemaBindings.xsd lightSignalSystems_v1.0.xsd

