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
import java.util.Map;
import java.util.TreeMap;

import net.opengis.kml._2.DocumentType;
import net.opengis.kml._2.KmlType;
import net.opengis.kml._2.ObjectFactory;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.facilities.OpeningTime;
import org.matsim.core.facilities.OpeningTimeImpl;
import org.matsim.core.network.KmlNetworkWriter;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.transformations.CH1903LV03toWGS84;
import org.matsim.vis.kml.KMZWriter;

import playground.christoph.evacuation.config.EvacuationConfig;

/*
 * Adapts a given Network to be used in an evacuation scenario.
 * 
 * To every Link that lies in a secure area a new Facility is
 * added. Agents that perform a Leg on such a Link can be
 * evacuated to that Facility. 
 */
public class AddSecureFacilitiesToNetwork {

	private static final Logger log = Logger.getLogger(AddSecureFacilitiesToNetwork.class);
	
	private Scenario scenario;
	private Network network;
	
	private double innerRadius = EvacuationConfig.innerRadius;
	private Coord center;
	
	/*
	 * args[0] ... config file
	 * args[1] ... network file that will be written
	 */
	public static void main(String[] args) {
		if (args.length != 2) return;
		
		ScenarioLoaderImpl sl = ScenarioLoaderImpl.createScenarioLoaderImplAndResetRandomSeed(args[0]);
		Scenario scenario = sl.loadScenario();
		
		AddSecureFacilitiesToNetwork addSecureFacilitiesToNetwork = new AddSecureFacilitiesToNetwork(scenario);
		Map<Id, Node> secureNodes = addSecureFacilitiesToNetwork.getSecureNodes();
		
		Network network = scenario.getNetwork();
		
		// remove secure nodes
		for (Node node : secureNodes.values()) {
			network.removeNode(node.getId());
		}
		
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
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public AddSecureFacilitiesToNetwork(Scenario scenario) {
		this.scenario = scenario;
		this.network = this.scenario.getNetwork();
		this.center = EvacuationConfig.centerCoord;
	}
	
	private Map<Id, Node> getSecureNodes() {
		Map<Id, Node> secureNodes = new TreeMap<Id, Node>();
		
		for (Node node : network.getNodes().values()) {
			double distance = CoordUtils.calcDistance(center, node.getCoord());
			
			if (distance >= innerRadius) secureNodes.put(node.getId(), node);
		}
		
		log.info("Found " + secureNodes.size() + " secure nodes.");
		
		return secureNodes;
	}
	
	private Map<Id, Link> getSecureLinks() {
		Map<Id, Node> secureNodes = getSecureNodes();
		Map<Id, Link> secureLinks = new TreeMap<Id, Link>();
		
		for (Link link : network.getLinks().values())
		{		
			if (secureNodes.containsKey(link.getFromNode().getId()) && secureNodes.containsKey(link.getToNode().getId())) secureLinks.put(link.getId(), link);
		}
		
		log.info("Found " + secureLinks.size() + " secure links.");
		
		return secureLinks;
	}
	
	public void createSecureFacilities() {
		Map<Id, Link> secureLinks = getSecureLinks();
		
		for (Link link : secureLinks.values()) {
			/*
			 * Create and add the rescue facility and add activity option ("rescue")
			 */
			String idString = "secureFacility" + link.getId();
			ActivityFacility secureFacility = ((ScenarioImpl)scenario).getActivityFacilities().createFacility(scenario.createId(idString), link.getCoord());
			((ActivityFacilityImpl)secureFacility).setLinkId(((LinkImpl)link).getId());
			
			ActivityOption activityOption = ((ActivityFacilityImpl)secureFacility).createActivityOption("rescue");
			activityOption.addOpeningTime(new OpeningTimeImpl(OpeningTime.DayType.wk, 0*3600, 24*3600));
			activityOption.setCapacity(Double.MAX_VALUE);
		}
	}
}
