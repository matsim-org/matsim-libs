/* *********************************************************************** *
 * project: org.matsim.*
 * GML2Pajek.java
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
package org.matsim.contrib.socnetgen.sna.graph.io;

import org.matsim.contrib.socnetgen.sna.graph.Edge;
import org.matsim.contrib.socnetgen.sna.graph.Graph;
import org.matsim.contrib.socnetgen.sna.graph.SparseGraphBuilder;
import org.matsim.contrib.socnetgen.sna.graph.Vertex;

import java.io.IOException;


/**
 * @author illenberger
 *
 */
public class GML2Pajek {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Graph g = new GMLReader().read(args[0], new SparseGraphBuilder());
		PajekWriter<Graph, Vertex, Edge> writer = new PajekWriter<Graph, Vertex, Edge>();
		writer.write(g, args[1]);
	}

}
