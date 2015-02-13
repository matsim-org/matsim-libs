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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.facilities.FacilitiesWriter;

/**
 * @author johannes
 *
 */
public class FacilitiesSubSample {

	private final static Logger logger = Logger.getLogger(FacilitiesSubSample.class);
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
	
		double proba = Double.parseDouble(args[2]);
		
		logger.info("Loading facilities...");
		FacilitiesReaderMatsimV1 facReader = new FacilitiesReaderMatsimV1(scenario);
		facReader.readFile(args[0]);
		
		ActivityFacilities facilities = scenario.getActivityFacilities();
		logger.info(String.format("Loaded %s facilities.", facilities.getFacilities().size()));
		
		List<Id<ActivityFacility>> toRemove = new ArrayList<Id<ActivityFacility>>(facilities.getFacilities().size());
		for(Id<ActivityFacility> id : facilities.getFacilities().keySet()) {
			if(Math.random() > proba) {
				toRemove.add(id);
			}
		}
		
		logger.info(String.format("Removing %s facilities...", toRemove.size()));
		
		for(Id<ActivityFacility> id : toRemove) {
			facilities.getFacilities().remove(id);
		}
		
		logger.info(String.format("Writing %s facilities...", facilities.getFacilities().size()));
		FacilitiesWriter writer = new FacilitiesWriter(facilities);
		writer.write(args[1]);
		logger.info("Done.");
	}

}
