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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.network.turnRestrictions.DisallowedNextLinksUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

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

	private static final Logger log = LogManager.getLogger(NetworkSimplifier.class);
	private boolean mergeLinksWithDifferentAttributes = false;
	private Collection<Integer> nodeTopoToMerge = Arrays.asList( NetworkCalcTopoType.PASS1WAY , NetworkCalcTopoType.PASS2WAY );

	private Set<Id<Node>> nodesNotToMerge = new HashSet<>();

	private final Map<Id<Link>,List<Node>> mergedLinksToIntermediateNodes = new HashMap<>();

	private final List<Consumer<Network>> preTransformations = new ArrayList<>();
	private final List<BiPredicate<Link, Link>> isMergeablePredicates = new ArrayList<>();
	private final List<BiConsumer<Tuple<Link, Link>, Link>> transferAttributesConsumers = new ArrayList<>();
	private final List<Consumer<Network>> postTransformations = new ArrayList<>();

	/**
	 * @deprecated Use {@link #createNetworkSimplifier(Network)}
	 */
	@Deprecated
	public NetworkSimplifier() {
	}

	/**
	 * Use this to create an instance of a NetworkSimplifier that also considers
	 * turn restrictions.
	 * 
	 * @param network
	 * @return
	 */
	public static NetworkSimplifier createNetworkSimplifier(Network network) {
		NetworkSimplifier networkSimplifier = new NetworkSimplifier();
		networkSimplifier.registerPreTransformation(
				DisallowedNextLinksUtils::annotateNetworkForSimplification);
		networkSimplifier.registerIsMergeablePredicate(
				DisallowedNextLinksUtils.createIsMergeablePredicate(network));
		networkSimplifier.registerTransferAttributesConsumer(
				DisallowedNextLinksUtils.createTransferAttributesConsumer(network));
		networkSimplifier.registerPostTransformation(
				DisallowedNextLinksUtils::removeAnnotation);
		return networkSimplifier;
	}

	/**
	 * Merges all qualifying links, ignoring length threshold.
	 * 
	 * @param network
	 */
	public void run(final Network network) {
		run(network, Double.POSITIVE_INFINITY, ThresholdExceeded.EITHER);
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
		run(network, thresholdLength, ThresholdExceeded.BOTH);
		run(network, thresholdLength, ThresholdExceeded.EITHER);
	}

	/**
	 * Specifies a set of nodes of which all outgoing and ingoing links should not be merged.
	 * Should probably not be used if nodes of type {@link NetworkCalcTopoType#INTERSECTION} are to be merged.
	 * tschlenther jun'17
	 * @param nodeIDs
	 */
	public void setNodesNotToMerge(Set<Long> nodeIDs){
		for(Long l : nodeIDs){
			this.nodesNotToMerge.add(Id.createNodeId(l));
		}
	}

	private void run(final Network network, double thresholdLength, ThresholdExceeded type) {

		if(this.nodeTopoToMerge.size() == 0){
			throw new RuntimeException("No types of node specified. Please use setNodesToMerge to specify which nodes should be merged");
		}

		log.info("running " + this.getClass().getName() + " algorithm...");

		NetworkCalcTopoType nodeTopo = new NetworkCalcTopoType();
		nodeTopo.run(network);

		this.preTransformations.forEach(c -> c.accept(network));

		for (Node node : network.getNodes().values()) {
			
			if(this.nodeTopoToMerge.contains(nodeTopo.getTopoType(node)) && (!this.nodesNotToMerge.contains(node.getId())) ){

				List<Link> removedLinks = new ArrayList<>();

				List<Link> iLinks = new ArrayList<>(node.getInLinks().values());
				List<Link> oLinks = new ArrayList<>(node.getOutLinks().values());

				for (Link inLink : iLinks) {
					for (Link outLink : oLinks) {

						if (areLinksMergeable(inLink, outLink)
								&& isMergeablePredicates.stream().allMatch(p -> p.test(inLink, outLink))) {
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
									if (NetworkUtils.getOrigId(inLink) != null || NetworkUtils.getOrigId(outLink) != null) {
										NetworkUtils.setOrigId(link, NetworkUtils.getOrigId(inLink) + "-" + NetworkUtils.getOrigId(outLink));
									}
									network.addLink(link);
									transferAttributesConsumers.forEach(c -> c.accept(Tuple.of(inLink, outLink), link));
									network.removeLink(inLink.getId());
									network.removeLink(outLink.getId());
									removedLinks.add(inLink);
									removedLinks.add(outLink);
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
										Link newLink = NetworkUtils.createAndAddLink(network,Id.create(inLink.getId() + "-" + outLink.getId(), Link.class), inLink.getFromNode(), outLink.getToNode(), inLink.getLength() + outLink.getLength(), inLink.getFreespeed(), inLink.getCapacity(), inLink.getNumberOfLanes());
										if (NetworkUtils.getOrigId(inLink) != null || NetworkUtils.getOrigId(outLink) != null) {
											NetworkUtils.setOrigId(newLink, NetworkUtils.getOrigId(inLink) + "-" + NetworkUtils.getOrigId(outLink));
										}

										newLink.setAllowedModes(inLink.getAllowedModes());

										transferAttributesConsumers
												.forEach(c -> c.accept(Tuple.of(inLink, outLink), newLink));
										network.removeLink(inLink.getId());
										network.removeLink(outLink.getId());
										removedLinks.add(inLink);
										removedLinks.add(outLink);
										collectMergedLinkNodeInfo(inLink, outLink, newLink.getId());
									}
								}
							}
						}
					}
				}
				for (Link removedLink : removedLinks) {
					this.mergedLinksToIntermediateNodes.remove(removedLink.getId());
				}
			}
		}

		this.postTransformations.forEach(c -> c.accept(network));

		log.info("  resulting network contains " + network.getNodes().size() + " nodes and " +
				network.getLinks().size() + " links.");
		log.info("done.");

		// writes stats as a side effect
		nodeTopo = new NetworkCalcTopoType();
		nodeTopo.run(network);
	}

	private boolean areLinksMergeable(Link inLink, Link outLink) {
		Set<Node> fromNodes = new HashSet<>();
		List<Node> tmp = this.mergedLinksToIntermediateNodes.get(inLink.getId());
		if (tmp != null) fromNodes.addAll(tmp);
		fromNodes.add(inLink.getFromNode());

		Set<Node> toNodes = new HashSet<>();
		tmp = this.mergedLinksToIntermediateNodes.get(outLink.getId());
		if (tmp != null) toNodes.addAll(tmp);
		toNodes.add(outLink.getToNode());

		// build intersection of from-nodes and to-nodes
		fromNodes.retainAll(toNodes);
		// there should be no intersection in order to merge the links
		return fromNodes.isEmpty();
	}

	private void collectMergedLinkNodeInfo(Link inLink, Link outLink, Id<Link> mergedLinkId) {
		List<Node> nodes = new ArrayList<>();
		List<Node> tmp = this.mergedLinksToIntermediateNodes.get(inLink.getId());
		if (tmp != null) {
			nodes.addAll(tmp);
		}
		tmp = this.mergedLinksToIntermediateNodes.get(outLink.getId());
		if (tmp != null) {
			nodes.addAll(tmp);
		}
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

	public void registerPreTransformation(Consumer<Network> preTransformation) {
		this.preTransformations.add(preTransformation);
	}

	public void registerIsMergeablePredicate(BiPredicate<Link, Link> isMergeablePredicate) {
		this.isMergeablePredicates.add(isMergeablePredicate);
	}

	public void registerTransferAttributesConsumer(BiConsumer<Tuple<Link, Link>, Link> transferAttributesConsumer) {
		this.transferAttributesConsumers.add(transferAttributesConsumer);
	}

	public void registerPostTransformation(Consumer<Network> postTransformation) {
		this.postTransformations.add(postTransformation);
	}

	public static void main(String[] args) {
		final String inNetworkFile = args[ 0 ];
		final String outNetworkFile = args[ 1 ];

		Set<Integer> nodeTypesToMerge = new TreeSet<>();
		nodeTypesToMerge.add(4);
		nodeTypesToMerge.add(5);

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		final Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile( inNetworkFile );

		NetworkSimplifier nsimply = new NetworkSimplifier();
		nsimply.setNodesToMerge(nodeTypesToMerge);
//		nsimply.setMergeLinkStats(true);
		nsimply.run(network, Double.NEGATIVE_INFINITY);

		new NetworkWriter(network).write( outNetworkFile );

	}
	
	private enum ThresholdExceeded {
		EITHER, BOTH
	}
}