/* *********************************************************************** *
 * project: org.matsim.*
 * LaneDefinitonsV11ToV20Converter
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.lanes.data.v11;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.MatsimFileTypeGuesser;
import org.matsim.lanes.data.v20.LaneDefinitionsReader;
import org.matsim.lanes.data.v20.LaneDefinitions20;
import org.matsim.lanes.data.v20.LaneDefinitionsWriter20;


/**
 * Class with main for LaneDefinitionsV11ToV20Conversion, see printUsage() for details.
 * @see LaneDefinitionsV11ToV20Conversion
 * @author dgrether
 */
public class LaneDefinitonsV11ToV20Converter {

	private static final Logger log = Logger.getLogger(LaneDefinitonsV11ToV20Converter.class);

	public LaneDefinitonsV11ToV20Converter(){
	}
	
	private void checkFileTypes(String laneDefs11Filename, String laneDefs20Filename){
		MatsimFileTypeGuesser fileTypeGuesser;
		fileTypeGuesser = new MatsimFileTypeGuesser(laneDefs11Filename);
		String sid11 = fileTypeGuesser.getSystemId();

		if (!(sid11.compareTo(LaneDefinitionsReader.SCHEMALOCATIONV11) == 0)){
			throw new IllegalArgumentException("File " + laneDefs11Filename + " is no laneDefinitions_v1.1.xsd format");
		}
	}
	
	public void convert(String laneDefs11Filename, String laneDefs20Filename, String networkFilename){
		this.checkFileTypes(laneDefs11Filename, laneDefs20Filename);
		Config config = ConfigUtils.createConfig();
		config.scenario().setUseLanes(true);
		Scenario sc = ScenarioUtils.createScenario(config);
		MatsimNetworkReader netReader = new MatsimNetworkReader(sc);
		netReader.readFile(networkFilename);
		Network net = sc.getNetwork();
		LaneDefinitions11 lanedefs11 = new LaneDefinitions11Impl();
		LaneDefinitionsReader11 reader11 = new LaneDefinitionsReader11(lanedefs11, LaneDefinitionsReader.SCHEMALOCATIONV11);
		reader11.readFile(laneDefs11Filename);
		LaneDefinitions20 lanedefs20 = LaneDefinitionsV11ToV20Conversion.convertTo20(lanedefs11, net);
		LaneDefinitionsWriter20 writer20 = new LaneDefinitionsWriter20(lanedefs20);
		writer20.write(laneDefs20Filename);
	}

	private static void printUsage(){
		log.info("Expects three arguments:");
		log.info("  path to lanedefinitions_v1.1 file");
		log.info("  path to lanedefinitions_v2.0 file to be written");
		log.info("  path to network file");
	}
	
	public static void main(String[] args) {
		if (args.length != 3){
			printUsage();
		}
		else {
			new LaneDefinitonsV11ToV20Converter().convert(args[0], args[1], args[2]);
		}
		
	}

}
