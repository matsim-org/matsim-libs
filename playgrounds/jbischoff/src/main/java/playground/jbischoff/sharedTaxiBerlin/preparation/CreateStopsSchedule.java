/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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
package playground.jbischoff.sharedTaxiBerlin.preparation;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class CreateStopsSchedule {
	public static void main(String[] args) {
	String net = "C:/Users/Joschka/Documents/shared-svn/projects/bvg_sharedTaxi/input/network-bvg_25833_cut_cleaned.xml.gz";
	String sched = 	"C:/Users/Joschka/Documents/shared-svn/projects/bvg_sharedTaxi/input/transitSchedule.xml.gz";
	String outputSched = 	"C:/Users/Joschka/Documents/shared-svn/projects/bvg_sharedTaxi/input/stoplocations.xml.gz";
	Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	new MatsimNetworkReader(scenario.getNetwork()).readFile(net);
	new TransitScheduleReader(scenario).readFile(sched);
	TransitScheduleFactory f = new TransitScheduleFactoryImpl();
	TransitSchedule stops = f.createTransitSchedule();
	for (TransitStopFacility stop : scenario.getTransitSchedule().getFacilities().values()){
		Link l = scenario.getNetwork().getLinks().get(stop.getLinkId());
		if (l!=null){
			TransitStopFacility newStop = f.createTransitStopFacility(stop.getId(), l.getCoord(), false);
			newStop.setLinkId(l.getId());
			newStop.setName(stop.getName());
			stops.addStopFacility(newStop);
		}
	}
	new TransitScheduleWriter(stops).writeFile(outputSched);
		
	}
}
