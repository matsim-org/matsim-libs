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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.FacilitiesWriter;

/**
 * @author johannes
 *
 */
public class MergeFacilities {

	private static final Logger logger = Logger.getLogger(MergeFacilities.class);
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ActivityFacilities all = FacilitiesUtils.createActivityFacilities();
		
		int counter = 0;
		for(int i = 0; i < args.length - 1; i++) {
			Config config = ConfigUtils.createConfig();
			Scenario scenario = ScenarioUtils.createScenario(config);
			FacilitiesReaderMatsimV1 facReader = new FacilitiesReaderMatsimV1(scenario);
			logger.info("Loading facilitites...");
			facReader.readFile(args[i]);
			logger.info("Merging facilities...");
			ActivityFacilities facilities = scenario.getActivityFacilities();
			for(ActivityFacility fac : facilities.getFacilities().values()) {
				ActivityFacility newFac = all.getFactory().createActivityFacility(Id.create(counter, ActivityFacility.class), fac.getCoord());
				counter++;
				for(ActivityOption opt : fac.getActivityOptions().values()) {
					ActivityOption newOpt = all.getFactory().createActivityOption(opt.getType());
					newFac.addActivityOption(newOpt);
					
//					newOpt = all.getFactory().createActivityOption("private");
//					newFac.addActivityOption(newOpt);
//					
//					newOpt = all.getFactory().createActivityOption("pickdrop");
//					newFac.addActivityOption(newOpt);
			
//					newOpt = all.getFactory().createActivityOption("misc");
//					newFac.addActivityOption(newOpt);
					
//					newOpt = all.getFactory().createActivityOption("outoftown");
//					newFac.addActivityOption(newOpt);
//					
//					newOpt = all.getFactory().createActivityOption("unknown");
//					newFac.addActivityOption(newOpt);
//					
//					newOpt = all.getFactory().createActivityOption("intown");
//					newFac.addActivityOption(newOpt);
				}
				all.addActivityFacility(newFac);
			}
		}

		logger.info(String.format("Total facilities: %s.", all.getFacilities().size()));
		FacilitiesWriter writer = new FacilitiesWriter(all);
		writer.write(args[args.length-1]);
	}

}
