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

package playground.dgrether.koehlerstrehlersignal.network;

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
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCalcTopoType;
import org.matsim.lanes.data.Lane;
import org.matsim.lanes.data.Lanes;
import org.matsim.lanes.data.LanesToLinkAssignment;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.model.SignalSystem;

import playground.dgrether.signalsystems.utils.DgSignalsUtils;

/**
 * Simplifies a given network, by merging links.
 * 
 * @author aneumann
 * @author dgrether
 * 
 */
public class NetworkLanesSignalsSimplifier {

	private static final Logger log = Logger.getLogger(NetworkLanesSignalsSimplifier.class);
	private Set<Integer> nodeTopoToMerge = new TreeSet<Integer>();

	private Map<Id<Link>, Id<Link>> originalToSimplifiedLinkIdMatching = new HashMap<>();
	private Map<Id<Link>, List<Id<Link>>> simplifiedToOriginalLinkIdMatching = new HashMap<Id<Link>, List<Id<Link>>>();

	private Set<Id<Node>> nodeIdsToRemove = new HashSet<>();
	private boolean simplifySignalizedNodes;
	private double maximalLinkLength;

	private Id<Link> createId(Id<Link> inLink, Id<Link> outLink) {
		Id<Link> id = Id.create(inLink + "-" + outLink, Link.class);
//		log.error("created id mapping: " + id.toString());
		this.simplifiedToOriginalLinkIdMatching.put(id, new ArrayList<Id<Link>>());
		List<Id<Link>> ids = new ArrayList<>();
		ids.add(inLink);
		ids.add(outLink);
		for (Id<Link> idOld : ids) {
			if (simplifiedToOriginalLinkIdMatching.containsKey(idOld)) { // the old Id was already simplified before
				List<Id<Link>> originalLinkIds = this.simplifiedToOriginalLinkIdMatching.remove(idOld);
				this.simplifiedToOriginalLinkIdMatching.get(id).addAll(originalLinkIds);
				
				for (Id<Link> originalId : originalLinkIds)
					this.originalToSimplifiedLinkIdMatching.put(originalId, id);		
			}
			else {
				this.originalToSimplifiedLinkIdMatching.put(idOld, id);
				this.simplifiedToOriginalLinkIdMatching.get(id).add(idOld);
			}
		}
		return id;
	}
	
