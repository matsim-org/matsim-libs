/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.zone.ZonalSystems;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.filter.NetworkFilterManager;

/**
 * @author Michal Maciejewski (michalm)
 */
public class NetworkAreaFiltering {
	public static Network filterNetworkUsingShapefile(Network network, List<PreparedGeometry> areaGeometries,
			boolean runNetworkCleaner) {
		Set<Node> nodesWithinArea = new HashSet<>(
				ZonalSystems.selectNodesWithinArea(network.getNodes().values(), areaGeometries));

		NetworkFilterManager networkFilterManager = new NetworkFilterManager(network);
		networkFilterManager.addLinkFilter(
				l -> nodesWithinArea.contains(l.getFromNode()) || nodesWithinArea.contains(l.getToNode()));
		Network filteredNetwork = networkFilterManager.applyFilters();

		if (runNetworkCleaner) {
			new NetworkCleaner().run(filteredNetwork);
		}
		return filteredNetwork;
	}
}
