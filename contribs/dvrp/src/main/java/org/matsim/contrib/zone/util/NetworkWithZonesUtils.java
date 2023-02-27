/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package org.matsim.contrib.zone.util;

import org.matsim.api.core.v01.IdCollectors;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.zone.Zone;

public class NetworkWithZonesUtils {
	// if CRSs of the network and zones are different, zoneFinder should convert between CRSs
	public static IdMap<Link, Zone> createLinkToZoneMap(Network network, ZoneFinder zoneFinder) {
		return network.getLinks()
				.values()
				.stream()
				.collect(IdCollectors.toIdMap(Link.class, Identifiable::getId, l -> zoneFinder.findZone(l.getToNode().getCoord())));
	}

	public static IdMap<Node, Zone> createNodeToZoneMap(Network network, ZoneFinder zoneFinder) {
		return network.getNodes()
				.values()
				.stream()
				.collect(IdCollectors.toIdMap(Node.class, Identifiable::getId, n -> zoneFinder.findZone(n.getCoord())));
	}
}
