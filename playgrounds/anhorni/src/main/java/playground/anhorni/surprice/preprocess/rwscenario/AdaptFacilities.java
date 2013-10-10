/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.anhorni.surprice.preprocess.rwscenario;

import java.util.SortedSet;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.facilities.OpeningTime;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;


public class AdaptFacilities {	
	private ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());			
	private final static Logger log = Logger.getLogger(ConvertThurgau2Plans.class);	
		
	public static void main(final String[] args) {	
		int nbrArgs = 3;
		if (args.length != nbrArgs) {
			log.error("Provide correct number of arguments: " + nbrArgs + " arguments required!");
			System.exit(-1);
		}		
		AdaptFacilities creator = new AdaptFacilities();
		creator.run(args[0], args[1], args[2]);
		log.info("finished conversion ==============================================");
	}
	
	public void run(String facilitiesFileIn, String facilitiesFileOut, String networkFile) {
		new MatsimNetworkReader(scenario).readFile(networkFile);
		new FacilitiesReaderMatsimV1(scenario).readFile(facilitiesFileIn);
				
		// -------------------------------------------------------
		// adding new act type: business and merge work_sector 2 and 3
		
		for (ActivityFacility facility : this.scenario.getActivityFacilities().getFacilities().values()) {
			ActivityOptionImpl actOpt2 = (ActivityOptionImpl) facility.getActivityOptions().get("work_sector2");
			ActivityOptionImpl actOpt3 = (ActivityOptionImpl) facility.getActivityOptions().get("work_sector3");
			
			ActivityOptionImpl actOpt = actOpt2;		
			if (actOpt3 != null) actOpt = actOpt3;
			
			if (actOpt != null) {
				ActivityOptionImpl aOptWork = ((ActivityFacilityImpl)facility).createActivityOption("work");				
				SortedSet<OpeningTime> ots = actOpt.getOpeningTimes();
				aOptWork.setOpeningTimes(ots);
			}
		}
		
		TreeMap<Id, ActivityFacility> workFacilities = this.scenario.getActivityFacilities().getFacilitiesForActivityType("work");		
		for (ActivityFacility facility : workFacilities.values()) {
			ActivityOptionImpl aOptWork = (ActivityOptionImpl) facility.getActivityOptions().get("work");
			SortedSet<OpeningTime> ots = aOptWork.getOpeningTimes();
					
			ActivityOptionImpl aOptBusiness = ((ActivityFacilityImpl)facility).createActivityOption("business");
			aOptBusiness.setOpeningTimes(ots);
		}
		new FacilitiesWriter(this.scenario.getActivityFacilities()).write(facilitiesFileOut);
	}
}
