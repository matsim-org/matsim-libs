/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop.osm;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.facilities.FacilitiesWriter;

import playground.johannes.sna.util.ProgressLogger;

/**
 * @author johannes
 *
 */
public class AddDummyActOptions {

	private static final Logger logger = Logger.getLogger(AddDummyActOptions.class);
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		logger.info("Loading facilities...");
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		FacilitiesReaderMatsimV1 facReader = new FacilitiesReaderMatsimV1(scenario);
		facReader.readFile(args[0]);
		ActivityFacilities facilities = scenario.getActivityFacilities();
		
		logger.info("Adding dummy activity options...");
		ProgressLogger.init(facilities.getFacilities().size(), 1, 10);
		
		for(ActivityFacility facility : facilities.getFacilities().values()) {
			ActivityOption newOpt = null;//facilities.getFactory().createActivityOption("private");
//			facility.addActivityOption(newOpt);
			
//			newOpt = facilities.getFactory().createActivityOption("pickdrop");
//			newOpt.setCapacity(1);
//			facility.addActivityOption(newOpt);
	
			newOpt = facilities.getFactory().createActivityOption("misc");
			newOpt.setCapacity(1);
			facility.addActivityOption(newOpt);
			
//			newOpt = facilities.getFactory().createActivityOption("outoftown");
//			newOpt.setCapacity(1);
//			facility.addActivityOption(newOpt);
//			
//			newOpt = facilities.getFactory().createActivityOption("unknown");
//			newOpt.setCapacity(1);
//			facility.addActivityOption(newOpt);
//			
//			newOpt = facilities.getFactory().createActivityOption("intown");
//			newOpt.setCapacity(1);
//			facility.addActivityOption(newOpt);
			
			ProgressLogger.step();
		}
		
		FacilitiesWriter writer = new FacilitiesWriter(facilities);
		writer.write(args[1]);

	}

}
