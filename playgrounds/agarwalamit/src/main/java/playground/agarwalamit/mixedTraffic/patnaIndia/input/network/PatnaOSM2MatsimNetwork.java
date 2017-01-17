/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.mixedTraffic.patnaIndia.input.network;

import java.util.HashSet;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkSimplifier;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.run.NetworkCleaner;

import playground.agarwalamit.mixedTraffic.patnaIndia.utils.OuterCordonUtils.PatnaNetworkType;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
* @author amit
*/

public class PatnaOSM2MatsimNetwork {
	
	private static final String osmNetworkFile = PatnaUtils.INPUT_FILES_DIR + "/raw/network/osmNetworkFile.osm"; // this file has been already modified.
	private static final String out_osmNetworkFile = PatnaUtils.INPUT_FILES_DIR + "/simulationInputs/network/"+PatnaNetworkType.osmNetwork.toString()+"/network.xml.gz"; 
	
	public static void main(String[] args) {
		
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		OsmNetworkReader reader = new OsmNetworkReader(sc.getNetwork(), PatnaUtils.COORDINATE_TRANSFORMATION);
		
		reader.setHighwayDefaults(2, "trunk",         2,  80.0/3.6, 1.0, 1500); // it looks that each direction has 2 lanes
		reader.setHighwayDefaults(2, "primary",       2,  60.0/3.6, 1.0, 1500);
		
		reader.setHighwayDefaults(5, "tertiary",      1,  45.0/3.6, 1.0,  400);// reducing capacities
		reader.setHighwayDefaults(6, "minor",         1,  45.0/3.6, 1.0,  400);
		reader.setHighwayDefaults(6, "unclassified",  1,  45.0/3.6, 1.0,  300);
		reader.setHighwayDefaults(6, "residential",   1,  30.0/3.6, 1.0,  300);
		
		reader.parse(osmNetworkFile);
		
		NetworkSimplifier simplifier = new NetworkSimplifier();
		simplifier.run(sc.getNetwork());
		
		String uncleanedNetworkFile = PatnaUtils.INPUT_FILES_DIR +"/simulationInputs/network/"+PatnaNetworkType.osmNetwork.toString()+"/networkFromOSM.xml.gz";
		String cleanedNetworkFile = PatnaUtils.INPUT_FILES_DIR +"/simulationInputs/network/"+PatnaNetworkType.osmNetwork.toString()+"/networkFromOSM_cleaned.xml.gz";
		
		new NetworkWriter(sc.getNetwork()).write(uncleanedNetworkFile);
		
		new NetworkCleaner().run(uncleanedNetworkFile, cleanedNetworkFile );
		
		// add missing links
		Scenario scenario = LoadMyScenarios.loadScenarioFromNetwork(cleanedNetworkFile);
		Network network = scenario.getNetwork();
		
		{//Ganga setu links
			Node fromNode = createAndReturnNode(network, new Coord(85.195394, 25.6001837), "3009300239");
			Node toNode = createAndReturnNode(network, new Coord(85.2026634, 25.6134719), "gangaSetuNode_1");
			final Node fromNode1 = fromNode;
			final Node toNode1 = toNode;
			
			NetworkUtils.createAndAddLink(network,Id.createLinkId("gangaSetuLink_1"), fromNode1, toNode1, CoordUtils.calcEuclideanDistance(fromNode.getCoord(), toNode.getCoord()), 80/3.6, 3000.0, 2.0 );
			
			fromNode = createAndReturnNode(network, new Coord(85.2028137, 25.6134009), "gangaSetuNode_2");
			toNode = createAndReturnNode(network, new Coord(85.1938243, 25.5970135), "1133765005");
			final Node fromNode2 = fromNode;
			final Node toNode2 = toNode;
			
			NetworkUtils.createAndAddLink(network,Id.createLinkId("gangaSetuLink_2"), fromNode2, toNode2, CoordUtils.calcEuclideanDistance(fromNode.getCoord(), toNode.getCoord()), 80/3.6, 3000.0, 2.0 );
			
			{// demand from outer cordon ("OC4")
				Node additionalNode = createAndReturnNode(network,new Coord(85.204251,25.6155418), "OC4Node"); // close to river
				Node fNode = network.getNodes().get(Id.createNodeId("gangaSetuNode_1"));
				final Node fromNode3 = fNode;
				final Node toNode3 = additionalNode;
				NetworkUtils.createAndAddLink(network,Id.createLinkId("OC4_out"), fromNode3, toNode3, CoordUtils.calcEuclideanDistance(fNode.getCoord(), additionalNode.getCoord()), 80./3.6, 3000., (double) 2 );
				Node tNode = network.getNodes().get(Id.createNodeId("gangaSetuNode_2"));
				final Node fromNode4 = additionalNode;
				final Node toNode4 = tNode;
				NetworkUtils.createAndAddLink(network,Id.createLinkId("OC4_in"), fromNode4, toNode4, CoordUtils.calcEuclideanDistance(additionalNode.getCoord(), tNode.getCoord()), 80./3.6, 3000., (double) 2 );
			}
		}
		{// demand from outer cordon ("OC2") -- using existing node 315974339
			Node oc2NearestNode = network.getNodes().get(Id.createNodeId("315974339")); 
			Node oc2Node = createAndReturnNode(network,new Coord(85.27708,25.5479753), "OC2Node");
			final Node fromNode = oc2Node;
			final Node toNode = oc2NearestNode;
					
			NetworkUtils.createAndAddLink(network,Id.createLinkId("OC2_in"), fromNode, toNode, CoordUtils.calcEuclideanDistance(oc2Node.getCoord(), oc2NearestNode.getCoord()), 80./3.6, 1500., (double) 1 );
			final Node fromNode1 = oc2NearestNode;
			final Node toNode1 = oc2Node;
			NetworkUtils.createAndAddLink(network,Id.createLinkId("OC2_out"), fromNode1, toNode1, CoordUtils.calcEuclideanDistance(oc2NearestNode.getCoord(), oc2Node.getCoord()), 80./3.6, 1500., (double) 1 );
		}
		
		// allow all main modes on the links
		for (Link l : network.getLinks().values()) {
			l.setAllowedModes(new HashSet<>(PatnaUtils.ALL_MAIN_MODES));
		}
		
		//delete some links towards danapur, which are not present in the network from transcad data and heavily distort the nearby counting station.
		
		network.removeLink(Id.createLinkId("262-590"));
		network.removeLink(Id.createLinkId("589-261"));
		
		network.removeLink(Id.createLinkId("7316"));
		network.removeLink(Id.createLinkId("7315"));
		
		//finally write it again
		new NetworkWriter(scenario.getNetwork()).write(out_osmNetworkFile);
	}
	
	private static Node createAndReturnNode(Network network, Coord cord, String nodeID) {
		Id<Node> nodeId = Id.createNodeId(nodeID);
		//see if it exists 
		Node n = network.getNodes().get(nodeId); 
		if(n !=null) ; 
		else {
			n = network.getFactory().createNode(nodeId, PatnaUtils.COORDINATE_TRANSFORMATION.transform(cord));
			network.addNode(n);
		}
		return n;
	}

}	