/* *********************************************************************** *
 * project: org.matsim.*
 * SNAdjacencyMatrix.java
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
package playground.johannes.socialnet.mcmc;

import gnu.trove.TIntArrayList;
import gnu.trove.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.basic.v01.population.BasicPerson;
import org.matsim.api.basic.v01.population.BasicPlan;
import org.matsim.api.basic.v01.population.BasicPlanElement;
import org.matsim.core.utils.collections.Tuple;

import playground.johannes.graph.Vertex;
import playground.johannes.graph.mcmc.AdjacencyMatrix;
import playground.johannes.socialnet.Ego;
import playground.johannes.socialnet.SocialNetwork;
import playground.johannes.socialnet.SocialTie;

/**
 * @author illenberger
 *
 */
public class SNAdjacencyMatrix<P extends BasicPerson<?>> extends AdjacencyMatrix {

	private List<Ego<P>> egoList;
	
	public SNAdjacencyMatrix(SocialNetwork<P> g) {
		super();
		egoList = new ArrayList<Ego<P>>(g.getVertices().size());
//		TObjectIntHashMap<Vertex> vertexIndicies = new TObjectIntHashMap<Vertex>();
		
		int idx = 0;
		for(Ego<P> v : g.getVertices()) {
//			vertexIndicies.put(v, idx);
			egoList.add(v);
			addVertex();
			idx++;
		}
		
		for(SocialTie e : g.getEdges()) {
			Tuple<? extends Vertex, ? extends Vertex> p = e.getVertices();
			int i = egoList.indexOf(p.getFirst());
//			if(vertexIndicies.contains(p.getFirst()))
//				i = vertexIndicies.get(p.getFirst());
//			if(egoList.contains(p.getFirst()))
//				i = egoList.indexOf(p.getFirst());
			int j = egoList.indexOf(p.getSecond());
//			if(vertexIndicies.contains(p.getSecond()))
//				j = vertexIndicies.get(p.getSecond());
			
			if(i > -1 && j > -1) {
				addEdge(i, j);
			} else {
				throw new IllegalArgumentException(String.format("Indices i=%1$s, j=%2$s not allowed!", i, j));
			}
		}
	}

	
	public SocialNetwork<P> getGraph() {
		SocialNetwork<P> g = new SocialNetwork<P>();

		TIntObjectHashMap<Ego<P>> vertexIdx = new TIntObjectHashMap<Ego<P>>();
		for(int i = 0; i < getVertexCount(); i++) {
			Ego<P> ego = g.addEgo(egoList.get(i).getPerson());
			vertexIdx.put(i, ego);
		}
		
		for(int i = 0; i < getVertexCount(); i++) {
			TIntArrayList row = getNeighbours(i);
			if(row != null) {
				for(int idx = 0; idx < row.size(); idx++) {
					int j = row.get(idx);
					if(j > i) {
						if(g.addEdge(vertexIdx.get(i), vertexIdx.get(j)) == null)
							throw new RuntimeException();
					}
				}
			}
		}
		
		return g;
	}
	
	public Ego<P> getEgo(int i) {
		return egoList.get(i);
	}
}
