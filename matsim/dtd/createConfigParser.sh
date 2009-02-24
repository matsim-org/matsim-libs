#!/bin/bash
# script to create jaxb bindings for config_v1.0.xsd
../libs/jaxb-2.1.7/bin/xjc.sh -d ../src/ -b configBindings.jxb xmlschemaBindings.xsd config_v0.1.xsd

