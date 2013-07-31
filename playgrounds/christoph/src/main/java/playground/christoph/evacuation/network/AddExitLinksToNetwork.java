/* *********************************************************************** *
 * project: org.matsim.*
 * AddExitLinksToNetwork.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.opengis.kml._2.DocumentType;
import net.opengis.kml._2.KmlType;
import net.opengis.kml._2.ObjectFactory;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.facilities.OpeningTime.DayType;
import org.matsim.core.facilities.OpeningTimeImpl;
import org.matsim.core.network.KmlNetworkWriter;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.transformations.CH1903LV03toWGS84;
import org.matsim.vis.kml.KMZWriter;

import playground.christoph.evacuation.config.EvacuationConfig;

/*
 * Adapts a given Network to be used in an evacuation scenario.
 * 
 * Nodes that lie within a given area are connected to a new
 * rescue node by adding new rescue links. These links have a
 * capacity and free speed of Double.MAX_VALUE.
 * 
 * At the moment we use only a single exit node therefore each
 * exit link is connected to that node - we do not separate the
 * evacuated people into regions that are assigned to different
 * exit nodes.
 */
public class AddExitLinksToNetwork {

	private static final Logger log = Logger.getLogger(AddExitLinksToNetwork.class);
	
	private Scenario scenario;
	private Network network;
	
	private final Map<Id, Node> exitNodes;
	
	private double innerRadius = EvacuationConfig.innerRadius;
	private double outerRadius = EvacuationConfig.outerRadius;
	private Coord center;
	
