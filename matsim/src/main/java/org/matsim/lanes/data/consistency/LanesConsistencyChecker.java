/* *********************************************************************** *
 * project: org.matsim.*
 * LanesConsistencyChecker
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.lanes.data.consistency;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.lanes.Lane;
import org.matsim.lanes.Lanes;
import org.matsim.lanes.LanesToLinkAssignment;


/**
 * 
 * @author dgrether, tthunig
 */
public class LanesConsistencyChecker {
  
	private static final Logger log = LogManager.getLogger(LanesConsistencyChecker.class);
	private Network network;
	private Lanes lanes;
	private boolean removeMalformed = false;
	
	public LanesConsistencyChecker(Network net, Lanes laneDefs) {
		this.network = net;
		this.lanes = laneDefs;
	}
	
	public void checkConsistency() {
		log.info("Checking consistency of lane data...");
		List<Id<Link>> linksWithMalformedLanes = new LinkedList<>();
		for (LanesToLinkAssignment l2l : this.lanes.getLanesToLinkAssignments().values()){
			if (!areLanesOnLinkConsistent(l2l)){
				linksWithMalformedLanes.add(l2l.getLinkId());
			}
		}		
		
		if (this.removeMalformed){
			for (Id<Link> linkId : linksWithMalformedLanes) {
				this.lanes.getLanesToLinkAssignments().remove(linkId);
				log.info("remove lanes on link " + linkId);
			}
		}
		log.info("checked consistency. Lanes on " + linksWithMalformedLanes.size() + " links have been removed.");
	}
	
