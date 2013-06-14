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

package playground.dgrether.koehlerstrehlersignal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.NetworkCalcTopoType;
import org.matsim.lanes.data.v20.LaneDefinitions20;
import org.matsim.signalsystems.data.SignalsData;

import playground.dgrether.signalsystems.utils.DgSignalsUtils;

/**
 * Simplifies a given network, by merging links.
 * 
 * @author aneumann
 * @author dgrether
 * 
 */
public class NetworkSimplifier {

	private static final Logger log = Logger.getLogger(NetworkSimplifier.class);
	private Set<Integer> nodeTopoToMerge = new TreeSet<Integer>();

	private Map<Id, Id> originalToSimplifiedLinkIdMatching = new HashMap<Id, Id>();
	private Map<Id, List<Id>> simplifiedToOriginalLinkIdMatching = new HashMap<Id, List<Id>>();

	private Set<Id> nodeIdsToRemove = new HashSet<Id>();

	private Id createId(Id inLink, Id outLink) {
		Id id = new IdImpl(inLink + "-" + outLink);
//		log.error("created id mapping: " + id.toString());
		this.simplifiedToOriginalLinkIdMatching.put(id, new ArrayList<Id>());
		List<Id> ids = new ArrayList<Id>();
		ids.add(inLink);
		ids.add(outLink);
		for (Id idOld : ids) {
			if (simplifiedToOriginalLinkIdMatching.containsKey(idOld)) { // the old Id was already simplified before
				List<Id> originalLinkIds = this.simplifiedToOriginalLinkIdMatching.remove(idOld);
				this.simplifiedToOriginalLinkIdMatching.get(id).addAll(originalLinkIds);
				
				for (Id originalId : originalLinkIds)
					this.originalToSimplifiedLinkIdMatching.put(originalId, id);		
			}
			else {
				this.originalToSimplifiedLinkIdMatching.put(idOld, id);
				this.simplifiedToOriginalLinkIdMatching.get(id).add(idOld);
			}
		}
		return id;
	}
	
	public void simplifyNetworkLanesAndSignals(final Network network, LaneDefinitions20 lanes,
			SignalsData signalsData) {
		Map<Id, Set<Id>> signalizedNodesBySystem = DgSignalsUtils.calculateSignalizedNodesPerSystem(signalsData.getSignalSystemsData(), network);
		if (this.nodeTopoToMerge.size() == 0) {
			log.error("No types of node specified. Please use setNodesToMerge to specify which nodes should be merged");
		}

		log.info("running " + this.getClass().getName() + " algorithm...");

		log.info("  checking " + network.getNodes().size() + " nodes and " + network.getLinks().size()
				+ " links for dead-ends...");

		NetworkCalcTopoType nodeTopo = new NetworkCalcTopoType();
		nodeTopo.run(network);

		for (Node node : network.getNodes().values()) {

			if (this.nodeTopoToMerge.contains(Integer.valueOf(nodeTopo.getTopoType(node)))) {

				List<Link> iLinks = new ArrayList<Link>(node.getInLinks().values());

				for (Link iL : iLinks) {
					LinkImpl inLink = (LinkImpl) iL;

					List<Link> oLinks = new ArrayList<Link>(node.getOutLinks().values());

					for (Link oL : oLinks) {
						LinkImpl outLink = (LinkImpl) oL;
						if (inLink != null && outLink != null) {
							if (!outLink.getToNode().equals(inLink.getFromNode())) {
								// Only merge links with same attributes
								if (bothLinksHaveSameLinkStats(inLink, outLink)) {
									Node removedNode = outLink.getFromNode();
									LinkImpl newLink = ((NetworkImpl) network).createAndAddLink(
											this.createId(inLink.getId(), outLink.getId()), inLink.getFromNode(), outLink.getToNode(),
											inLink.getLength() + outLink.getLength(), inLink.getFreespeed(),
											inLink.getCapacity(), inLink.getNumberOfLanes(), null, null);

									newLink.setAllowedModes(inLink.getAllowedModes());

									//remove all from scenario
									network.removeLink(inLink.getId());
									network.removeLink(outLink.getId());
									nodeIdsToRemove.add(removedNode.getId());

									if (lanes.getLanesToLinkAssignments().containsKey(inLink.getId())) {
										lanes.getLanesToLinkAssignments().remove(inLink.getId());
									}
									for (Entry<Id, Set<Id>> entry : signalizedNodesBySystem.entrySet()){
										if (entry.getValue().contains(removedNode.getId())){
											Id signalSystemId = entry.getKey();
											signalsData.getSignalSystemsData().getSignalSystemData().remove(signalSystemId);
											signalsData.getSignalGroupsData().getSignalGroupDataBySignalSystemId().remove(signalSystemId);
											signalsData.getSignalControlData().getSignalSystemControllerDataBySystemId().remove(signalSystemId);
										}
									}
								}
							}
						}
					}
				}
			}
		}

		for (Id nodeId : nodeIdsToRemove) {
			network.removeNode(nodeId);
		}

		nodeTopo = new NetworkCalcTopoType();
		nodeTopo.run(network);

		log.info("  resulting network contains " + network.getNodes().size() + " nodes and "
				+ network.getLinks().size() + " links.");
		log.info("done.");
	}

