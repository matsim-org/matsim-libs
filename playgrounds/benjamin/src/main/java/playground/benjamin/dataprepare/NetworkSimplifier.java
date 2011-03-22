/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkCleaner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.benjamin.dataprepare;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCalcTopoType;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

/**
 * Simplifies a given network, by merging links.
 *
 * @author benjamin after aneumann
 *
 */
public class NetworkSimplifier {

	private static final Logger log = Logger.getLogger(NetworkSimplifier.class);
	private boolean mergeLinkStats = false;
	private Set<Integer> nodeTopoToMerge = new TreeSet<Integer>();

	public void run(final Network network) {

		if(this.nodeTopoToMerge.size() == 0){
			Gbl.errorMsg("No types of node specified. Please use setNodesToMerge to specify which nodes should be merged");
		}

		log.info("running " + this.getClass().getName() + " algorithm...");

		log.info("  checking " + network.getNodes().size() + " nodes and " +
				network.getLinks().size() + " links for dead-ends...");

		NetworkCalcTopoType nodeTopo = new NetworkCalcTopoType();
		nodeTopo.run(network);

		for (Node node : network.getNodes().values()) {

			if(this.nodeTopoToMerge.contains(Integer.valueOf(nodeTopo.getTopoType(node)))){

				List<Link> iLinks = new ArrayList<Link> (node.getInLinks().values());

				for (Link iL : iLinks) {
					LinkImpl inLink = (LinkImpl) iL;

					List<Link> oLinks = new ArrayList<Link> (node.getOutLinks().values());

					for (Link oL : oLinks) {
						LinkImpl outLink = (LinkImpl) oL;

						if(inLink != null && outLink != null){
							if(!outLink.getToNode().equals(inLink.getFromNode())){

								if(this.mergeLinkStats){

									// Try to merge both links by guessing the resulting links attributes
									Link link = network.getFactory().createLink(
											new IdImpl(inLink.getId() + "-" + outLink.getId()),
											inLink.getFromNode().getId(),
											outLink.getToNode().getId());

									// length can be summed up
									link.setLength(inLink.getLength() + outLink.getLength());

									// freespeed depends on total length and time needed for inLink and outLink
									link.setFreespeed(
											(inLink.getLength() + outLink.getLength()) /
											(inLink.getFreespeedTravelTime() + outLink.getFreespeedTravelTime())
											);

									// the capacity and the new links end is important, thus it will be set to the minimum
									link.setCapacity(Math.min(inLink.getCapacity(), outLink.getCapacity()));

									// number of lanes can be derived from the storage capacity of both links
									link.setNumberOfLanes((inLink.getLength() * inLink.getNumberOfLanes()
													+ outLink.getLength() * outLink.getNumberOfLanes())
													/ (inLink.getLength() + outLink.getLength())
													);

//									inLink.getOrigId() + "-" + outLink.getOrigId(),
									network.addLink(link);
									network.removeLink(inLink.getId());
									(network).removeLink(outLink.getId());

								} else {

									// Only merge links with same attributes
									if(bothLinksHaveSameLinkStats(inLink, outLink)){
										LinkImpl newLink = ((NetworkImpl) network).createAndAddLink(
												new IdImpl(inLink.getId() + "-" + outLink.getId()),
												inLink.getFromNode(),
												outLink.getToNode(),
												inLink.getLength() + outLink.getLength(),
												inLink.getFreespeed(),
												inLink.getCapacity(),
												inLink.getNumberOfLanes(),
												inLink.getOrigId() + "-" + outLink.getOrigId(),
												null);

										newLink.setAllowedModes(inLink.getAllowedModes());

										network.removeLink(inLink.getId());
										network.removeLink(outLink.getId());
									}

								}
							}
						}
					}
				}
			}

		}

		org.matsim.core.network.algorithms.NetworkCleaner nc = new org.matsim.core.network.algorithms.NetworkCleaner();
		nc.run(network);

		nodeTopo = new NetworkCalcTopoType();
		nodeTopo.run(network);

		log.info("  resulting network contains " + network.getNodes().size() + " nodes and " +
				network.getLinks().size() + " links.");
		log.info("done.");
	}

	/**
	 * Specify the types of node which should be merged.
	 *
	 * @param nodeTypesToMerge A Set of integer indicating the node types as specified by {@link NetworkCalcTopoType}
	 * @see NetworkCalcTopoType NetworkCalcTopoType for a list of available classifications.
	 */
	public void setNodesToMerge(Set<Integer> nodeTypesToMerge){
		this.nodeTopoToMerge.addAll(nodeTypesToMerge);
	}

	/**
	 *
	 * @param mergeLinkStats If set true, links will be merged despite their different attributes.
	 *  If set false, only links with the same attributes will be merged, thus preserving as much information as possible.
	 *  Default is set false.
	 */
	public void setMergeLinkStats(boolean mergeLinkStats){
		this.mergeLinkStats = mergeLinkStats;
	}

	// helper

	/**
	 * Compare link attributes. Return whether they are the same or not.
	 */
	private boolean bothLinksHaveSameLinkStats(LinkImpl linkA, LinkImpl linkB){

		boolean bothLinksHaveSameLinkStats = true;

		if(!linkA.getAllowedModes().equals(linkB.getAllowedModes())){ bothLinksHaveSameLinkStats = false; }

		if(linkA.getFreespeed() != linkB.getFreespeed()){ bothLinksHaveSameLinkStats = false; }

		if(linkA.getCapacity() != linkB.getCapacity()){ bothLinksHaveSameLinkStats = false; }

		if(linkA.getNumberOfLanes() != linkB.getNumberOfLanes()){ bothLinksHaveSameLinkStats = false; }
		
//		if(linkA.getType() != linkB.getType()){ bothLinksHaveSameLinkStats = false; }

		return bothLinksHaveSameLinkStats;
	}

	public static void main(String[] args) {

		Set<Integer> nodeTypesToMerge = new TreeSet<Integer>();
		nodeTypesToMerge.add(new Integer(4));
		nodeTypesToMerge.add(new Integer(5));

		Scenario scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		final Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile("../../detailedEval/Net/network-86-85-87-84_withLanes.xml");

		NetworkSimplifier nsimply = new NetworkSimplifier();
		nsimply.setNodesToMerge(nodeTypesToMerge);
//		nsimply.setMergeLinkStats(true);
		nsimply.run(network);

		new NetworkWriter(network).write("../../detailedEval/Net/network-86-85-87-84_simplified---withLanes");

	}
}