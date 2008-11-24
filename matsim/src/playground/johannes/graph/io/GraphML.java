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

/**
 * 
 */
package playground.johannes.graph.io;

/**
 * @author illenberger
 *
 */
interface GraphML {

	static final String GRAPHML_TAG = "graphml";
	
	static final String XMLNS_TAG = "xmlns";
	
	static final String XMLNS_URL = "http://graphml.graphdrawing.org/xmlns";
	
	static final String GRAPH_TAG = "graph"; 
	
	static final String EDGEDEFAULT_TAG = "edgedefault";
	
	static final String UNDIRECTED = "undirected";
	
	static final String DIRECTED = "directed";
	
	static final String ID_TAG = "id";
	
	static final String SOURCE_TAG = "source";
	
	static final String TARGET_TAG = "target";
	
	static final String NODE_TAG = "node";
	
	static final String EDGE_TAG = "edge";
}
