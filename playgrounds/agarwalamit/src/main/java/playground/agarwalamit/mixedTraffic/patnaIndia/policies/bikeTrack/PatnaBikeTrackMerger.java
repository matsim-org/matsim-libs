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

package playground.agarwalamit.mixedTraffic.patnaIndia.policies.bikeTrack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.utils.geometry.CoordUtils;
import playground.agarwalamit.mixedTraffic.patnaIndia.policies.PatnaTrafficRestrainer;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * Simply merge bike track to actual network with some connectors.
 *
 * @author amit
 */

public final class PatnaBikeTrackMerger {

	private static final List<Id<Link>> bikeLinks = new ArrayList<>();
	private static final boolean isMotorbikeAllowed = true;

	public static void main (String [] args){

		String networkFile = PatnaUtils.INPUT_FILES_DIR + "/simulationInputs/network/shpNetwork/network.xml.gz";
		Scenario scenario = LoadMyScenarios.loadScenarioFromNetwork(networkFile);
		Network network = scenario.getNetwork();
		{
			PatnaBikeTrackMerger pbtc = new PatnaBikeTrackMerger();
			pbtc.addBikeTrackOnly(network);
			new NetworkWriter(network).write(PatnaUtils.INPUT_FILES_DIR + "/simulationInputs/network/shpNetwork/networkWithBikeTrack.xml.gz");
		}

		if (isMotorbikeAllowed) {
			Set<String> modes = new HashSet<>();
			modes.add("bike");
			modes.add("motorbike");

			for (Id<Link> linkId : bikeLinks){
				Link l = network.getLinks().get(linkId);
				l.setAllowedModes(modes);
			}
			new NetworkWriter(network).write(PatnaUtils.INPUT_FILES_DIR + "/simulationInputs/network/shpNetwork/networkWithBikeMotorbikeTrack.xml.gz");
		}

		if (!isMotorbikeAllowed) {
			// now apply traffic restrain to this network
			PatnaTrafficRestrainer.run(network);
			new NetworkWriter(network).write(PatnaUtils.INPUT_FILES_DIR + "/simulationInputs/network/shpNetwork/networkWithTrafficRestricationAndBikeTrack.xml.gz");
		}
	}

	private void addBikeTrackOnly(final Network network){
		// is using with cluster, provide bike track network.
		String bikeTrack = PatnaUtils.INPUT_FILES_DIR + "/simulationInputs/network/shpNetwork/bikeTrack.xml.gz";
		Network bikeNetwork = LoadMyScenarios.loadScenarioFromNetwork(bikeTrack).getNetwork();

		// put everything to the original network
		for (Node n : bikeNetwork.getNodes().values()){
			// cant simply add the nodes because it also has information about in-/out- links from the node
			// adding the same in- out- link again will throw exception (because while adding a link, in-/out-links are added to to-/from-nodes.)

			Node nNew = network.getFactory().createNode(n.getId(), n.getCoord());
			network.addNode(nNew);
		}

		for (Link l: bikeNetwork.getLinks().values() ) {
			Node fromNode = network.getNodes().get(l.getFromNode().getId());
			Node toNode = network.getNodes().get(l.getToNode().getId());
			Link lNew = NetworkUtils.createAndAddLink(network, l.getId(), fromNode, toNode,
					l.getLength(), l.getFreespeed(), l.getCapacity(), l.getNumberOfLanes());
			lNew.setAllowedModes(l.getAllowedModes());
			bikeLinks.add(lNew.getId());
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

	private void createAndAddLink(Network network, Node [] nodes, String osmId) {
		int noOfLinks = network.getLinks().size();
		String id = osmId+"_link"+noOfLinks;
		double dist = CoordUtils.calcEuclideanDistance(nodes[0].getCoord(), nodes[1].getCoord());
		Set<String> modes = new HashSet<>();
		modes.add("bike");

		Id<Link> linkId = Id.createLinkId(id);
		Link l = network.getFactory().createLink(linkId, nodes[0], nodes[1]);
		l.setAllowedModes(modes );
		l.setLength(dist);
		l.setCapacity(900); // half of the one lane capacity
		l.setFreespeed(25/3.6); // lets keep this also lower than a normal car link
		l.setNumberOfLanes(1);
		network.addLink(l);
		bikeLinks.add(l.getId());
	}
}