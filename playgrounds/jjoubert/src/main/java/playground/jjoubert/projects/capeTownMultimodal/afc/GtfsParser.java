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
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.router.FakeFacility;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import playground.southafrica.utilities.Header;

/**
 * Class to parse (and use) the GTFS-derived transit schedule.
 * 
 * @author jwjoubert
 */
public class GtfsParser {
	final private static Logger LOG = Logger.getLogger(GtfsParser.class);
	private Scenario sc;
	private TransitRouterConfig trConfig;
	private TransitRouter trRouter;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(GtfsParser.class.toString(), args);

		String networkFile = args[0];
		String scheduleFile = args[1];
		
		GtfsParser gp = new GtfsParser(networkFile, scheduleFile);
		int from = 1;
		int to = 25;
		double dist = gp.findRouteDistance(from, to, Time.parseTime("08:00:00"));
		LOG.info(String.format("Distance from %d to %d: %.0fm", from, to, dist));
		
		Header.printFooter();
	}
	
	
	/**
	 * Construct an instance of the GTFS scenario and router. This is currently
	 * mainly set up to work for the MyCiTi scenario for the City of Cape Town. 
	 * @param network
	 * @param schedule
	 */
	public GtfsParser(String network, String schedule) {
		this.sc = parseSchedule(schedule, network);
		this.trConfig = new TransitRouterConfig(sc.getConfig());
		this.trRouter = new TransitRouterImpl(this.trConfig , sc.getTransitSchedule());
	}
	
	/**
	 * Parse all the necessary networks and transit schedules.
	 * @param scheduleFile
	 * @param networkFile
	 * @return
	 */
	private static Scenario parseSchedule(String scheduleFile, String networkFile){
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		new MatsimNetworkReader(sc.getNetwork()).readFile(networkFile);
		new TransitScheduleReader(sc).readFile(scheduleFile);
		return sc;
	}
	
	/**
	 * Calculates the shortest route's distance on the GTFS network between 
	 * two given stop IDs and at a specified time. 
	 * @param from
	 * @param to
	 * @param time
	 * @return
	 */
	public double findRouteDistance(int from, int to, double time){
		double dist = 0.0;
		
		Node fNode = sc.getNetwork().getNodes().get(Id.createNodeId("MyCiTi_" + from));
		Node tNode = sc.getNetwork().getNodes().get(Id.createNodeId("MyCiTi_" + to));
		
		if(fNode != null && tNode != null){
			FakeFacility fFrom = new FakeFacility(fNode.getCoord());
			FakeFacility fTo = new FakeFacility(tNode.getCoord());
			List<Leg> route = trRouter.calcRoute(fFrom, fTo, time, sc.getPopulation().getFactory().createPerson(Id.createPersonId("dummy")));
			
			for(Leg l : route){
				if(l.getRoute() != null){
					dist += l.getRoute().getDistance();
				}
			}
		} else{
			LOG.error("Either origin (" + from + ") or destination (" + to + ") node, or both, could not be found. Returning zero distance.");
		}
		return dist;
	}
	

}
