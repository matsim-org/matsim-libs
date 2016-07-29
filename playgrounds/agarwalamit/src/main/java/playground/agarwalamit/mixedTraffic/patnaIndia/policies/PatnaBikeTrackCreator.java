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

package playground.agarwalamit.mixedTraffic.patnaIndia.policies;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * @author amit
 */

public final class PatnaBikeTrackCreator {

	public static void run(Network network){

//		PatnaBikeTrackGenerator bikeTrack = new PatnaBikeTrackGenerator();
//		bikeTrack.process();
		
		// is using with cluster, provide bike track network.
		String bikeTrack = PatnaUtils.INPUT_FILES_DIR + "/simulationInputs/network/shpNetwork/bikeTrack.xml.gz";
		if ( ! new File(bikeTrack).exists()) bikeTrack = "/net/ils4/agarwal/patnaIndia/run108/input/bikeTrack.xml.gz"; 
		
		Network bikeNetwork = LoadMyScenarios.loadScenarioFromNetwork(bikeTrack).getNetwork();

		// put everything to the original network
		
		for (Node n : bikeNetwork.getNodes().values()){
			// cant simply add the network because it also has information about in-/out- links from the node
			// adding the same in- out- link will throw exception (while adding a link, in-/out-links are added to to-/from-nodes.)
			Node nNew = network.getFactory().createNode(n.getId(), n.getCoord());
			network.addNode(nNew);
		}
		
		for (Link l: bikeNetwork.getLinks().values() ) {
			network.addLink(l);
		}
		
		// now connect track with the network.
		{// connection on the right (Ashok rajpath and NH 31 junction)
			Node n1 = network.getNodes().get(Id.createNodeId("256501411_node103"));
			Node n2 = network.getNodes().get(Id.createNodeId("16039"));
			createAndAddLink(network, new Node [] {n1,  n2}, "connector");
			createAndAddLink(network, new Node [] {n2,  n1}, "connector");
		}
		{// connection near Patna Sahib
			Node n1 = network.getNodes().get(Id.createNodeId("256501411_node87"));
			Node n2 = network.getNodes().get(Id.createNodeId("15869"));
			createAndAddLink(network, new Node [] {n1,  n2}, "connector");
			createAndAddLink(network, new Node [] {n2,  n1}, "connector");
		}
		{// connection near Ganga setu (but no connection with Ganga Setu)
			Node n1 = network.getNodes().get(Id.createNodeId("256501411_node59"));
			Node n2 = network.getNodes().get(Id.createNodeId("14222"));
			createAndAddLink(network, new Node [] {n1,  n2}, "connector");
			createAndAddLink(network, new Node [] {n2,  n1}, "connector");
		}
		{// connection near railway station
			Node n1 = network.getNodes().get(Id.createNodeId("316677246_node189"));
			Node n2 = network.getNodes().get(Id.createNodeId("10834"));
			createAndAddLink(network, new Node [] {n1,  n2}, "connector");
			createAndAddLink(network, new Node [] {n2,  n1}, "connector");
		}
		{// connection near railway station
			Node n1 = network.getNodes().get(Id.createNodeId("97953615_node20"));
			Node n2 = network.getNodes().get(Id.createNodeId("10597"));
			createAndAddLink(network, new Node [] {n1,  n2}, "connector");
			createAndAddLink(network, new Node [] {n2,  n1}, "connector");
		}
		{// connection near baily roads for institutional area
			Node n1 = network.getNodes().get(Id.createNodeId("315604580_node142"));
			Node n2 = network.getNodes().get(Id.createNodeId("6320"));
			createAndAddLink(network, new Node [] {n1,  n2}, "connector");
			createAndAddLink(network, new Node [] {n2,  n1}, "connector");
		}
		{// connection south west
			Node n1 = network.getNodes().get(Id.createNodeId("316677246_node166"));
			Node n2 = network.getNodes().get(Id.createNodeId("3856"));
			createAndAddLink(network, new Node [] {n1,  n2}, "connector");
			createAndAddLink(network, new Node [] {n2,  n1}, "connector");
		}
		{// connection north west
			Node n1 = network.getNodes().get(Id.createNodeId("395991040_node208"));
			Node n2 = network.getNodes().get(Id.createNodeId("8907"));
			createAndAddLink(network, new Node [] {n1,  n2}, "connector");
			createAndAddLink(network, new Node [] {n2,  n1}, "connector");
		}
	}
	private static void createAndAddLink(Network network, Node [] nodes, String osmId) {
		int noOfLinks = network.getLinks().size();
		String id = osmId+"_link"+noOfLinks;
		double dist = CoordUtils.calcEuclideanDistance(nodes[0].getCoord(), nodes[1].getCoord());
		Set<String> modes = new HashSet<>();
		modes.add("bike");
		modes.add("bike_ext");
		
		Id<Link> linkId = Id.createLinkId(id);
		Link l = network.getFactory().createLink(linkId, nodes[0], nodes[1]);
		l.setAllowedModes(modes );
		l.setLength(dist);
		l.setCapacity(900); // half of the one lane capacity
		l.setFreespeed(25/3.6); // lets keep this also lower than a normal car link
		l.setNumberOfLanes(1);
		network.addLink(l);
	}
}