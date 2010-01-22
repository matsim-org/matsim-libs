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
package playground.johannes.socialnetworks.graph.social.mcmc;

import gnu.trove.TIntArrayList;
import gnu.trove.TIntObjectHashMap;

import org.matsim.api.core.v01.population.Person;

import playground.johannes.socialnetworks.graph.social.Ego;
import playground.johannes.socialnetworks.graph.social.SocialNetwork;
import playground.johannes.socialnetworks.graph.social.SocialNetworkBuilder;
import playground.johannes.socialnetworks.graph.spatial.SpatialAdjacencyMatrix;

/**
 * @author illenberger
 *
 */
public class SNAdjacencyMatrix<P extends Person> extends SpatialAdjacencyMatrix {

	public SNAdjacencyMatrix(SocialNetwork g) {
		super(g);
	}

	
	@Override
	public SocialNetwork getGraph() {
		SocialNetworkBuilder builder = new SocialNetworkBuilder();
		SocialNetwork g = new SocialNetwork();

		TIntObjectHashMap<Ego> vertexIdx = new TIntObjectHashMap<Ego>();
		for(int i = 0; i < getVertexCount(); i++) {
			Ego ego = builder.addVertex(g, getVertex(i).getPerson());
			vertexIdx.put(i, ego);
		}
		
		for(int i = 0; i < getVertexCount(); i++) {
			TIntArrayList row = getNeighbours(i);
			if(row != null) {
				for(int idx = 0; idx < row.size(); idx++) {
					int j = row.get(idx);
					if(j > i) {
						if(builder.addEdge(g, vertexIdx.get(i), vertexIdx.get(j)) == null)
							throw new RuntimeException();
					}
				}
			}
		}
		
		return g;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public Ego getVertex(int i) {
		return (Ego) super.getVertex(i);
	}
}
