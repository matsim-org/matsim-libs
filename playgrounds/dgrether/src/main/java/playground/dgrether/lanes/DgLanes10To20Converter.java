/* *********************************************************************** *
 * project: org.matsim.*
 * DgLanes10To20Converter
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.dgrether.lanes;

import org.matsim.lanes.run.LaneDefinitonsV11ToV20Converter;


/**
 * @author dgrether
 *
 */
public class DgLanes10To20Converter {

	
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
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		convertTests();
	}

}
