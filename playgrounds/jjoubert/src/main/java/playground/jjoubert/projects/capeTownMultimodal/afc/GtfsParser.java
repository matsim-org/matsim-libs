/* *********************************************************************** *
 * project: org.matsim.*
 * GtfsParser.java
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

/**
 * 
 */
package playground.jjoubert.projects.capeTownMultimodal.afc;

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.cadyts.pt.TransitStopFacilityLookUp;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.Facility;
import org.matsim.pt.router.FakeFacility;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.router.TransitRouterImplFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.mrieser.pt.fares.api.TransitFares;
import playground.southafrica.utilities.Header;

/**
 * Class to parse (and use) the GTFS-derived transit schedule.
 * 
 * @author jwjoubert
 */
public class GtfsParser {
	final private static Logger LOG = Logger.getLogger(GtfsParser.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(GtfsParser.class.toString(), args);

		String networkFile = args[0];
		String scheduleFile = args[1];
		
		Scenario sc = parseSchedule(scheduleFile, networkFile);
		int from = 1;
		int to = 25;
		double dist = findRoute(sc, from, to, Time.parseTime("08:00:00"));
		LOG.info("Distance from " + from + " to " + to + ": " + dist + "m");
		
		Header.printFooter();
	}
	
	
	private static Scenario parseSchedule(String scheduleFile, String networkFile){
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		new MatsimNetworkReader(sc.getNetwork()).parse(networkFile);
		new TransitScheduleReader(sc).readFile(scheduleFile);
		
		ActivityFacilitiesFactory aff = sc.getActivityFacilities().getFactory();
		
		return sc;
	}
	
	private static double findRoute(Scenario sc, int from, int to, double time){
		
		TransitRouterConfig tc = new TransitRouterConfig(sc.getConfig());
		TransitRouter tr = new TransitRouterImpl(tc , sc.getTransitSchedule());
		
		FakeFacility fFrom = new FakeFacility(sc.getNetwork().getNodes().get(Id.createNodeId("MyCiTi_" + from)).getCoord());
		FakeFacility fTo = new FakeFacility(sc.getNetwork().getNodes().get(Id.createNodeId("MyCiTi_" + to)).getCoord());
		
		List<Leg> route = tr.calcRoute(fFrom, fTo, time, sc.getPopulation().getFactory().createPerson(Id.createPersonId("dummy")));

		double dist = 0.0;
		for(Leg l : route){
			if(l.getRoute() != null){
				dist += l.getRoute().getDistance();
			}
		}
		
		return dist;
	}
	
	
	
	
	
	
	
	
	

}
