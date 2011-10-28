/* *********************************************************************** *
 * project: org.matsim.*
 * GraphML.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.johannes.sna.graph.io;

/**
 * Constants used by {@link AbstractGraphMLReader}.
 * 
 * @author illenberger
 * 
 */
public interface GraphML {

	public static final String GRAPHML_TAG = "graphml";

	public static final String XMLNS_TAG = "xmlns";

	public static final String XMLNS_URL = "http://graphml.graphdrawing.org/xmlns";

	public static final String GRAPH_TAG = "graph";

	public static final String EDGEDEFAULT_ATTR = "edgedefault";

	public static final String UNDIRECTED = "undirected";

	public static final String DIRECTED = "directed";

	public static final String ID_ATTR = "id";

	public static final String SOURCE_ATTR = "source";

	public static final String TARGET_ATTR = "target";

	public static final String NODE_TAG = "node";

	public static final String EDGE_TAG = "edge";

}