	private boolean areLanesOnLinkConsistent(LanesToLinkAssignment l2l) {
		//check if link exists for each assignment of one or more lanes to a link
		if (!this.network.getLinks().containsKey(l2l.getLinkId())) {
			log.error("No link found for lanesToLinkAssignment on link Id(linkIdRef): "  + l2l.getLinkId());
			return false;
		}
		//check length
		else {
			Link link = this.network.getLinks().get(l2l.getLinkId());
			for (Lane l : l2l.getLanes().values()){
				if (link.getLength() < l.getStartsAtMeterFromLinkEnd()) {
					log.error("Link Id " + link.getId() + " is shorter than an assigned lane with id " + l.getId());
					return false;
				}
			}
		}

		// sbraun Dec 2020:
		// If Links are merged via NetworkSimplifier, the new name has to be added to lane file
		// (signal nodes are excluded from merging therefore the signal file is already fine)

		Map<Id<Link>,Id<Link>> mergedLinks = new HashMap();
		for (Id<Link> link : network.getLinks().keySet()){
			if(link.toString().contains("-")){
				List<String> origLinks = Arrays.asList(link.toString().split("-"));
				for (String origlink : origLinks){
					mergedLinks.put(Id.createLinkId(origlink),link);
				}
			}
		}
		// Update Lanefile
		for (Id<Link> link : network.getLinks().keySet()) {
			if (lanes.getLanesToLinkAssignments().get(link) != null &&
					lanes.getLanesToLinkAssignments().get(link).getLanes().values() != null) {
				for (Lane lane : lanes.getLanesToLinkAssignments().get(link).getLanes().values()) {
					if (lane.getToLinkIds()!=null) {
						Set<Id<Link>> links2Replace = new HashSet<>();
						for (Id<Link> toLink : lane.getToLinkIds()) {
							if (mergedLinks.keySet().contains(toLink)) {
								Id<Link> merged = mergedLinks.get(toLink);

								links2Replace.add(merged);
							}
						}
						if (!links2Replace.isEmpty()) {
							for (Id<Link> temp : links2Replace) {
								lane.addToLinkId(temp);
							}
							if (removeMalformed) {
								Iterator<Id<Link>> toLinkIdIterator = lane.getToLinkIds().iterator();
								while (toLinkIdIterator.hasNext()) {
									Id<Link> toLinkId = toLinkIdIterator.next();
									if (mergedLinks.keySet().contains(toLinkId)) {
										toLinkIdIterator.remove();
										log.info("Replace ToLinks of Lane Id: " + lane.getId() + " on Link Id: " + l2l.getLinkId() +
												" - Replace ToLink Id: " + toLinkId.toString() + " with " + mergedLinks.get(toLinkId).toString());
									}
								}
							}
						}
					}
				}
			}
		}


		//sbraun Dec 2020
		// Check a junction where the InLinks have lanes
		// More precisely: check if every Outlink is reachable (and if not delete all lanes at this junction)
		// Note: This case should happen rarely and
		// only when turn restrictions are misinterpreted or the OSM-data was not understood

		log.info("Check if all OutLinks of junction with lanes is reachable...");
		Map<Id<Node>,Set<Id<Link>>> junctionNodes = new HashMap<>();
		//Fill Map with junction nodes and all ToLinks from Lanes
		for (Id<Link> link : network.getLinks().keySet()) {
			if (lanes.getLanesToLinkAssignments().get(link) != null &&
					lanes.getLanesToLinkAssignments().get(link).getLanes().values() != null) {
				for (Lane lane : lanes.getLanesToLinkAssignments().get(link).getLanes().values()) {
					if (lane.getToLinkIds() != null) {
						Id<Node> jn = network.getLinks().get(link).getToNode().getId();
						if (!junctionNodes.containsKey(jn)){
							junctionNodes.put(jn,new HashSet<Id<Link>>());
						}
						for (Id<Link> toLink :lane.getToLinkIds()){
							if (!junctionNodes.get(jn).contains(toLink)) junctionNodes.get(jn).add(toLink);
						}
					}
				}
			}
		}
		Set<Id<Node>> checkedJn = new HashSet<Id<Node>>();
		for (Id<Node> jn : junctionNodes.keySet()){
			//At a junction if the size of the set filled above equals the number of outLinks everything is fine
			if (network.getNodes().get(jn).getOutLinks().keySet().size()== junctionNodes.get(jn).size()){
				checkedJn.add(jn);
				continue;
			}
			//If there is one inLink of that junction which has no Lanes (i.e connects every Outlink) - the junction is fine
			for (Id<Link>inLink :network.getNodes().get(jn).getInLinks().keySet()){
				if (!lanes.getLanesToLinkAssignments().containsKey(inLink)){
					checkedJn.add(jn);
					break;
				}
			}
			// Identify the not connected OutLink
			for (Id<Link>outLink :network.getNodes().get(jn).getOutLinks().keySet()){
				if (!junctionNodes.get(jn).contains(outLink) && !checkedJn.contains(jn)){
					log.warn("Link "+outLink.toString()+" is not connected to the network - remove lanes from all inLinks of the corresponding junction:");
					checkedJn.add(jn);
					for (Id<Link> inLink: network.getNodes().get(jn).getInLinks().keySet()){
						log.warn("\t\tRemove Lanes on Link "+inLink.toString());
						lanes.getLanesToLinkAssignments().remove(inLink);
					}
				}
			}
		}






		
		//check toLinks or toLanes specified in the lanes 
		for (Lane lane : l2l.getLanes().values()) {
			if (lane.getToLaneIds() != null) {
				Iterator<Id<Lane>> toLaneIdIterator = lane.getToLaneIds().iterator();
				while (toLaneIdIterator.hasNext()){
					Id<Lane> toLaneId = toLaneIdIterator.next();
					if (! l2l.getLanes().containsKey(toLaneId)){
						log.error("Error: toLane not existing:");
						log.error("  Lane Id: " + lane.getId() + " on Link Id: " + l2l.getLinkId() + 
								" leads to Lane Id: " + toLaneId + " that is not existing!");
//						return false; // do not return false because this would remove the whole l2l on this link
						// delete this toLane from the lane
						if (this.removeMalformed) {
							toLaneIdIterator.remove();
						}
					}
				}
			}
			//check availability of toLink in network
			if (lane.getToLinkIds() != null){
				Iterator<Id<Link>> toLinkIdIterator = lane.getToLinkIds().iterator();
				while (toLinkIdIterator.hasNext()) {
					Id<Link> toLinkId = toLinkIdIterator.next();
					if (! this.network.getLinks().containsKey(toLinkId)){
						log.error("No link found in network for toLinkId " + toLinkId + " of laneId " + lane.getId() + " of link id " + l2l.getLinkId());
//						return false; // do not return false because this would remove the whole l2l on this link
						// delete this toLink from the lane
						if (this.removeMalformed) {
							toLinkIdIterator.remove();
						}
					} else {
						Link link = this.network.getLinks().get(l2l.getLinkId());
						if (! link.getToNode().getOutLinks().containsKey(toLinkId)){
							log.error("The given toLink " + toLinkId + " is not reachable from lane " + lane.getId() + " on link " + link.getId());
//							return false; // do not return false because this would remove the whole l2l on this link
							// delete this toLink from the lane
							if (this.removeMalformed) {
								toLinkIdIterator.remove();
							}
						}
					}
				}
			}
			// identify lanes without tolanes and tolinks
			if ((lane.getToLaneIds() == null || lane.getToLaneIds().isEmpty()) 
					&& (lane.getToLinkIds() == null || lane.getToLinkIds().isEmpty())) {
				log.error("The lane " + lane.getId() + " on link " + l2l.getLinkId() + " does not lead to any lane nor link.");
				return false;
			}
		}
		// identify lanes on links that only have one outlink
		Link link = this.network.getLinks().get(l2l.getLinkId());
		if (link.getToNode().getOutLinks().size() <= 1){
			log.error("The link " + link.getId() + " has lanes but only one outLink.");
			return false;
		}
		
		// comment this out, because not every out-link of a node has to be reached by every in-link. theresa, aug'17
//		//second check matching of link's outlinks and lane's toLinks
//		Link link = this.network.getLinks().get(l2l.getLinkId());
//		log.info("Link id: " + l2l.getLinkId());
//		Set<Id<Link>> toLinksFromLanes = new HashSet<>();
//		for (Lane lane : l2l.getLanes().values()){
//			if (lane.getToLinkIds() != null){
//				toLinksFromLanes.addAll(lane.getToLinkIds());
//			}
//		}
//		
//		for (Link nodeOutLink : link.getToNode().getOutLinks().values()){
//			log.info("\t\thas outlink: " + nodeOutLink.getId());
//			if (!toLinksFromLanes.contains(nodeOutLink.getId())){
//				log.error("Error: Lane Outlink: ");
//				log.error("\t\tThe lanes of link " + link.getId() + " do not lead to all of the outlinks of the links toNode " + link.getToNode().getId() + " . The outlink " + nodeOutLink.getId()
//				+ " is not reachable from the lanes of this link. ");
//				for (Lane lane : l2l.getLanes().values()){
//					log.error("\t\tLane id: " + lane.getId());
//					if (lane.getToLinkIds() != null){
//						for (Id<Link> id : lane.getToLinkIds()){
//							log.error("\t\t\t\thas toLinkId: " + id);
//						}
//					}
//					log.error("End: Lane Outlink Error Message");
//				}
//				return false;
//			}
//		}
		return true;
	}

	public boolean isRemoveMalformed() {
		return removeMalformed;
	}
	
	public void setRemoveMalformed(boolean removeMalformed) {
		this.removeMalformed = removeMalformed;
	}

}
