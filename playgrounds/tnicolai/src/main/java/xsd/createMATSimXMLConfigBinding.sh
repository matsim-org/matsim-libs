#!/bin/bash
# script to create jaxb bindings for People
# im Terminal im Ordner xsds (/Users/thomas/Development/workspace/OPUS_MATSim_Config_Test/xsds) folgendes Kommando eingeben.
../libs/jaxb-2.1.7/bin/xjc.sh -p com.matsim.config -d ../playground/thomas/urbansim/ MATSim4UrbanSimTestConfig2.xsd