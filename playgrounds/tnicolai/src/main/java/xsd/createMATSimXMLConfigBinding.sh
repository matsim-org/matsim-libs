#!/bin/bash
# script to create jaxb bindings
# the command is structured as folloed:
# location/of/xjc.sh -p package_name -d directory location/of/xsd_file

../libs/jaxb-2.1.7/bin/xjc.sh -p com.matsim.config -d ../playground/thomas/urbansim/ MATSim4UrbanSimTestConfig2.xsd