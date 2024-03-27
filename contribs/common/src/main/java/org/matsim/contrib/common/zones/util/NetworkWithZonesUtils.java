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

package org.matsim.contrib.common.zones.util;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdCollectors;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.common.zones.Zone;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class NetworkWithZonesUtils {
	// if CRSs of the network and zones are different, zoneFinder should convert between CRSs
	public static IdMap<Link, Zone> createLinkToZoneMap(Network network, ZoneFinder zoneFinder) {
		return EntryStream.of(network.getLinks())
			.mapValues(link -> zoneFinder.findZone(link.getToNode().getCoord()))
			.filterValues(Objects::nonNull)
			.collect(IdCollectors.toIdMap(Link.class, Map.Entry::getKey, Map.Entry::getValue));
	}

	public static IdMap<Node, Zone> createNodeToZoneMap(Network network, ZoneFinder zoneFinder) {
		return EntryStream.of(network.getNodes())
			.mapValues(node -> zoneFinder.findZone(node.getCoord()))
			.filterValues(Objects::nonNull)
			.collect(IdCollectors.toIdMap(Node.class, Map.Entry::getKey, Map.Entry::getValue));

	}
}
