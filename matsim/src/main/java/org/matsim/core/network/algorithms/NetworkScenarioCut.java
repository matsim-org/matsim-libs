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

package org.matsim.core.network.algorithms;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.internal.NetworkRunnable;
import org.matsim.core.utils.geometry.CoordUtils;

public class NetworkScenarioCut implements NetworkRunnable {

	private enum CutType {RECTANGLE, CIRCLE}

    private final CutType cutType;
	
	private final double minX;
	private final double maxX;
	private final double minY;
	private final double maxY;
	
	private final double radius;
	private final Coord center;

	private final static Logger log = Logger.getLogger(NetworkScenarioCut.class);

	public NetworkScenarioCut(final Coord min, final Coord max) {
		super();
		
		this.cutType = CutType.RECTANGLE;
		
		this.minX = min.getX();
		this.maxX = max.getX();
		this.minY = min.getY();
		this.maxY = max.getY();
		
		this.radius = Double.MAX_VALUE;
		this.center = null;
	}

	public NetworkScenarioCut(final Coord center, final double radius) {
		super();
		
		this.cutType = CutType.CIRCLE;
		
		this.center = center;
		this.radius = radius;
		
		this.minX = Double.MIN_VALUE;
		this.maxX = Double.MAX_VALUE;
		this.minY = Double.MIN_VALUE;
		this.maxY = Double.MAX_VALUE;
	}
	
	@Override
	public void run(final Network network) {
		
		Set<Node> nodesToRemove; 
		
		if (this.cutType == CutType.RECTANGLE) nodesToRemove = rectangularCut(network);
		else if (this.cutType == CutType.CIRCLE) nodesToRemove = circularCut(network);
		else return;
		
		int nofLinksRemoved = 0;
		for (Node n : nodesToRemove) {
			nofLinksRemoved += n.getInLinks().size() + n.getOutLinks().size();
			network.removeNode(n.getId());
		}
		
		log.info("number of nodes removed: "+nodesToRemove.size());
		log.info("number of links removed: "+nofLinksRemoved);
		log.info("number of nodes remaining: "+network.getNodes().size());
		log.info("number of links remaining: "+network.getLinks().size());
	}
	
	private Set<Node> rectangularCut(Network network) {
		Set<Node> nodesToRemove = new HashSet<>();
		for (Node n : network.getNodes().values()) {
			Coord coord = n.getCoord();
			double x = coord.getX();
			double y = coord.getY();
			if (!((x < this.maxX) && (this.minX < x) && (y < this.maxY) && (this.minY < y))) {
				nodesToRemove.add(n);
			}
		}
		return nodesToRemove;
	}
	
	private Set<Node> circularCut(Network network) {
		Set<Node> nodesToRemove = new HashSet<>();
		for (Node n : network.getNodes().values()) {
			Coord coord = n.getCoord();
			double distance = CoordUtils.calcEuclideanDistance(coord, center);
			if (distance > radius) {
				nodesToRemove.add(n);
			}
		}
		return nodesToRemove;
	}
}
