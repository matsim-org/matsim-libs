/* *********************************************************************** *
 * project: org.matsim.*
 * CentralitySampled.java
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
package playground.johannes.snowball2;

import java.util.LinkedHashMap;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;

/**
 * @author illenberger
 *
 */
public class CentralitySampled extends Centrality {

	@Override
	public void run(Graph g) {
		if(g instanceof SampledGraph)
			super.run(g);
		else
			throw new IllegalArgumentException("Graph must be instance of SampledGraph.");
	}

	@Override
	protected void calcBetweenness() {
		betweennessValues = new LinkedHashMap<Vertex, Double>();
		graphBetweenness = 0;
		double norm = (graph.getVertices().size() - 1) * (graph.getVertices().size() - 2) * 0.5; 
		for(SparseVertex v : graph.getVertices()) {
			SampledVertex delegate = (SampledVertex) graphDecorator.getVertex(v);
			if(!delegate.isAnonymous()) {
				double bc = ((CentralityVertex)v).getBetweenness() / norm;
				betweennessValues.put(delegate, bc);
				graphBetweenness += bc;
			}
		}
		
		graphBetweenness = graphBetweenness/(double)graph.getVertices().size();
	}

	@Override
	protected void calcCloseness() {
		closenessValues = new LinkedHashMap<Vertex, Double>();
		graphCloseness = 0;
		for(SparseVertex v : graph.getVertices()) {
			SampledVertex delegate = (SampledVertex) graphDecorator.getVertex(v);
			if(!delegate.isAnonymous()) {
				closenessValues.put(delegate, ((CentralityVertex)v).getCloseness());
				graphCloseness+= ((CentralityVertex)v).getCloseness();
			}
		}
		graphCloseness = graphCloseness/ (double)graph.getVertices().size();
	}

}
