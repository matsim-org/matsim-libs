/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
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

package org.matsim.core.network.algorithms.intersectionSimplifier.containers;

import java.util.Comparator;
import java.util.Map;

import com.vividsolutions.jts.triangulate.quadedge.QuadEdge;

public class QuadEdgeComparator implements Comparator<QuadEdge> {
	
	Map<QuadEdge,Double> map;
	
	/**
	 * Constructor.
	 * 
	 * @param map
	 * 		map containing QuadEdge and Double
	 */
	public QuadEdgeComparator(Map<QuadEdge,Double> map) {
		this.map = map;
	}

	/**
	 * Method of comparison. Ranks the QuadEdge in descending order.
	 * 
	 * @param qeA
	 * 		quad edge to compare
	 * @param qeB
	 * 		quad edge to compare
	 * @return
	 * 		1 if double value associated to qeA  < double
	 * 		value associated to qeB,
	 * 		0 if values are equals,
	 * 		-1 otherwise
	 */
	@Override
	public int compare(QuadEdge qeA, QuadEdge qeB) {
		if (this.map.get(qeA) < this.map.get(qeB)) {
			return 1;
		} else if (this.map.get(qeA) == this.map.get(qeB)) {
			return 0;
		} else {
			return -1;
		}
	}

}
