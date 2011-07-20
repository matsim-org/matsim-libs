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

import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.GraphUtils;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.matrix.AdjacencyMatrix;

import playground.johannes.socialnetworks.graph.matrix.MatrixCentrality;

/**
 * @author illenberger
 *
 */
public class Centrality {

	private AdjacencyMatrix<Vertex> y;
	
	private MatrixCentrality mCentrality;
	
	private Set<? extends Vertex> sources;
	
	public void init(Graph graph, boolean calcBetweenness) {
		y = new AdjacencyMatrix<Vertex>(graph);
		mCentrality = new MatrixCentrality();
		mCentrality.setCalcBetweenness(calcBetweenness);
		mCentrality.run(y);
	}
	
	public void init(Graph graph) {
		init(graph, true);
	}
	
	public void init(Graph graph, Set<? extends Vertex> sources, Set<? extends Vertex> targets) {
		init(graph, sources, targets, true);
	}
	
	public void init(Graph graph, Set<? extends Vertex> sources, Set<? extends Vertex> targets, boolean calcBetweenness) {
		this.sources = sources;
		y = new AdjacencyMatrix<Vertex>(graph);
		
		int[] sourceIndices = new int[sources.size()];
		int[] targetIndices = new int[targets.size()];
		
		int i = 0;
		for(Vertex v : sources) {
			sourceIndices[i] = y.getIndex(v);
			i++;
		}
		
		i = 0;
		for(Vertex v : targets) {
			targetIndices[i] = y.getIndex(v);
			i++;
		}
		
		mCentrality = new MatrixCentrality();
		mCentrality.setCalcBetweenness(calcBetweenness);
		mCentrality.run(y, sourceIndices, targetIndices);
	}
	
	public DescriptiveStatistics closenessDistribution() {
		DescriptiveStatistics ds = new DescriptiveStatistics();
		
		if (sources == null) {
			for (double val : mCentrality.getVertexCloseness()) {
				if(!Double.isInfinite(val))
					ds.addValue(val);
			}
		} else {
			for(Vertex v : sources) {
				int idx = y.getIndex(v);
				double val = mCentrality.getVertexCloseness()[idx];
				if(!Double.isInfinite(val))
					ds.addValue(val);
			}
		}
		return ds;
	}
	
	public DescriptiveStatistics vertexBetweennessDistribution() {
		DescriptiveStatistics distr = new DescriptiveStatistics();
		for(int i = 0; i < mCentrality.getVertexBetweenness().length; i++) {
			distr.addValue(mCentrality.getVertexBetweenness()[i]);
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
	
	public DescriptiveStatistics getAPL() {
		return mCentrality.getAPL();
	}
}
