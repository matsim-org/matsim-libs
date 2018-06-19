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

package org.matsim.contrib.dvrp.router;

import java.util.LinkedList;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.common.collect.ImmutableList;

public class DijkstraWithThinPath extends Dijkstra {
	public DijkstraWithThinPath(Network network, TravelDisutility costFunction, TravelTime timeFunction) {
		super(network, costFunction, timeFunction);
	}

	public DijkstraWithThinPath(final Network network, final TravelDisutility costFunction,
			final TravelTime timeFunction, final PreProcessDijkstra preProcessData) {
		super(network, costFunction, timeFunction, preProcessData);
	}

	@Override
	protected Path constructPath(Node fromNode, Node toNode, double startTime, double arrivalTime) {
		LinkedList<Link> links = new LinkedList<>();

		Link tmpLink = getData(toNode).getPrevLink();
		if (tmpLink != null) {
			while (tmpLink.getFromNode() != fromNode) {
				links.add(tmpLink);
				tmpLink = getData(tmpLink.getFromNode()).getPrevLink();
			}
			links.add(tmpLink);
		}

		return new Path(null, ImmutableList.copyOf(links.descendingIterator()), arrivalTime - startTime,
				getData(toNode).getCost());
	}
}
