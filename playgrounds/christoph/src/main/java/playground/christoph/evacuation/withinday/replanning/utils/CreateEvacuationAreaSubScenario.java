/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationControlerListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.withinday.replanning.utils;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.vividsolutions.jts.geom.Geometry;

import playground.christoph.evacuation.analysis.CoordAnalyzer;
import playground.christoph.evacuation.config.EvacuationConfig;

public class CreateEvacuationAreaSubScenario {

	private static final Logger log = Logger.getLogger(CreateEvacuationAreaSubScenario.class);
	
	private final Scenario scenario;
	private final CoordAnalyzer coordAnalyzer;
	private final Geometry affectedArea;
	private final Set<String> modes;
	
	public CreateEvacuationAreaSubScenario(Scenario scenario, CoordAnalyzer coordAnalyzer, Geometry affectedArea, Set<String> modes) {
		this.scenario = scenario;
		this.coordAnalyzer = coordAnalyzer;
		this.affectedArea = affectedArea;
		this.modes = modes;
	}
	
	public Scenario createSubScenario() {
		
		/*
		 * Create a subnetwork that only contains the Evacuation area plus some exit nodes.
		 * This network is used to calculate estimated evacuation times starting from the 
		 * home locations which are located inside the evacuation zone.
		 */
		Network subNetwork = NetworkImpl.createNetwork();
		((NetworkImpl) subNetwork).setName("Evacuation Area Sub-Network with Exit Links");
		TransportModeNetworkFilter networkFilter = new TransportModeNetworkFilter(scenario.getNetwork());
		networkFilter.filter(subNetwork, modes);
		
		// use a ScenarioWrapper that returns the sub-network instead of the network
		Scenario subScenario = new ScenarioWrapper(scenario, subNetwork);
		
		/*
		 * Identify affected nodes.
		 */
		Set<Id> affectedNodes = new HashSet<Id>();
		for (Node node : subNetwork.getNodes().values()) {
			if (coordAnalyzer.isNodeAffected(node)) affectedNodes.add(node.getId());
		}
		log.info("Found " + affectedNodes.size() + " nodes inside affected area.");
		
		/*
		 * Identify buffered affected nodes.
		 */
		CoordAnalyzer bufferedCoordAnalyzer = this.defineBufferedArea();
		Set<Id> bufferedAffectedNodes = new HashSet<Id>();
		for (Node node : subNetwork.getNodes().values()) {
			if (bufferedCoordAnalyzer.isNodeAffected(node) && !affectedNodes.contains(node.getId())) {
				bufferedAffectedNodes.add(node.getId());
			}
		}
		log.info("Found " + bufferedAffectedNodes.size() + " additional nodes inside buffered affected area.");
		
		/*
		 * Identify link that cross the evacuation line and their start and
		 * end nodes which are located right after the evacuation line.
		 */
		Set<Id> crossEvacuationLineNodes = new HashSet<Id>(bufferedAffectedNodes);
		Set<Id> crossEvacuationLineLinks = new HashSet<Id>();
		for (Link link : subNetwork.getLinks().values()) {
			boolean fromNodeInside = affectedNodes.contains(link.getFromNode().getId());
			boolean toNodeInside = affectedNodes.contains(link.getToNode().getId());
			
			if (fromNodeInside && !toNodeInside) {
				crossEvacuationLineLinks.add(link.getId());
				crossEvacuationLineNodes.add(link.getToNode().getId());
			} else if (!fromNodeInside && toNodeInside) {
				crossEvacuationLineLinks.add(link.getId());
				crossEvacuationLineNodes.add(link.getFromNode().getId());
			}
		}
		log.info("Found " + crossEvacuationLineLinks.size() + " links crossing the evacuation boarder.");
		log.info("Found " + crossEvacuationLineNodes.size() + " nodes outside the evacuation boarder.");
		
		/*
		 * Remove links and nodes.
		 */
		Set<Id> nodesToRemove = new HashSet<Id>();
		for (Node node : subNetwork.getNodes().values()) {
			if (!crossEvacuationLineNodes.contains(node.getId()) && !affectedNodes.contains(node.getId())) {
				nodesToRemove.add(node.getId());
			}
		}
		for (Id id : nodesToRemove) subNetwork.removeNode(id);	
		log.info("Remaining nodes " + subNetwork.getNodes().size());
		log.info("Remaining links " + subNetwork.getLinks().size());
		
		Set<String> transportModes = new HashSet<String>();
		transportModes.add(TransportMode.bike);
		transportModes.add(TransportMode.car);
		transportModes.add(TransportMode.pt);
		transportModes.add(TransportMode.walk);
		
		NetworkFactory networkFactory = subNetwork.getFactory();
		Coord exitNode1Coord = subScenario.createCoord(EvacuationConfig.centerCoord.getX() + 50000.0, EvacuationConfig.centerCoord.getY() + 50000.0); 
		Coord exitNode2Coord = subScenario.createCoord(EvacuationConfig.centerCoord.getX() + 50001.0, EvacuationConfig.centerCoord.getY() + 50001.0);
		Node exitNode1 = networkFactory.createNode(subScenario.createId("exitNode1"), exitNode1Coord);
		Node exitNode2 = networkFactory.createNode(subScenario.createId("exitNode2"), exitNode2Coord);
		Link exitLink = networkFactory.createLink(subScenario.createId("exitLink"), exitNode1, exitNode2);
		exitLink.setAllowedModes(transportModes);
		exitLink.setLength(1.0);
		subNetwork.addNode(exitNode1);
		subNetwork.addNode(exitNode2);
		subNetwork.addLink(exitLink);
		
		/*
		 * Create exit links for links that cross the evacuation line.
		 */
		int i = 0;
		for (Id id : crossEvacuationLineNodes) {
			Node node = subNetwork.getNodes().get(id);
			Link link = networkFactory.createLink(subScenario.createId("exitLink" + i), node, exitNode1);
			link.setAllowedModes(transportModes);
			link.setLength(1.0);
			subNetwork.addLink(link);
			i++;
		}
	
		return subScenario;
	}
	
