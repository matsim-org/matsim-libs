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

package org.matsim.core.network.algorithms;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Simplifies a given network, by merging links. All other criteria met, no 
 * link should have a length less than a given threshold. This class is based
 * on {@link NetworkSimplifier}. It goes through the network in two steps. 
 * First it merges links if <i>both</i> are shorter then the threshold. It then
 * goes through the network a <i>second</i> time and merges links that are
 * shorter than the threshold with either of the links' mergeable neighbours.<br><br>
 * 
 * If no link threshold is given, an infinite threshold is assumed. This should
 * behave the same as a 'clean' network.
 *
 * @author aneumann, jwjoubert
 *
 */
public final class NetworkSimplifier {

	private static final Logger log = Logger.getLogger(NetworkSimplifier.class);
	private boolean mergeLinksWithDifferentAttributes = false;
	private Collection<Integer> nodeTopoToMerge = Arrays.asList( NetworkCalcTopoType.PASS1WAY , NetworkCalcTopoType.PASS2WAY );

	private Set<Id<Node>> nodesNotToMerge = new HashSet<Id<Node>>();

	private final Map<Id<Link>,List<Node>> mergedLinksToIntermediateNodes = new HashMap<>();


	/**
	 * Merges all qualifying links, ignoring length threshold.
	 * @param network
	 */
	public void run(final Network network){
		run(network, Double.POSITIVE_INFINITY, thresholdExceeded.EITHER);
	}
	
	
	/**
	 * Merges all qualifying links while ensuring no link is shorter than the
	 * given threshold.
	 * <br/>
	 * Comments:<ul>
	 *     <li>I would argue against setting the thresholdLength to anything different from POSITIVE_INFINITY, since
	 *     the result of the method depends on the sequence in which the algorithm goes through the nodes.  </li>
	 * </ul>
	 * @param network
	 * @param thresholdLength
	 */
	@Deprecated
	public void run(final Network network, double thresholdLength){
		run(network, thresholdLength, thresholdExceeded.BOTH);
		run(network, thresholdLength, thresholdExceeded.EITHER);
	}

	/**
	 * Specifies a set of nodes of which all outgoing and ingoing links should not be merged.
	 * Should probably not be used if nodes of type {@link NetworkCalcTopoType.INTERSECTION} are to be merged.
	 * tschlenther jun'17
	 * @param nodeIDs
	 */
	public void setNodesNotToMerge(Set<Long> nodeIDs){
		for(Long l : nodeIDs){
			this.nodesNotToMerge.add(Id.createNodeId(l));
		}
	}
	
