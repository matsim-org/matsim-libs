/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkTurnInfoBuilder
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package org.matsim.core.router.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.algorithms.NetworkExpandNode.TurnInfo;

/**
 * Creates TurnInfo objects for a Network instance.
 * 
 * @author dgrether
 * 
 */
public class NetworkTurnInfoBuilder {

	/**
	 * Creates a List of TurnInfo objects for every existing link of the network. If the links have mode attributes set,
	 * those are considered in TurnInfo creation.
	 */
	public void createAndAddTurnInfo(String mode, Map<Id<Link>, List<TurnInfo>> inLinkTurnInfoMap, Network network) {
		TurnInfo turnInfo = null;
		Set<String> modes = null;
		List<TurnInfo> turnInfosForInLink = null;
		for (Node node : network.getNodes().values()) {
			for (Link inLink : node.getInLinks().values()) {
				turnInfosForInLink = inLinkTurnInfoMap.get(inLink.getId());
				if (turnInfosForInLink == null) {
					turnInfosForInLink = new ArrayList<TurnInfo>();
					inLinkTurnInfoMap.put(inLink.getId(), turnInfosForInLink);
				}

				for (Link outLink : node.getOutLinks().values()) {
					if (! inLink.getAllowedModes().isEmpty() && ! outLink.getAllowedModes().isEmpty()) {
						if (inLink.getAllowedModes().contains(mode) && outLink.getAllowedModes().contains(mode)) {
							modes = new HashSet<String>();
							modes.add(mode);
							turnInfo = new TurnInfo(inLink.getId(), outLink.getId(), modes);
							turnInfosForInLink.add(turnInfo);
						}
					}
					else { // we have no mode information at all
						turnInfo = new TurnInfo(inLink.getId(), outLink.getId());
						turnInfosForInLink.add(turnInfo);
					}
				}
			}
		}
	}

	/**
	 * Modifies the first Map containing the allowed turning moves: All turning moves of a fromLink for that the second
	 * Map contains (only in this case!) an entry are checked for differences concerning outLinks and modes. If an outLink
	 * or mode is not contained in the restriction, the corresponding TurnInfo or mode is removed or modified in the first
	 * map.
	 */
	public void mergeTurnInfoMaps(Map<Id<Link>, List<TurnInfo>> allowedInLinkTurnInfoMap,
			Map<Id<Link>, List<TurnInfo>> restrictingTurnInfoMap) {
		for (Map.Entry<Id<Link>, List<TurnInfo>> e : allowedInLinkTurnInfoMap.entrySet()) {
			if (!restrictingTurnInfoMap.containsKey(e.getKey())) {
				continue; // there is no restriction for the inLink
			}
			else { // there are restrictions for the inLink
				Id<Link> inLinkId = e.getKey();
				List<TurnInfo> allowedTurnInfos = new ArrayList<TurnInfo>(e.getValue());
				List<TurnInfo> restrictingTurnInfos = restrictingTurnInfoMap.get(inLinkId);
				for (TurnInfo allowedForOutlink : allowedTurnInfos) {
					TurnInfo restrictionForOutlink = this.getTurnInfoForOutlinkId(
							restrictingTurnInfos, allowedForOutlink.getToLinkId());
					if (restrictionForOutlink == null) { // there is no turn at all allowed from the inLink to the outLink
						allowedInLinkTurnInfoMap.get(inLinkId).remove(allowedForOutlink);
					}
					else { // turns are restricted to some modes or allowed without any mode information
						if (restrictionForOutlink.getModes() != null && allowedForOutlink.getModes() != null){
							Set<String> commonModes = this.calculateCommonModes(
									restrictionForOutlink, allowedForOutlink);
							Set<String> allowedModes = allowedForOutlink.getModes();
							for (String mode : allowedModes) {
								if (!commonModes.contains(mode)) {
									allowedForOutlink.getModes().remove(mode);
								}
							}	
						}
					}
				}
			}
		}
	}

	/**
	 * Linear search for the TurnInfo describing the turn to the outLinkId
	 */
	public TurnInfo getTurnInfoForOutlinkId(List<TurnInfo> turnInfoList, Id<Link> outLinkId) {
		for (TurnInfo ti : turnInfoList) {
			if (ti.getToLinkId().equals(outLinkId)) {
				return ti;
			}
		}
		return null;
	}

	private Set<String> calculateCommonModes(TurnInfo first, TurnInfo second) {
		Set<String> modes = new HashSet<String>();
		for (String mode : first.getModes()) {
			if (second.getModes().contains(mode)) {
				modes.add(mode);
			}
		}
		return modes;
	}


}