	/*
	 * args[0] ... config file
	 * args[1] ... network file that will be written
	 */
	public static void main(String[] args) {
		if (args.length != 2) return;
		
		Config config = ConfigUtils.loadConfig(args[0]);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		AddExitLinksToNetwork addExitLinksToNetwork = new AddExitLinksToNetwork(scenario);
		addExitLinksToNetwork.identifyExitNodes();
		Map<Id, Node> exitNodes = addExitLinksToNetwork.getExitNodes(); 
		
		Network network = scenario.getNetwork();
		List<Node> nonExitNodes = new ArrayList<Node>();
		
		// identify non exit nodes
		for (Node node : network.getNodes().values()) {
			if (!exitNodes.containsKey(node.getId())) nonExitNodes.add(node);
		}
		
		// remove non exit nodes
		for (Node node : nonExitNodes) {
			network.removeNode(node.getId());
		}
		
//		// remove all remaining links
//		List<Link> links = new ArrayList<Link>(network.getLinks().values());
//		for (Link link : links) {
//			network.removeLink(link.getId());
//		}
		
		// write exit nodes to network file
		new NetworkWriter(network).write(args[1]);
		
		try {
			String kmzFile = args[1];
			if (kmzFile.toLowerCase().endsWith(".gz")) kmzFile = kmzFile.substring(0, kmzFile.length() - 3);
			if (kmzFile.toLowerCase().endsWith(".xml")) kmzFile = kmzFile.substring(0, kmzFile.length() - 4);
			kmzFile = kmzFile + ".kmz";
			
			ObjectFactory kmlObjectFactory = new ObjectFactory();
			KMZWriter kmzWriter = new KMZWriter(kmzFile);
		
			KmlType mainKml = kmlObjectFactory.createKmlType();
			DocumentType mainDoc = kmlObjectFactory.createDocumentType();
			mainKml.setAbstractFeatureGroup(kmlObjectFactory.createDocument(mainDoc));
			
			KmlNetworkWriter kmlNetworkWriter = new KmlNetworkWriter(network, new CH1903LV03toWGS84(), kmzWriter, mainDoc);
		
			mainDoc.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(kmlNetworkWriter.getNetworkFolder()));
			kmzWriter.writeMainKml(mainKml);
			kmzWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public AddExitLinksToNetwork(Scenario scenario) {
		this.scenario = scenario;
		this.network = this.scenario.getNetwork();
				
		this.center = EvacuationConfig.centerCoord;
		this.exitNodes = new TreeMap<Id, Node>();
		
		identifyExitNodes();
	}
	
	public Map<Id, Node> getExitNodes() {
		return Collections.unmodifiableMap(this.exitNodes);
	}
	
	private void identifyExitNodes() {		
		for (Node node : network.getNodes().values()) {
			double distance = CoordUtils.calcDistance(center, node.getCoord());
			
			if (distance >= innerRadius && distance <= outerRadius) exitNodes.put(node.getId(), node);
		}
		
		log.info("Found " + exitNodes.size() + " exit nodes.");
	}
	
	public void createExitLinks() {
		/*
		 * Use a coordinate which should be outside the evacuation area but still is feasible.
		 * Otherwise the AStarLandmarks algorithm could be confused and loose some performance.
		 */
//		Coord rescueCoord = scenario.createCoord(0.0, 0.0);
//		Coord rescueCoord = scenario.createCoord(EvacuationConfig.centerCoord.getX() + 50000.0, EvacuationConfig.centerCoord.getY() + 50000.0);
		Coord rescueCoord = EvacuationConfig.getRescueCoord();
		Node rescueNode = network.getFactory().createNode(scenario.createId("rescueNode"), rescueCoord);
		network.addNode(rescueNode);
		
		int counter = 0;
		for (Node node : exitNodes.values()) {
			counter++;
			Link rescueLink = network.getFactory().createLink(scenario.createId("rescueLink" + counter), node, rescueNode);
			rescueLink.setLength(10);	// use short links for non-vehicular traffic
			rescueLink.setCapacity(1000000);
			rescueLink.setFreespeed(1000000);			
//			rescueLink.setCapacity(Double.MAX_VALUE);
//			rescueLink.setFreespeed(Double.MAX_VALUE);
			Set<String> allowedTransportModes = new HashSet<String>();
			allowedTransportModes.add(TransportMode.bike);
			allowedTransportModes.add(TransportMode.car);
			allowedTransportModes.add(TransportMode.pt);
			allowedTransportModes.add(TransportMode.ride);
			allowedTransportModes.add(TransportMode.walk);
			allowedTransportModes.add("evacuation");
			rescueLink.setAllowedModes(allowedTransportModes);
			network.addLink(rescueLink);
		}
		
		log.info("Created " + counter + " exit links.");
		
		/*
		 * Now we create a second rescue node that is connected only to the
		 * first rescue node. The link between them gets equipped with the
		 * rescue facility that is the destination of the evacuated persons.
		 */
//		Coord rescueCoord2 = scenario.createCoord(1.0, 1.0);
//		Coord rescueCoord2 = scenario.createCoord(EvacuationConfig.centerCoord.getX() + 50001.0, EvacuationConfig.centerCoord.getY() + 50001.0);
		Coord rescueCoord2 = scenario.createCoord(rescueCoord.getX() + 1.0, rescueCoord.getY() + 1.0);
		Node rescueNode2 = network.getFactory().createNode(scenario.createId("rescueNode2"), rescueCoord2);
		network.addNode(rescueNode2);
		
		Link rescueLink = network.getFactory().createLink(scenario.createId("rescueLink"), rescueNode, rescueNode2);
//		rescueLink.setLength(100000);
//		rescueLink.setCapacity(Double.MAX_VALUE);
//		rescueLink.setFreespeed(Double.MAX_VALUE);
		rescueLink.setLength(10);	// use short links for non-vehicular traffic
		rescueLink.setCapacity(1000000);
		rescueLink.setFreespeed(1000000);
		Set<String> allowedTransportModes = new HashSet<String>();
		allowedTransportModes.add(TransportMode.bike);
		allowedTransportModes.add(TransportMode.car);
		allowedTransportModes.add(TransportMode.pt);
		allowedTransportModes.add(TransportMode.ride);
		allowedTransportModes.add(TransportMode.walk);
		allowedTransportModes.add("evacuation");
		rescueLink.setAllowedModes(allowedTransportModes);
		network.addLink(rescueLink);
		
		/*
		 * Create and add the rescue facility and an activity option ("rescue")
		 */
		ActivityFacility rescueFacility = scenario.getActivityFacilities().getFactory().createActivityFacility(scenario.createId("rescueFacility"), rescueLink.getCoord());
		scenario.getActivityFacilities().addActivityFacility(rescueFacility);
		((ActivityFacilityImpl)rescueFacility).setLinkId(((LinkImpl)rescueLink).getId());
		
		ActivityOption activityOption = ((ActivityFacilityImpl)rescueFacility).createActivityOption("rescue");
//		activityOption.addOpeningTime(new OpeningTimeImpl(0*3600, 24*3600));
		OpeningTimeImpl openingTime = new OpeningTimeImpl(0*3600, 24*3600);
		openingTime.setDay(DayType.wk);
		activityOption.addOpeningTime(openingTime);
		activityOption.setCapacity(Double.MAX_VALUE);
	}
}