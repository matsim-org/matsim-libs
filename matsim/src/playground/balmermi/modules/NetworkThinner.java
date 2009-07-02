/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkThinner.java
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

package playground.balmermi.modules;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;

public class NetworkThinner {
	
	private static final Logger log = Logger.getLogger(NetworkThinner.class);
	
	private final void merge(List<LinkImpl> list, NetworkLayer network, Counts counts) {
		LinkImpl first = list.get(0);
		NodeImpl fromNode = first.getFromNode();
		NodeImpl toNode = list.get(list.size()-1).getToNode();
		// remove the links
		for (LinkImpl l : list) { network.removeLink(l); }
		// add a new link
		double length = 0.0; for (LinkImpl l : list) { length += l.getLength(); }
		LinkImpl newLink = network.createLink(first.getId(),fromNode,toNode,length,first.getFreespeed(Time.UNDEFINED_TIME),first.getCapacity(Time.UNDEFINED_TIME),first.getNumberOfLanes(Time.UNDEFINED_TIME));
		newLink.setType(first.getType());
		// always assign the origId of the first link (for convenience)
		newLink.setOrigId(first.getOrigId());
		
		// mapping info
		for (LinkImpl l : list) { log.info("    mapping: "+l.getId()+" => "+newLink.getId()); }
		
		// get the counts to remap
		List<Count> countsToRemap = new ArrayList<Count>();
		for (LinkImpl l : list) {
			if (counts.getCount(l.getId()) != null) { countsToRemap.add(counts.getCount(l.getId())); }
		}
		for (Count c : countsToRemap) {
			counts.getCounts().remove(c.getLocId());
			Count newCount = counts.createCount(newLink.getId(),c.getCsId());
			if (newCount != null) {
				newCount.setCoord(c.getCoord());
				newCount.getVolumes().clear();
				newCount.getVolumes().putAll(c.getVolumes());
			}
			else {
				log.info("    count id="+c.getLocId()+" removed but not replaced by a new one.");
			}
		}
	}
	
	public void run(final NetworkLayer network, Counts counts) {
		log.info("running "+this.getClass().getName()+" module...");
		log.info("  init number of links: "+network.getLinks().size());
		log.info("  init number of nodes: "+network.getNodes().size());
		// get the start links of "one way" or "two way" passes
		Set<Id> lids = new HashSet<Id>();
		for (LinkImpl l : network.getLinks().values()) {
			LinkImpl currLink = l;
			NodeImpl fromNode = currLink.getFromNode();
			while ((fromNode.getIncidentNodes().size() == 2) &&
					(((fromNode.getOutLinks().size() == 1) && (fromNode.getInLinks().size() == 1)) ||
					((fromNode.getOutLinks().size() == 2) && (fromNode.getInLinks().size() == 2)))) {
				Iterator<? extends LinkImpl> linkIt = fromNode.getInLinks().values().iterator();
				LinkImpl prevLink = linkIt.next();
				if (prevLink.getFromNode().getId().equals(currLink.getToNode().getId())) { prevLink = linkIt.next(); }
				currLink = prevLink;
				fromNode = currLink.getFromNode();
			}
			lids.add(currLink.getId());
		}
		log.info("  number of start links: "+lids.size());

		// calc the merge groups
		List<List<LinkImpl>> mergeGroups = new ArrayList<List<LinkImpl>>();
		for (Id lid : lids) {
			List<LinkImpl> linksToMerge = new ArrayList<LinkImpl>();
			LinkImpl currLink = network.getLink(lid);
			linksToMerge.add(currLink);
			NodeImpl toNode = currLink.getToNode();
			while ((toNode.getIncidentNodes().size() == 2) &&
					(((toNode.getOutLinks().size() == 1) && (toNode.getInLinks().size() == 1)) ||
					((toNode.getOutLinks().size() == 2) && (toNode.getInLinks().size() == 2)))) {
				Iterator<? extends LinkImpl> linkIt = toNode.getOutLinks().values().iterator();
				LinkImpl nextLink = linkIt.next();
				if (nextLink.getToNode().getId().equals(currLink.getFromNode().getId())) { nextLink = linkIt.next(); }

				if ((currLink.getCapacity(Time.UNDEFINED_TIME) != nextLink.getCapacity(Time.UNDEFINED_TIME)) ||
						(currLink.getFlowCapacity(Time.UNDEFINED_TIME) != nextLink.getFlowCapacity(Time.UNDEFINED_TIME)) ||
						(currLink.getFreespeed(Time.UNDEFINED_TIME) != nextLink.getFreespeed(Time.UNDEFINED_TIME)) ||
						(currLink.getNumberOfLanes(Time.UNDEFINED_TIME) != nextLink.getNumberOfLanes(Time.UNDEFINED_TIME)) ||
						!currLink.getType().equals(nextLink.getType()) ||
						!currLink.getAllowedModes().equals(nextLink.getAllowedModes())) {
					mergeGroups.add(linksToMerge);
					linksToMerge = new ArrayList<LinkImpl>();
				}
				currLink = nextLink;
				linksToMerge.add(currLink);
				toNode = currLink.getToNode();
			}
			mergeGroups.add(linksToMerge);
		}
		log.info("  number of merge groups: "+mergeGroups.size());
		int cnt = 0;
		for (List<LinkImpl> list : mergeGroups) { cnt += list.size(); }
		log.info("  number of links of all merge groups: "+cnt);
		
		int cntKeepLast = 0;
		int cntMerge = 0;
		for (List<LinkImpl> list : mergeGroups) {
			LinkImpl first = list.get(0);
			NodeImpl fromNode = first.getFromNode();
			NodeImpl toNode = list.get(list.size()-1).getToNode();

			boolean keepLast = false;
			// check if a merge will produce "double links"
			for (LinkImpl l : fromNode.getOutLinks().values()) {
				if (!l.equals(first)) {
					if (l.getToNode().equals(toNode)) { keepLast = true; }
				}
			}
			// check if a merge will produce a loop
			if (fromNode.equals(toNode)) { keepLast = true; }
			
			// keep the last link of the list as it is. It will prevent for loops and double links
			if (keepLast) { list.remove(list.size()-1); cntKeepLast++; }
			
			if (list.size() > 1) { merge(list,network,counts); cntMerge++; }
		}
		log.info("  number of last links of the merge groups which are kept (to prevent loops or double links): "+cntKeepLast);
		log.info("  number of merge groups that are merged into one link: "+cntMerge);
		
		log.info("  final number of links: "+network.getLinks().size());
		log.info("  final number of nodes: "+network.getNodes().size());
		log.info("done. ("+this.getClass().getName()+")");
		if (cntMerge > 0) {
			log.info("resulting network can be thinned even more => re-running "+this.getClass().getName()+" again...");
			this.run(network,counts);
		}
	}
}
