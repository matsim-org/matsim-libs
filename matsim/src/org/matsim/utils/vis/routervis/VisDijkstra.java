/* *********************************************************************** *
 * project: org.matsim.*
 * VisDijkstra.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.utils.vis.routervis;

import java.io.IOException;
import java.util.PriorityQueue;

import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Node;
import org.matsim.network.NetworkLayer;
import org.matsim.router.Dijkstra;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;

/**
 * @author laemmel
 */
public class VisDijkstra extends Dijkstra implements VisLeastCostPathCalculator {

	private final RouterNetStateWriter writer;

	private final static int DUMP_INTERVAL = 1;

	private int explCounter;
	private int dumpCounter;

	public VisDijkstra(final NetworkLayer network, final TravelCost costFunction, final TravelTime timeFunction, final RouterNetStateWriter writer) {
		super(network, costFunction, timeFunction);
		this.writer = writer;
		this.explCounter = 0;
		this.dumpCounter = 0;
	}
	
	@Override
	public Path calcLeastCostPath(final Node fromNode, final Node toNode, final double startTime) {
		doSnapshot();
		final Path path = super.calcLeastCostPath(fromNode, toNode, startTime);

		this.writer.reset();
		for (Link link : path.links) {
			this.writer.setLinkColor(link.getId(), 0.1);
			doSnapshot();
		}

		return path;
	}

	@Override
	protected boolean addToPendingNodes(final Link l, final Node n,
			final PriorityQueue<Node> pendingNodes, final double currTime,
			final double currCost, final Node toNode) {
		final boolean succ = super.addToPendingNodes(l, n, pendingNodes, currTime, currCost, toNode);

		if (succ) {
			/* test if the node was revisited - if so the former shortest
			 * path has to be canceled... */
			for (final Link link : l.getToNode().getInLinks().values()) {
				if (this.writer.getLinkDisplValue(link,0) == 0.25) {
					this.writer.setLinkColor(link.getId(), 0.9);
				}
			}
			this.writer.setLinkColor(l.getId(), 0.25);
		} else {
			this.writer.setLinkColor(l.getId(), 0.9);
		}

		this.explCounter++;

		if (this.explCounter >= DUMP_INTERVAL) {
			this.explCounter = 0;
			doSnapshot();
		}

		return succ;
	}

	private void doSnapshot() {
		try {
			this.writer.dump(this.dumpCounter++);
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}


}
