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

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.johannes.gsv.visum.IdGenerator;
import playground.johannes.gsv.visum.PrefixIdGenerator;

/**
 * @author johannes
 *
 */
public class LinkStopFacilities {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		config.transit().setUseTransit(true);
		
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario);
		netReader.readFile("/home/johannes/gsv/matsim/studies/netz2030/data/network.gk3.xml");
		Network network = scenario.getNetwork();
		
		TransitScheduleReader tReader = new TransitScheduleReader(scenario);
		tReader.readFile("/home/johannes/gsv/matsim/studies/netz2030/data/transitSchedule.routed.gk3.xml");
		TransitSchedule schedule = scenario.getTransitSchedule();
		
		IdGenerator idGenerator = new PrefixIdGenerator("rail.");
		
		Set<String> modes = new HashSet<String>();
		modes.add("pt");
		
		for(TransitStopFacility facility : schedule.getFacilities().values()) {
			Node node = network.getNodes().get(idGenerator.generateId(facility.getId().toString(), Node.class));
			
			if(node == null)
				throw new RuntimeException("Node not found.");
			
			Id<Link> id = Id.create("stop." + facility.getId().toString(), Link.class);
			Link link = network.getLinks().get(id);
			
			if(link == null) {
				link = network.getFactory().createLink(id, node, node);
				
				link.setAllowedModes(modes);
				link.setCapacity(Double.MAX_VALUE);
				link.setFreespeed(0);
				link.setLength(Double.MAX_VALUE);
				link.setNumberOfLanes(0);
				
				network.addLink(link);
			}
			
			facility.setLinkId(link.getId());
		}

		NetworkWriter writer = new NetworkWriter(network);
		writer.write("/home/johannes/gsv/matsim/studies/netz2030/data/network.gk3.xml");
		
		TransitScheduleWriter tWriter = new TransitScheduleWriter(schedule);
		tWriter.writeFile("/home/johannes/gsv/matsim/studies/netz2030/data/transitSchedule.routed.gk3.xml");
	}

}
