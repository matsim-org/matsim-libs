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
package playground.johannes.gsv.visum;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.visum.VisumNetwork;
import org.matsim.visum.VisumNetwork.LineRouteItem;
import org.matsim.visum.VisumNetworkReader;

import java.util.ArrayList;
import java.util.List;

/**
 * @author johannes
 *
 */
public class ScheduleNetworkMatcher {
	
	private static final Logger logger = Logger.getLogger(ScheduleNetworkMatcher.class);

	public static void main(String args[]) {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		config.transit().setUseTransit(true);
		
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario);
		netReader.readFile("/home/johannes/gsv/matsim/studies/netz2030/data/network.rail.xml");
		
		
		TransitScheduleReader reader = new TransitScheduleReader(scenario);
		reader.readFile("/home/johannes/gsv/matsim/studies/netz2030/data/transitSchedule.tmp.xml");
		
		
		VisumNetwork visNetwork = new VisumNetwork();
		VisumNetworkReader visReader = new VisumNetworkReader(visNetwork);
		visReader.read("/home/johannes/gsv/matsim/studies/netz2030/data/raw/network.net");
		
		Network network = scenario.getNetwork();
		TransitSchedule schedule = scenario.getTransitSchedule();
		
		IdGenerator idGenerator = new PrefixIdGenerator("rail.");
		
		logger.info("Matching routes...");
		for(TransitLine line : schedule.getTransitLines().values()) {
			for(TransitRoute route : line.getRoutes().values()) {
			
				String idStr = route.getId().toString();
//				String dcode = idStr.substring(idStr.length() - 1, idStr.length());
				String tokens[] = route.getId().toString().split("\\.");
				int idx = 1;
				boolean finish = false;
				List<LineRouteItem> items = new ArrayList<LineRouteItem>(100);
				while(!finish) {
					
					String id = line.getId().toString() + "/" + tokens[1] + "/" + idx + "/" + tokens[3];
					LineRouteItem lri = visNetwork.lineRouteItems.get(id);
					
					if(lri == null) {
						finish = true;
					} else {
						items.add(lri);
					}
					
					idx++;
				}
				
				List<Id<Link>> linkIds = new ArrayList<Id<Link>>(items.size());
				
				for(int i = 1; i < items.size(); i++) {
					Node from = network.getNodes().get(idGenerator.generateId(items.get(i-1).nodeId.toString(), Node.class));
					Node to = network.getNodes().get(idGenerator.generateId(items.get(i).nodeId.toString(), Node.class));
					
					Link link = NetworkUtils.getConnectingLink(from, to);
					if(link == null)
						throw new RuntimeException("Link not found.");
					linkIds.add(link.getId());
				}
				
				NetworkRoute netroute = RouteUtils.createNetworkRoute(linkIds, network);
				route.setRoute(netroute);
			}
		}
		
		TransitScheduleWriter writer = new TransitScheduleWriter(schedule);
		writer.writeFile("/home/johannes/gsv/matsim/studies/netz2030/data/transitSchedule.matched.xml");
	}
}
