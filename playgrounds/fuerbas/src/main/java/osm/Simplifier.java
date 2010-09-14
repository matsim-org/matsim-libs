/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityTimeAnalyzer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package osm;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.NetworkCalcTopoType;
import org.matsim.core.network.algorithms.NetworkCleaner;

public class Simplifier {

	/**
	 * @param args
	 */
	
	private Set<Integer> nodeTopoToMerge = new TreeSet<Integer>();
	
	
	public void run (final Network network) {
		
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
		NetworkCleaner nc = new NetworkCleaner();
		nc.run(network);
		nodeTopo.run(network);
	}
	
	public void setNodesToMerge(Set<Integer> nodeTypesToMerge){
		this.nodeTopoToMerge.addAll(nodeTypesToMerge);
	}

	
	private boolean bothLinksHaveSameLinkStats(LinkImpl linkA, LinkImpl linkB){

		boolean bothLinksHaveSameLinkStats = true;

		if(!linkA.getAllowedModes().equals(linkB.getAllowedModes())){ bothLinksHaveSameLinkStats = false; }

		if(linkA.getFreespeed() != linkB.getFreespeed()){ bothLinksHaveSameLinkStats = false; }

		if(linkA.getCapacity() != linkB.getCapacity()){ bothLinksHaveSameLinkStats = false; }

		if(linkA.getNumberOfLanes() != linkB.getNumberOfLanes()){ bothLinksHaveSameLinkStats = false; }

		return bothLinksHaveSameLinkStats;
	}
}