	public void simplifyNetworkLanesAndSignals(final Network network, Lanes lanes,
			SignalsData signalsData) {
				
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

				List<Link> inLinks = new ArrayList<Link>(node.getInLinks().values());

				for (Link iL : inLinks) {
					Link inLink = iL;
					List<Link> outLinks = new ArrayList<Link>(node.getOutLinks().values());

					for (Link oL : outLinks) {
						Map<Id<SignalSystem>, Set<Id<Node>>> signalizedNodesBySystem = DgSignalsUtils.calculateSignalizedNodesPerSystem(signalsData.getSignalSystemsData(), network);
						Map<Id<SignalSystem>, Set<Id<Link>>> signalizedLinksBySystemId = DgSignalsUtils.calculateSignalizedLinksPerSystem(signalsData.getSignalSystemsData());
					
						Link  outLink = oL;
						if (inLink != null && outLink != null) {
							if (! outLink.getToNode().equals(inLink.getFromNode())) {
								Node removedNode = outLink.getFromNode();
								
								if (! this.simplifySignalizedNodes) {
									boolean stop = false;
									for (Entry<Id<SignalSystem>, Set<Id<Node>>> entry : signalizedNodesBySystem.entrySet()){
										if (entry.getValue().contains(removedNode.getId())){
											stop = true;
											break;
										}
									}
									if (stop) break;//continue;
								}
								
								// The new link should not exceed the maximum link length
								if (linkLengthAccepted(inLink,outLink)){									
								// Only merge links with same attributes
								if (bothLinksHaveSameLinkStats(inLink, outLink)) {
									Link newLink = NetworkUtils.createAndAddLink(((Network) network),this.createId(inLink.getId(), outLink.getId()), inLink.getFromNode(), outLink.getToNode(), inLink.getLength() + outLink.getLength(), inLink.getFreespeed(), inLink.getCapacity(), inLink.getNumberOfLanes(), null, null);

									newLink.setAllowedModes(inLink.getAllowedModes());

									//remove all from scenario
									network.removeLink(inLink.getId());
									network.removeLink(outLink.getId());
									nodeIdsToRemove.add(removedNode.getId());

									if (lanes.getLanesToLinkAssignments().containsKey(inLink.getId())) {
										lanes.getLanesToLinkAssignments().remove(inLink.getId());
									}
									
									//take lanes of outLink and adapt length of first lane
									if (lanes.getLanesToLinkAssignments().containsKey(outLink.getId())){
										LanesToLinkAssignment l2l = lanes.getLanesToLinkAssignments().remove(outLink.getId());
										LanesToLinkAssignment newL2l = lanes.getFactory().createLanesToLinkAssignment(newLink.getId());
										newL2l.getLanes().putAll(l2l.getLanes());
										//correct first lane of newLink which starts at the beginning of inLink
										for (Lane lane : newL2l.getLanes().values()){
											if (lane.getStartsAtMeterFromLinkEnd() == outLink.getLength()){
												lane.setStartsAtMeterFromLinkEnd(inLink.getLength() + outLink.getLength());
											}
										}
										lanes.addLanesToLinkAssignment(newL2l);
									}
									
									//correct lanes with toLink = inLink
									for (LanesToLinkAssignment  l2l : lanes.getLanesToLinkAssignments().values()){
										for (Lane lane : l2l.getLanes().values()){
											if (lane.getToLinkIds() != null && lane.getToLinkIds().contains(inLink.getId())) {
												lane.getToLinkIds().remove(inLink.getId());
												lane.getToLinkIds().add(newLink.getId());
											}
										}
									}
									
									//correct signal systems at outLink
									for (Entry<Id<SignalSystem>, Set<Id<Link>>> entry : signalizedLinksBySystemId.entrySet()){
										if (entry.getValue().contains(outLink.getId())){
											Id<SignalSystem> signalSystemId = entry.getKey();
											SignalSystemData system = signalsData.getSignalSystemsData().getSignalSystemData().get(signalSystemId);
											for (SignalData signal : system.getSignalData().values()){
												if (signal.getLinkId().equals(outLink.getId())){
													signal.setLinkId(newLink.getId());
												}
											}
										}
									}
									
									//delete signal system at removedNode if one exists
									for (Entry<Id<SignalSystem>, Set<Id<Node>>> entry : signalizedNodesBySystem.entrySet()){
										if (entry.getValue().contains(removedNode.getId())){
											Id<SignalSystem> signalSystemId = entry.getKey();
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
		}

		for (Id<Node> nodeId : nodeIdsToRemove) {
			network.removeNode(nodeId);
		}

		nodeTopo = new NetworkCalcTopoType();
		nodeTopo.run(network);

		log.info("  resulting network contains " + network.getNodes().size() + " nodes and "
				+ network.getLinks().size() + " links.");
		log.info("done.");
	}

	private boolean linkLengthAccepted(Link inLink, Link outLink) {
		if (inLink.getLength() + outLink.getLength() > maximalLinkLength)
			return false;
		else
			return true;
	}

	public Set<Id<Node>> getRemovedNodeIds() {
		return this.nodeIdsToRemove;
	}
	
	public Map<Id<Link>, Id<Link>> getOriginalToSimplifiedLinkIdMatching(){
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
	private boolean bothLinksHaveSameLinkStats(Link linkA, Link linkB) {

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
		NetworkLanesSignalsSimplifier ns = new NetworkLanesSignalsSimplifier();
		Id<Link> id1 = Id.create("1", Link.class);
		Id<Link> id2 = Id.create("2", Link.class);
		Id<Link> id12 = ns.createId(id1, id2);
		log.error(ns.simplifiedToOriginalLinkIdMatching);
		log.error(ns.originalToSimplifiedLinkIdMatching);
		Id<Link> id3 = Id.create("3", Link.class);
		Id<Link> id123 = ns.createId(id12, id3);
		log.error(ns.simplifiedToOriginalLinkIdMatching);
		log.error(ns.originalToSimplifiedLinkIdMatching);
		Id<Link> id4 = Id.create("4", Link.class);
		Id<Link> id5 = Id.create("5", Link.class);
		Id<Link> id45 = ns.createId(id4, id5);
		log.error(ns.simplifiedToOriginalLinkIdMatching);
		log.error(ns.originalToSimplifiedLinkIdMatching);
		log.error("");
		ns.createId(id45, id123);
		log.error(ns.simplifiedToOriginalLinkIdMatching);
		log.error(ns.originalToSimplifiedLinkIdMatching);
		
		
	}

	public void setSimplifySignalizedNodes(boolean b) {
		this.simplifySignalizedNodes = b;
	}

	public void setMaximalLinkLength(double maximalLinkLength) {
		this.maximalLinkLength = maximalLinkLength;
	}

	
	
}