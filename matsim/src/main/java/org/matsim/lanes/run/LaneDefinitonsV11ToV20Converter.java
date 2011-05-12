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
package org.matsim.lanes.run;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.MatsimFileTypeGuesser;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.lanes.LaneDefinitions;
import org.matsim.lanes.LaneDefinitionsImpl;
import org.matsim.lanes.LaneDefinitionsReader11;
import org.matsim.lanes.LaneDefinitionsV11ToV20Conversion;
import org.matsim.lanes.LaneDefinitionsWriter20;
import org.matsim.lanes.MatsimLaneDefinitionsReader;
import org.xml.sax.SAXException;


/**
 * Class with main for LaneDefinitionsV11ToV20Conversion, see printUsage() for details.
 * @see org.matsim.lanes.LaneDefinitionsV11ToV20Conversion
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

		if (!(sid11.compareTo(MatsimLaneDefinitionsReader.SCHEMALOCATIONV11) == 0)){
			throw new IllegalArgumentException("File " + laneDefs11Filename + " is no laneDefinitions_v1.1.xsd format");
		}
	}
	
	public void convert(String laneDefs11Filename, String laneDefs20Filename, String networkFilename){
		this.checkFileTypes(laneDefs11Filename, laneDefs20Filename);
		
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader netReader = new MatsimNetworkReader(sc);
		netReader.readFile(networkFilename);
		Network net = sc.getNetwork();
		LaneDefinitions lanedefs11 = new LaneDefinitionsImpl();
		LaneDefinitionsReader11 reader11 = new LaneDefinitionsReader11(lanedefs11, MatsimLaneDefinitionsReader.SCHEMALOCATIONV11);
		try {
			reader11.readFile(laneDefs11Filename);
			LaneDefinitions lanedefs20 = new LaneDefinitionsV11ToV20Conversion().convertTo20(lanedefs11, net);
			LaneDefinitionsWriter20 writer20 = new LaneDefinitionsWriter20(lanedefs20);
			writer20.write(laneDefs20Filename);
		} catch (JAXBException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	private static void convertTests(){
		String base = "./test/input/org/matsim/";

		//fourWaysTest
		String inputDir = base + "signalsystems/TravelTimeFourWaysTest/";
		
		String net = inputDir + "network.xml.gz";
		String lanes = inputDir + "testLaneDefinitions_v1.1.xml";
		String lanes20 = inputDir + "testLaneDefinitions_v2.0.xml";
		
		new LaneDefinitonsV11ToV20Converter().convert(lanes, lanes20, net);
		
		//one agent test
		inputDir = base + "signalsystems/SignalSystemsOneAgentTest/";
		net = inputDir + "network.xml.gz";
		lanes = inputDir + "testLaneDefinitions_v1.1.xml";
		lanes20 = inputDir + "testLaneDefinitions_v2.0.xml";

		new LaneDefinitonsV11ToV20Converter().convert(lanes, lanes20, net);
		
		//travel time one way test
		inputDir = base + "signalsystems/TravelTimeOneWayTest/";
		net = inputDir + "network.xml.gz";
		lanes = inputDir + "testLaneDefinitions_v1.1.xml";
		lanes20 = inputDir + "testLaneDefinitions_v2.0.xml";

		new LaneDefinitonsV11ToV20Converter().convert(lanes, lanes20, net);
		//signalsystems integration test
		inputDir = base + "integration/signalsystems/SignalSystemsIntegrationTest/";
		net = inputDir + "network.xml.gz";
		lanes = inputDir + "testLaneDefinitions_v1.1.xml";
		lanes20 = inputDir + "testLaneDefinitions_v2.0.xml";

		new LaneDefinitonsV11ToV20Converter().convert(lanes, lanes20, net);
	}

	private static void printUsage(){
		log.info("Expects three arguments:");
		log.info("  path to lanedefinitions_v1.1 file");
		log.info("  path to lanedefinitions_v2.0 file to be written");
		log.info("  path to network file");
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
//		convertTests();
		if (args.length != 3){
			printUsage();
		}
		else {
			new LaneDefinitonsV11ToV20Converter().convert(args[0], args[1], args[2]);
		}
		
	}

}
