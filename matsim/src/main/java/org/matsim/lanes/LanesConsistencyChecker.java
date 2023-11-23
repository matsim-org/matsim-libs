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
package org.matsim.lanes;

import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;


/**
 * 
 * @author dgrether, tthunig
 */
public final class LanesConsistencyChecker {
  
	private static final Logger log = LogManager.getLogger(LanesConsistencyChecker.class);
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
			if (!isLaneOnLinkConsistent(l2l)){
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
	
	private boolean isLaneOnLinkConsistent(LanesToLinkAssignment l2l) {
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
				for (Id<Lane> toLaneId : lane.getToLaneIds()){
					if (! l2l.getLanes().containsKey(toLaneId)){
						log.error("Error: toLane not existing:");
						log.error("  Lane Id: " + lane.getId() + " on Link Id: " + l2l.getLinkId() + 
								" leads to Lane Id: " + toLaneId + " that is not existing!");
						return false;
						// TODO just delete this toLane?
					}
				}
			}
			//check availability of toLink in network
			else if (lane.getToLinkIds() != null){
				for (Id<Link> toLinkId : lane.getToLinkIds()) {
					if (! this.network.getLinks().containsKey(toLinkId)){
						log.error("No link found in network for toLinkId " + toLinkId + " of laneId " + lane.getId() + " of link id " + l2l.getLinkId());
						return false;
						// TODO just delete this toLink?
					} else {
						Link link = this.network.getLinks().get(l2l.getLinkId());
						if (! link.getToNode().getOutLinks().containsKey(toLinkId)){
							log.error("The given toLink " + toLinkId + " is not reachable from lane " + lane.getId() + " on link " + link.getId());
							return false;
							// TODO just delete this toLink?
						}
					}
				}
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
