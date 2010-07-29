/* *********************************************************************** *
 * project: org.matsim.*
 * Centrality.java
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
package playground.johannes.socialnetworks.graph.analysis;

import gnu.trove.TIntDoubleIterator;
import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectLongHashMap;

import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.GraphUtils;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.matrix.AdjacencyMatrix;
import org.matsim.contrib.sna.math.Distribution;

import playground.johannes.socialnetworks.graph.matrix.MatrixCentrality;

/**
 * @author illenberger
 *
 */
public class Centrality {

	private AdjacencyMatrix<Vertex> y;
	
	private MatrixCentrality mCentrality;
	
	public void init(Graph graph) {
		y = new AdjacencyMatrix<Vertex>(graph);
		mCentrality = new MatrixCentrality();
		mCentrality.run(y);
	}
	
	public Distribution closenessDistribution() {
		return new Distribution(mCentrality.getVertexCloseness());
	}
	
	public Distribution vertexBetweennessDistribution() {
		Distribution distr = new Distribution();
		for(int i = 0; i < mCentrality.getVertexBetweenness().length; i++) {
			distr.add(mCentrality.getVertexBetweenness()[i]);
		}
		return distr;
	}
	
	public TObjectDoubleHashMap<Edge> edgeBetweenness() {
		TObjectDoubleHashMap<Edge> edgeBetweenness = new TObjectDoubleHashMap<Edge>();
		
		for(int i = 0; i < y.getVertexCount(); i++) {
			Vertex v1 = y.getVertex(i);
			
			if(mCentrality.getEdgeBetweenness()[i] != null) {
				TIntDoubleIterator it = mCentrality.getEdgeBetweenness()[i].iterator();
				for(int k = 0; k < mCentrality.getEdgeBetweenness()[i].size(); k++) {
					it.advance();
					Vertex v2 = y.getVertex(it.key());
					Edge e = GraphUtils.findEdge(v1, v2);
					edgeBetweenness.put(e, it.value());
				}
			}
		}
		
		return edgeBetweenness;
	}
	
	public int diameter() {
		return mCentrality.getDiameter();
	}
	
	public int radius() {
		return mCentrality.getRadius();
	}
}
