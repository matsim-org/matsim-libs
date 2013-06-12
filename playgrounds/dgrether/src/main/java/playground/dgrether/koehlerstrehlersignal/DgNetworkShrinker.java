/* *********************************************************************** *
 * project: org.matsim.*
 * CottbusSmallNetworkGenerator
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.analysis.FeatureNetworkLinkStartOrEndCoordFilter;

import com.vividsolutions.jts.geom.Envelope;


public class DgNetworkShrinker {


	private Set<Id> signalizedNodes;

	public Network createSmallNetwork(Network net, Envelope envelope, CoordinateReferenceSystem networkCrs) {
		
		NetworkFilterManager filterManager = new NetworkFilterManager(net);
		filterManager.addLinkFilter(new FeatureNetworkLinkStartOrEndCoordFilter(networkCrs, envelope, networkCrs));
		Set<Id> shortestPathLinkIds = new TtSignalizedNodeShortestPath().calcShortestPathLinkIdsBetweenSignalizedNodes(net, signalizedNodes);
		filterManager.addLinkFilter(new SignalizedNodesSpeedFilter(this.signalizedNodes, shortestPathLinkIds));
		Network newNetwork = filterManager.applyFilters();
		return newNetwork;		
	}

	public void setSignalizedNodes(Set<Id> signalizedNodes) {
		this.signalizedNodes = signalizedNodes;
	}
}

