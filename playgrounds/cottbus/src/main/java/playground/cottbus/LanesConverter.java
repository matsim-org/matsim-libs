/* *********************************************************************** *
 * project: org.matsim.*
 * LanesConverter
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.cottbus;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReaderMatsimV1;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.lanes.LaneDefinitions;
import org.matsim.lanes.LaneDefinitionsReader11;
import org.matsim.lanes.LaneDefinitionsV11ToV20Conversion;

/**
 * @author 	rschneid-btu
 * start-up for converting laneDefinitionFiles V1.1 To V2.0
 */

public class LanesConverter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String chosenScenario = "cottbus";
		
		LaneDefinitions laneDefs = null;
		String schemaLocation = "../matsim/dtd/laneDefintitions_v1.1.xsd";
		String configFile = "./input/"+chosenScenario+"/config.xml";
		String laneFile = "./input/"+chosenScenario+"/laneDefinitions.xml";
		String networkFile = "./input/"+chosenScenario+"/network.xml";
		Config config = null;
		LaneDefinitions laneDefs22 = null;
		
		ConfigReaderMatsimV1 configReader = new ConfigReaderMatsimV1(config);
		/*configReader.readFile(configFile);
		Scenario scenario = new ScenarioImpl(config);
		
		LaneDefinitionsReader11 lanesReader11 = new LaneDefinitionsReader11(laneDefs, schemaLocation);
		NetworkReaderMatsimV1 networkReader = new NetworkReaderMatsimV1(scenario);
		try {
			lanesReader11.readFile(laneFile);
			networkReader.parse(networkFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		LaneDefinitionsV11ToV20Conversion converter = new LaneDefinitionsV11ToV20Conversion();
		laneDefs22 = converter.convertTo20(laneDefs, scenario.getNetwork());
		*/
		System.out.println("done");

	}
}