	private void run(final Network network, double thresholdLength, thresholdExceeded type) {

		if(this.nodeTopoToMerge.size() == 0){
			throw new RuntimeException("No types of node specified. Please use setNodesToMerge to specify which nodes should be merged");
		}

		log.info("running " + this.getClass().getName() + " algorithm...");

		NetworkCalcTopoType nodeTopo = new NetworkCalcTopoType();
		nodeTopo.run(network);

		for (Node node : network.getNodes().values()) {
			
			if(this.nodeTopoToMerge.contains(Integer.valueOf(nodeTopo.getTopoType(node))) && (!this.nodesNotToMerge.contains(node.getId())) ){

				List<Link> iLinks = new ArrayList<Link> (node.getInLinks().values());

				for (Link iL : iLinks) {
					Link inLink = (Link) iL;

					List<Link> oLinks = new ArrayList<Link> (node.getOutLinks().values());

					for (Link oL : oLinks) {
						Link outLink = (Link) oL;

						if(inLink != null && outLink != null){
//							if(!outLink.getToNode().equals(inLink.getFromNode())){
							if(  areLinksMergeable(inLink, outLink) ){
								if(this.mergeLinksWithDifferentAttributes){

									// Only merge if threshold criteria is met.  
									boolean criteria = false;
									switch (type) {
									case BOTH:
										criteria = bothLinksAreShorterThanThreshold(inLink, outLink, thresholdLength);
										break;
									case EITHER:
										criteria = eitherLinkIsShorterThanThreshold(inLink, outLink, thresholdLength);
										break;
									default:
										break;
									}
									
									// yyyy The approach here depends on the sequence in which this goes through the nodes:
									// * in the "EITHER" situation, a long link may gobble up short neighboring links
									// until it hits another long link doing the same.
									// * In the "BOTH" situation, something like going through nodes randomly will often merge
									// the neighboring links, while going through the nodes along some path will mean that it will
									// gobble up until the threshold is met.
									// I would strongly advise against setting thresholdLength to anything other than POSITIVE_INFINITY.
									// kai, feb'18
									
									if(criteria){
										// Try to merge both links by guessing the resulting links attributes
										Link link = network.getFactory().createLink(
												Id.create(inLink.getId() + "-" + outLink.getId(), Link.class),
												inLink.getFromNode(),
												outLink.getToNode());
										
										// length can be summed up
										link.setLength(inLink.getLength() + outLink.getLength());
										
										// freespeed depends on total length and time needed for inLink and outLink
										link.setFreespeed(
												(inLink.getLength() + outLink.getLength()) /
												(NetworkUtils.getFreespeedTravelTime(inLink) + NetworkUtils.getFreespeedTravelTime(outLink))
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
										network.removeLink(outLink.getId());
										collectMergedLinkNodeInfo(inLink, outLink, link.getId());
									}
								} else {

									// Only merge links with same attributes
									if(bothLinksHaveSameLinkStats(inLink, outLink)){
										
										// Only merge if threshold criteria is met.  
										boolean isHavingShortLinks = false;
										switch (type) {
										case BOTH:
											isHavingShortLinks = bothLinksAreShorterThanThreshold(inLink, outLink, thresholdLength);
											break;
										case EITHER:
											isHavingShortLinks = eitherLinkIsShorterThanThreshold(inLink, outLink, thresholdLength);
											break;
										default:
											break;
										}
																				
										if(isHavingShortLinks){
											Link newLink = NetworkUtils.createAndAddLink(((Network) network),Id.create(inLink.getId() + "-" + outLink.getId(), Link.class), inLink.getFromNode(), outLink.getToNode(), inLink.getLength() + outLink.getLength(), inLink.getFreespeed(), inLink.getCapacity(), inLink.getNumberOfLanes(), NetworkUtils.getOrigId( inLink ) + "-" + NetworkUtils.getOrigId( outLink ), null);
											
											newLink.setAllowedModes(inLink.getAllowedModes());
											
											network.removeLink(inLink.getId());
											network.removeLink(outLink.getId());
											collectMergedLinkNodeInfo(inLink, outLink, newLink.getId());
										}
									}
								}
							}
						}
					}
				}
			}
		}

		log.info("  resulting network contains " + network.getNodes().size() + " nodes and " +
				network.getLinks().size() + " links.");
		log.info("done.");

		// writes stats as a side effect
		nodeTopo = new NetworkCalcTopoType();
		nodeTopo.run(network);
	}

	private boolean areLinksMergeable(Link inLink, Link outLink) {
		List<Node> fromNodes = this.mergedLinksToIntermediateNodes.get(inLink.getId());
		if (fromNodes==null) fromNodes = new ArrayList<>();
		fromNodes.add(inLink.getFromNode());

		List<Node> toNodes = this.mergedLinksToIntermediateNodes.get(outLink.getId());
		if(toNodes==null) toNodes = new ArrayList<>();
		toNodes.add(outLink.getToNode());

		for(Node n :fromNodes) {
			if (toNodes.contains(n)) return false;
		}
		return true;
	}

	private void collectMergedLinkNodeInfo(Link inLink, Link outLink, Id<Link> mergedLinkId) {
		List<Node> nodes = new ArrayList<>();
		if (this.mergedLinksToIntermediateNodes.containsKey(inLink.getId())) nodes.addAll(this.mergedLinksToIntermediateNodes
				.remove(inLink.getId()));
		if (this.mergedLinksToIntermediateNodes.containsKey(outLink.getId())) nodes.addAll(this.mergedLinksToIntermediateNodes
				.remove(outLink.getId()));
		nodes.add(inLink.getToNode());

		this.mergedLinksToIntermediateNodes.put(mergedLinkId, nodes);
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
	 * @param mergeLinksWithDifferentAttributes If set true, links will be merged despite their different attributes.
	 *  If set false, only links with the same attributes will be merged, thus preserving as much information as possible.
	 *  Default is set false.
	 */
	public void setMergeLinkStats(boolean mergeLinksWithDifferentAttributes){
		this.mergeLinksWithDifferentAttributes = mergeLinksWithDifferentAttributes;
	}

	// helper

	/**
	 * Quick check to see whether <i>both</i> the links are shorter than the 
	 * given threshold.
	 * @param linkA
	 * @param linkB
	 * @param thresholdLength
	 * @return true if <i>both</i> links are shorter than the given threshold, 
	 * false otherwise. 
	 */
	private boolean bothLinksAreShorterThanThreshold(Link linkA, Link linkB, double thresholdLength){
		boolean hasTwoShortLinks = false;
		if(linkA.getLength() < thresholdLength && linkB.getLength() < thresholdLength){
			hasTwoShortLinks = true;
		}
		return hasTwoShortLinks;
	}
	
	/**
	 * Quick check to see whether <i>either</i> the links are shorter than the 
	 * given threshold.
	 * @param linkA
	 * @param linkB
	 * @param thresholdLength
	 * @return true if <i>either</i> links are shorter than the given threshold, 
	 * false otherwise. 
	 */
	private boolean eitherLinkIsShorterThanThreshold(Link linkA, Link linkB, double thresholdLength){
		boolean hasShortLink = false;
		if(linkA.getLength() < thresholdLength || linkB.getLength() < thresholdLength){
			hasShortLink = true;
		}
		return hasShortLink;
	}
	
	/**
	 * Compare link attributes. Return whether they are the same or not.
	 */
	private boolean bothLinksHaveSameLinkStats(Link linkA, Link linkB){

		boolean bothLinksHaveSameLinkStats = true;

		if(!linkA.getAllowedModes().equals(linkB.getAllowedModes())){ bothLinksHaveSameLinkStats = false; }

		if(linkA.getFreespeed() != linkB.getFreespeed()){ bothLinksHaveSameLinkStats = false; }

		if(linkA.getCapacity() != linkB.getCapacity()){ bothLinksHaveSameLinkStats = false; }

		if(linkA.getNumberOfLanes() != linkB.getNumberOfLanes()){ bothLinksHaveSameLinkStats = false; }

		return bothLinksHaveSameLinkStats;
	}

	public static void main(String[] args) {
		final String inNetworkFile = args[ 0 ];
		final String outNetworkFile = args[ 1 ];

		Set<Integer> nodeTypesToMerge = new TreeSet<Integer>();
		nodeTypesToMerge.add(new Integer(4));
		nodeTypesToMerge.add(new Integer(5));

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		final Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile( inNetworkFile );

		NetworkSimplifier nsimply = new NetworkSimplifier();
		nsimply.setNodesToMerge(nodeTypesToMerge);
//		nsimply.setMergeLinkStats(true);
		nsimply.run(network, Double.NEGATIVE_INFINITY);

		new NetworkWriter(network).write( outNetworkFile );

	}
	
	private enum thresholdExceeded{
		EITHER, BOTH
	}
}