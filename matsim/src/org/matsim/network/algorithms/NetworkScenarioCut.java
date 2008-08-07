/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkScenarioCut.java
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

package org.matsim.network.algorithms;

import java.util.Iterator;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.utils.geometry.Coord;

public class NetworkScenarioCut {

	private final double minX;
	private final double maxX;
	private final double minY;
	private final double maxY;
	
	private final static Logger log = Logger.getLogger(NetworkScenarioCut.class);

	public NetworkScenarioCut(final Coord min, final Coord max) {
		super();
		this.minX = min.getX();
		this.maxX = max.getX();
		this.minY = min.getY();
		this.maxY = max.getY();
	}

	public void run(final NetworkLayer network) {
		log.info("running module...");

		TreeSet<Node> n_set = new TreeSet<Node>();
		Iterator<? extends Node> n_it = network.getNodes().values().iterator();
		while (n_it.hasNext()) {
			Node n = n_it.next();
			Coord coord = n.getCoord();
			double x = coord.getX();
			double y = coord.getY();
			if (!((x < this.maxX) && (this.minX < x) && (y < this.maxY) && (this.minY < y))) {
				n_set.add(n);
			}
		}

		log.info("  Number of nodes to be cut out = " + n_set.size() + "...");
		n_it = n_set.iterator();
		int l_cnt = 0;
		while (n_it.hasNext()) {
			Node n = n_it.next();
			l_cnt += n.getIncidentLinks().size();
			network.removeNode(n);
		}
		log.info("  Number of links cut out = " + l_cnt + ".");

		log.info("done.");
	}
}
