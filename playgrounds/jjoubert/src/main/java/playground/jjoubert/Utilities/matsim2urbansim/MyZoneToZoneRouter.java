/* *********************************************************************** *
 * project: org.matsim.*
 * MyZoneToZoneRouter.java
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

package playground.jjoubert.Utilities.matsim2urbansim;

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

public class MyZoneToZoneRouter {
	private final Logger log = Logger.getLogger(MyZoneToZoneRouter.class);
	private final Scenario scenario;
	private final List<MyZone> zones;
	private PreProcessDijkstra pp;
	private LeastCostPathCalculator cc;
	
	public MyZoneToZoneRouter(final Scenario scenario, final List<MyZone> zones) {
		// TODO Auto-generated constructor stub
		this.scenario = scenario;
		this.zones = zones;
		
		log.info("Preprocessing the network for zone-to-zone travel time calculation.");
		pp = new PreProcessDijkstra();
		pp.run(this.scenario.getNetwork());
		DijkstraFactory df = new DijkstraFactory(pp);
//		TravelCost tcc = 
//		TravelTime ttc = new TravelTimeCalculator(scenario.getNetwork(), scenario.getConfig().travelTimeCalculator());
//		this.cc = df.createPathCalculator(this.scenario.getNetwork(), travelCosts, travelTimes);
		
		
		scenario.getNetwork().getLinks().get("123");
//		TravelTimeData ttd = new TravelTimeDataArray();		
	}
	
	public void readLinkStats(String filename){
		// TODO Maybe rather use the MyLinkStatsReader class.
		
		
	}
	
	private void prepareTravelTimeData(String filename, String hour){
		MyLinkStatsReader mlsr = new MyLinkStatsReader(filename, hour);
//		mlsr.buildTravelTimeDataObject();
		
	}
	
	private void findRoute(Node fromNode, Node toNode){
//		TravelTimeData ttd;
	}
	
}
