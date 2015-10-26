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

/**
 * 
 */
package playground.johannes.gsv.demand;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author johannes
 *
 */
public class TransitLineRemover {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		config.transit().setUseTransit(true);
		
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		TransitScheduleReader schedReader = new TransitScheduleReader(scenario);
		schedReader.readFile("/home/johannes/gsv/matsim/studies/netz2030/data/transitSchedule.routed.gk3.xml");
		
		TransitSchedule schedule = scenario.getTransitSchedule();
		Map<Id<TransitLine>, TransitLine> lines = schedule.getTransitLines();
		Set<TransitLine> toRemove = new HashSet<TransitLine>();
		for(TransitLine line : lines.values()) {
//			if(Math.random() > 0.5)
			if(line.getId().toString().startsWith("s"))
				toRemove.add(line);
		}
		
		for(TransitLine line : toRemove)
			schedule.removeTransitLine(line);
		
		Set<TransitStopFacility> usedFacilities = new HashSet<TransitStopFacility>();
		for(TransitLine line : lines.values()) {
			for(TransitRoute route : line.getRoutes().values()) {
				for(TransitRouteStop stop : route.getStops()) {
					TransitStopFacility fac = stop.getStopFacility();
					usedFacilities.add(fac);
				}
			}
		}
		
		Set<TransitStopFacility> remove = new HashSet<TransitStopFacility>();
		for(TransitStopFacility fac : schedule.getFacilities().values()) {
			if(!usedFacilities.contains(fac))
				remove.add(fac);
		}
		
		for(TransitStopFacility fac : remove)
			schedule.removeStopFacility(fac);
		
		TransitScheduleWriter writer = new TransitScheduleWriter(schedule);
		writer.writeFile("/home/johannes/gsv/matsim/studies/netz2030/data/transitSchedule.nosbahn.xml");
	}

}