	/*
	 * Identify facilities that are located inside the affected area. They
	 * might be attached to links which are NOT affected. However, those links
	 * still have to be included in the sub network.
	 * The links themselves are secure, therefore we can directly connect them
	 * to the exit node.
	 */
	private CoordAnalyzer defineBufferedArea() {
	
		double buffer = 0.0;
		double dBuffer = 50.0;	// buffer increase per iteration
		
		/*
		 * Identify not affected links where affected facilities are attached.
		 */
		Set<Link> links = new HashSet<Link>();
		for (ActivityFacility facility : ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().values()) {
			if (this.coordAnalyzer.isFacilityAffected(facility)) {
				Id linkId = facility.getLinkId();
				Link link = scenario.getNetwork().getLinks().get(linkId);
				
				if (!this.coordAnalyzer.isLinkAffected(link)) {
					links.add(link);
				}
			}
		}
		
		/*
		 * Increase the buffer until all links are included in the geometry.
		 */
		if (links.size() == 0) return this.coordAnalyzer;
		else {
			while (true) {
				buffer += dBuffer;
				Geometry geometry = this.affectedArea.buffer(buffer);
				
				CoordAnalyzer bufferedCoordAnalyzer = new CoordAnalyzer(geometry);
				
				boolean increaseBuffer = false;
				for (Link link : links) {
					/*
					 * If the link and/or its from/to nodes is not affected, 
					 * the buffer has to be increased.
					 */
					if (!bufferedCoordAnalyzer.isLinkAffected(link) ||
							!bufferedCoordAnalyzer.isNodeAffected(link.getFromNode()) ||
							!bufferedCoordAnalyzer.isNodeAffected(link.getToNode())) {
						increaseBuffer = true;
						break;
					}
				}
				
				if (!increaseBuffer) {
					log.info("A buffer of  " + buffer + " was required to catch all links where affected" +
							"facilities are attached to.");
					return bufferedCoordAnalyzer;
				}
			}
		}		
	}
	
	/*
	 * Returns a sub-network instead of the full network. This is needed for the evacuation routing
	 * (find the fastest route to leave the evacuation area).
	 */
	private static class ScenarioWrapper implements Scenario {

		private final Scenario scenario;
		private final Network network;
		
		public ScenarioWrapper(Scenario scenario, Network network) {
			this.scenario = scenario;
			this.network = network;
		}
		
		@Override
		public Id createId(String id) {
			return this.scenario.createId(id);
		}

		@Override
		public Network getNetwork() {
			return this.network;
		}

		@Override
		public Population getPopulation() {
			return this.scenario.getPopulation();
		}

		@Override
		public TransitSchedule getTransitSchedule() {
			return this.scenario.getTransitSchedule();
		}
		
		@Override
		public ActivityFacilities getActivityFacilities() {
			return this.scenario.getActivityFacilities();
		}

		@Override
		public Config getConfig() {
			return this.scenario.getConfig();
		}

		@Override
		public Coord createCoord(double x, double y) {
			return this.scenario.createCoord(x, y);
		}

		@Override
		public void addScenarioElement(Object o) {
			this.scenario.addScenarioElement(o);
		}

		@Override
		public boolean removeScenarioElement(Object o) {
			return this.scenario.removeScenarioElement(o);
		}

		@Override
		public <T> T getScenarioElement(Class<? extends T> klass) {
			return this.scenario.getScenarioElement(klass);
		}
	}
}
