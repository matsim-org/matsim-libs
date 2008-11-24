/* *********************************************************************** *
 * project: org.matsim.*
 * Edge.java
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
package playground.johannes.graph;

import org.matsim.utils.collections.Tuple;

/**
 * @author illenberger
 *
 */
public interface Edge {

	public Tuple<? extends Vertex, ? extends Vertex> getVertices();
	
	public Vertex getOpposite(Vertex v);
	
}
