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
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.scenario.ScenarioUtils;

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
	
		logger.info("Loading facilities...");
		FacilitiesReaderMatsimV1 facReader = new FacilitiesReaderMatsimV1(scenario);
		facReader.readFile("/home/johannes/gsv/osm/facilities/facilities.full.h20.xml");
		
		ActivityFacilities facilities = scenario.getActivityFacilities();
		logger.info(String.format("Loaded %s facilities.", facilities.getFacilities().size()));
		
		List<Id> toRemove = new ArrayList<Id>(facilities.getFacilities().size());
		for(Id id : facilities.getFacilities().keySet()) {
			if(Math.random() > 0.5) {
				toRemove.add(id);
			}
		}
		
		logger.info(String.format("Removing %s facilities...", toRemove.size()));
		
		for(Id id : toRemove) {
			facilities.getFacilities().remove(id);
		}
		
		logger.info(String.format("Writing %s facilities...", facilities.getFacilities().size()));
		FacilitiesWriter writer = new FacilitiesWriter(facilities);
		writer.write("/home/johannes/gsv/osm/facilities/facilities.home.10.xml");
		logger.info("Done.");
	}

}
