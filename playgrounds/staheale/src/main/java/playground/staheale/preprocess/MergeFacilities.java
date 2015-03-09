/* *********************************************************************** *
 * project: org.matsim.*
 * MergeFacilities.java
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

package playground.staheale.preprocess;



import java.io.IOException;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.MatsimFacilitiesReader;

public class MergeFacilities {
	private static Logger log = Logger.getLogger(MergeFacilities.class);

	public static void main(String[] args) throws IOException {

		final ScenarioImpl scenarioWork = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		MatsimFacilitiesReader workFacReader = new MatsimFacilitiesReader((ScenarioImpl) scenarioWork);  
		System.out.println("Reading work facilities xml file... ");
		workFacReader.readFile("./input/workFacilitiesNew.xml.gz");
		System.out.println("Reading work facilities xml file...done.");
		ActivityFacilities workFacilities = scenarioWork.getActivityFacilities();
		log.info("Number of work facilities: " +workFacilities.getFacilities().size());


		//---------------------read home facilities xml file---------

		final ScenarioImpl scenarioHome = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		MatsimFacilitiesReader homeFacReader = new MatsimFacilitiesReader((ScenarioImpl) scenarioHome);  
		System.out.println("Reading home facilities xml file... ");
		homeFacReader.readFile("./input/homeFacilities.xml.gz");
		System.out.println("Reading home facilities xml file...done.");
		ActivityFacilities homeFacilities = scenarioHome.getActivityFacilities();
		log.info("Number of home facilities: " +homeFacilities.getFacilities().size());

		TreeMap<Id<ActivityFacility>, ActivityFacility> ActHomeFacilities = ((ActivityFacilitiesImpl) homeFacilities).getFacilitiesForActivityType("home");
		log.info("Number of facilities of activity type home: " +ActHomeFacilities.size());

		//------------------initial home facility file is zero------

		//    final ScenarioImpl scenarioActHome = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		//    
		//	MatsimFacilitiesReader aHomeFacReader = new MatsimFacilitiesReader((ScenarioImpl) scenarioActHome);  
		//	aHomeFacReader.readFile("./input/aHomeFacilities.xml");
		//	ActivityFacilitiesImpl aHomeFacilities = ((ScenarioImpl) scenarioActHome).getActivityFacilities();
		//    
		//	for (ActivityFacility f : ActHomeFacilities.values()) {
		//		ActivityFacilityImpl a = aHomeFacilities.createFacility(f.getId(), f.getCoord());
		//		a.createActivityOption("home");
		//		a.getActivityOptions().get("home").addOpeningTime(new OpeningTimeImpl(
		//			DayType.wk,
		//			0.0 * 3600,
		//			24.0 * 3600));
		//	}

		for (ActivityFacility f : ActHomeFacilities.values()) {

			ActivityFacility a = workFacilities.getFactory().createActivityFacility(f.getId(), f.getCoord());
			workFacilities.addActivityFacility(a);
			a.addActivityOption(workFacilities.getFactory().createActivityOption("home"));
			//        a.getActivityOptions().get("home").addOpeningTime(new OpeningTimeImpl(
			//				DayType.wk,
			//				0.0 * 3600,
			//				24.0 * 3600));
		}
		log.info("Number of work facilities: " +workFacilities.getFacilities().size());

		new FacilitiesWriter(workFacilities).write("./output/facilities2012.xml.gz");
	}
}
