/* *********************************************************************** *
 * project: org.matsim.*
 * GraphIO.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.studies.snowball;

import java.io.IOException;

import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.io.GraphMLWriter;
import org.matsim.contrib.sna.graph.io.SparseGraphMLReader;

/**
 * @author illenberger
 *
 */
public class GraphIO {

	public static void main(String args[]) throws IOException {
		SparseGraphMLReader reader = new SparseGraphMLReader();
		Graph graph = reader.readGraph("/Users/jillenberger/Work/socialnets/data/graphs/cond-mat-2005-gc.graphml");
		
		GraphMLWriter writer = new GraphMLWriter();
		writer.write(graph, "/Users/jillenberger/Work/socialnets/data/graphs/cond-mat-2005-gc2.graphml");
	}
}