	public Set<Id> getRemovedNodeIds() {
		return this.nodeIdsToRemove;
	}
	
	public Map<Id, Id> getOriginalToSimplifiedLinkIdMatching(){
		return this.originalToSimplifiedLinkIdMatching;
	}

	/**
	 * Specify the types of node which should be merged.
	 * 
	 * @param nodeTypesToMerge
	 *          A Set of integer indicating the node types as specified by {@link NetworkCalcTopoType}
	 * @see NetworkCalcTopoType NetworkCalcTopoType for a list of available classifications.
	 */
	public void setNodesToMerge(Set<Integer> nodeTypesToMerge) {
		this.nodeTopoToMerge.addAll(nodeTypesToMerge);
	}

	/**
	 * Compare link attributes. Return whether they are the same or not.
	 */
	private boolean bothLinksHaveSameLinkStats(LinkImpl linkA, LinkImpl linkB) {

		boolean bothLinksHaveSameLinkStats = true;

		if (!linkA.getAllowedModes().equals(linkB.getAllowedModes())) {
			bothLinksHaveSameLinkStats = false;
		}

		if (linkA.getFreespeed() != linkB.getFreespeed()) {
			bothLinksHaveSameLinkStats = false;
		}

		if (linkA.getCapacity() != linkB.getCapacity()) {
			bothLinksHaveSameLinkStats = false;
		}

		if (linkA.getNumberOfLanes() != linkB.getNumberOfLanes()) {
			bothLinksHaveSameLinkStats = false;
		}

		return bothLinksHaveSameLinkStats;
	}

	public static void main(String[] args){
		//some kind of unit test for id matchings ;-)
		NetworkSimplifier ns = new NetworkSimplifier();
		Id id1 = new IdImpl("1");
		Id id2 = new IdImpl("2");
		Id id12 = ns.createId(id1, id2);
		log.error(ns.simplifiedToOriginalLinkIdMatching);
		log.error(ns.originalToSimplifiedLinkIdMatching);
		Id id3 = new IdImpl("3");
		Id id123 = ns.createId(id12, id3);
		log.error(ns.simplifiedToOriginalLinkIdMatching);
		log.error(ns.originalToSimplifiedLinkIdMatching);
		Id id4 = new IdImpl("4");
		Id id5 = new IdImpl("5");
		Id id45 = ns.createId(id4, id5);
		log.error(ns.simplifiedToOriginalLinkIdMatching);
		log.error(ns.originalToSimplifiedLinkIdMatching);
		log.error("");
		ns.createId(id45, id123);
		log.error(ns.simplifiedToOriginalLinkIdMatching);
		log.error(ns.originalToSimplifiedLinkIdMatching);
		
		
	}

	
	
}