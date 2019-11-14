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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.lanes.data.Lane;
import org.matsim.lanes.data.Lanes;
import org.matsim.lanes.data.LanesToLinkAssignment;


/**
 * 
 * @author dgrether, tthunig
 */
public class LanesConsistencyChecker {
  
	private static final Logger log = Logger.getLogger(LanesConsistencyChecker.class);
	private Network network;
	private Lanes lanes;
	private boolean removeMalformed = false;
	
	public LanesConsistencyChecker(Network net, Lanes laneDefs) {
		this.network = net;
		this.lanes = laneDefs;
	}
	
	public void checkConsistency() {
		log.info("checking consistency...");
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
