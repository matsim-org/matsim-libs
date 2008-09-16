/* *********************************************************************** *
 * project: org.matsim.*
 * SampledGraph.java
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

/**
 * 
 */
package playground.johannes.snowball2;

import java.util.Set;

import edu.uci.ics.jung.graph.impl.UndirectedSparseGraph;

/**
 * @author illenberger
 *
 */
public class SampledGraph extends UndirectedSparseGraph {

	@SuppressWarnings("unchecked")
	@Override
	public Set<SampledVertex> getVertices() {
		return super.getVertices();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Set<SampledEdge> getEdges() {
		return super.getEdges();
	}

}
