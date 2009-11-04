/* *********************************************************************** *
 * project: org.matsim.*
 * PajekAttributes.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.graph.io;

import java.util.List;

import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Vertex;


/**
 * @author illenberger
 *
 */
public interface PajekAttributes<V extends Vertex, E extends Edge> {

	public final static String VERTEX_FILL_COLOR = "ic";
	
	public List<String> getVertexAttributes();
	
	public List<String> getEdgeAttributes();
	
	public String getVertexValue(V v, String attribute);
	
	public String getEdgeValue(E e, String attribute);
	
}
