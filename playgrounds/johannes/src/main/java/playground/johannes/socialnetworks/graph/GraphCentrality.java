/* *********************************************************************** *
 * project: org.matsim.*
 * GraphCentrao.java
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
package playground.johannes.socialnetworks.graph;

import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;

import gnu.trove.TIntIntIterator;
import gnu.trove.TObjectIntHashMap;
import playground.johannes.socialnetworks.graph.matrix.Centrality;
import playground.johannes.socialnetworks.graph.mcmc.AdjacencyMatrixDecorator;

/**
 * @author illenberger
 *
 */
public class GraphCentrality {

	private Graph g;
	
	private TObjectIntHashMap<Edge> edgeBetweenness;
	
	public GraphCentrality(Graph g) {
		this.g = g;
	}
	
	public TObjectIntHashMap<Edge> getEdgeBetweenness() {
		return edgeBetweenness;
	}
	
	public void calculate() {
		AdjacencyMatrixDecorator<? extends Vertex> y = new AdjacencyMatrixDecorator<Vertex>(g);
		
		Centrality c = new Centrality();
		c.run(y);
	
		int sum = 0;
		for(int i = 0; i < c.getVertexBetweenness().length; i++) {
			sum += c.getVertexBetweenness()[i];
		}
//		for(int i = 0; i < c.getVertexBetweenness().length; i++) {
//			System.out.println((i+1) + " " + c.getVertexBetweenness()[i]/(float)sum);
//		}
		
		edgeBetweenness = new TObjectIntHashMap<Edge>();
		
		for(int i = 0; i < y.getVertexCount(); i++) {
			Vertex v1 = y.getVertex(i);
			
			if(c.getEdgeBetweenness()[i] != null) {
				TIntIntIterator it = c.getEdgeBetweenness()[i].iterator();
				for(int k = 0; k < c.getEdgeBetweenness()[i].size(); k++) {
					it.advance();
					Vertex v2 = y.getVertex(it.key());
					Edge e = getEdge(v1, v2);
					edgeBetweenness.put(e, it.value());
				}
			}
		}
	}
	
	private Edge getEdge(Vertex v1, Vertex v2) {
		for(Edge e : v1.getEdges()) {
			if(e.getOpposite(v1).equals(v2)) {
				return e;
			}
		}
		
		return null;
	}
}
