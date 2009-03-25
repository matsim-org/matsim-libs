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

import java.util.ArrayList;
import java.util.List;

import org.matsim.utils.collections.Tuple;

import playground.johannes.graph.Vertex;
import playground.johannes.graph.mcmc.AdjacencyMatrix;
import playground.johannes.socialnet.Ego;
import playground.johannes.socialnet.SocialNetwork;
import playground.johannes.socialnet.SocialTie;

/**
 * @author illenberger
 *
 */
public class SNAdjacencyMatrix extends AdjacencyMatrix {

	private List<Ego<?>> egoList;
	
	public SNAdjacencyMatrix(SocialNetwork<?> g) {
		super();
		egoList = new ArrayList<Ego<?>>(g.getVertices().size());
//		TObjectIntHashMap<Vertex> vertexIndicies = new TObjectIntHashMap<Vertex>();
		
		int idx = 0;
		for(Ego<?> v : g.getVertices()) {
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
}
