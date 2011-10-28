/* *********************************************************************** *
 * project: org.matsim.*
 * SparseGraphUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.johannes.sna.graph;

/**
 * Utility class for SpraseGraphs.
 * 
 * @author jillenberger
 * 
 */
public class GraphUtils {

	/**
	 * Returns the edge connecting <tt>v1</tt> and <tt>v2</tt>. If <tt>v1</tt>
	 * and <tt>v2</tt> are connected by multiple edges the first edge that is
	 * found will be returned.
	 * 
	 * @param v1
	 *            a vertex.
	 * @param v2
	 *            a vertex.
	 * @return the edge connecting <tt>v1</tt> and <tt>v2</tt>, or <tt>null</tt>
	 *         if <tt>v1</tt> and <tt>v2</tt> are not connected.
	 */
	public static Edge findEdge(Vertex v1, Vertex v2) {
		Edge e = null;
		int cnt = v1.getEdges().size();
		for (int i = 0; i < cnt; i++) {
			e = v1.getEdges().get(i);
			if (e.getOpposite(v1) == v2) {
				return e;
			}
		}

		return null;
	}
}
