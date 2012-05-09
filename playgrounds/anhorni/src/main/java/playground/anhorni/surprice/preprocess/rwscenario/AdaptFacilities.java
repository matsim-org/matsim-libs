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

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.facilities.OpeningTime.DayType;
import org.matsim.core.facilities.OpeningTimeImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.facilities.ActivityFacilityImpl;


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
		
		QuadTree<ActivityFacilityImpl> otherFacQuadTree = null;
		
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		
		for (ActivityFacility f : this.scenario.getActivityFacilities().getFacilities().values()) {
			if (f.getActivityOptions().get("home") != null) {
				if (f.getCoord().getX() < minx) { minx = f.getCoord().getX(); }
				if (f.getCoord().getY() < miny) { miny = f.getCoord().getY(); }
				if (f.getCoord().getX() > maxx) { maxx = f.getCoord().getX(); }
				if (f.getCoord().getY() > maxy) { maxy = f.getCoord().getY(); }
			}
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;		
		System.out.println("        xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
		otherFacQuadTree = new QuadTree<ActivityFacilityImpl>(minx, miny, maxx, maxy);
		
		log.info("add act opt other ................");
		for (ActivityFacility facility : this.scenario.getActivityFacilities().getFacilities().values()) {
						
			if (otherFacQuadTree.get(facility.getCoord().getX(), facility.getCoord().getY(), 100.0).size() == 0 
					&& (facility.getActivityOptions().containsKey("home") ||
							facility.getActivityOptions().containsKey("work") ||
							facility.getActivityOptions().containsKey("shop") ||
							facility.getActivityOptions().containsKey("education") ||
							facility.getActivityOptions().containsKey("leisure"))) { 
				
				otherFacQuadTree.put(facility.getCoord().getX(), facility.getCoord().getY(),(ActivityFacilityImpl) facility);
				((ActivityFacilityImpl)facility).createActivityOption("other");
				ActivityOption actOpt = ((ActivityFacilityImpl)facility).getActivityOptions().get("other");
				
				for (DayType day : DayType.values()) {				
					actOpt.addOpeningTime(new OpeningTimeImpl(day, 0.0 * 3600, 24.0 * 3600));
				}
			}
		}
		new FacilitiesWriter(this.scenario.getActivityFacilities()).write(facilitiesFileOut);
	}
}
