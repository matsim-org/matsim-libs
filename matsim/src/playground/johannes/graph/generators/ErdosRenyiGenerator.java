/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractErdosRenyiGenerator.java
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
package playground.johannes.graph.generators;

import java.util.LinkedList;
import java.util.Random;

import playground.johannes.graph.Edge;
import playground.johannes.graph.Graph;
import playground.johannes.graph.Vertex;

/**
 * @author illenberger
 *
 */
public class ErdosRenyiGenerator<G extends Graph, V extends Vertex, E extends Edge> {

	private GraphFactory<G, V, E> factory;
	
	public ErdosRenyiGenerator(GraphFactory<G, V, E> factory) {
		this.factory = factory;
	}

	public G generate(int numVertices, double p, long randomSeed) {
		G g = factory.createGraph();
		LinkedList<V> pending = new LinkedList<V>();
		for (int i = 0; i < numVertices; i++)
			pending.add(factory.addVertex(g));

		Random random = new Random(randomSeed);
		V v1;
		while ((v1 = pending.poll()) != null) {
			for (V v2 : pending) {
				if (random.nextDouble() <= p) {
					factory.addEdge(g, v1, v2);
				}
			}
		}

		return g;
	}
}
