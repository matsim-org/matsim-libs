/* *********************************************************************** *
 * project: org.matsim.*
 * PropDegreeFixedSizePartition.java
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
package playground.johannes.sna.graph.analysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import playground.johannes.sna.graph.Vertex;

/**
 * @author illenberger
 *
 */
public class PropDegreeFixedSizePartition<V extends Vertex> implements VertexFilter<V> {

	private final int n;
	
	private final Random random;
	
	public PropDegreeFixedSizePartition(int n, long randomSeed) {
		this.n = n;
		this.random = new Random(randomSeed);
	}
	
	@Override
	public Set<V> apply(Set<V> vertices) {
		double ksum = 0;
		for(V v : vertices) {
			ksum += v.getNeighbours().size();
		}
		
		double psum = 0;
		for(V v : vertices) {
			psum += v.getNeighbours().size()/ksum;
		}
		
		double c = n/psum;
		
		List<V> vertexList = new ArrayList<V>(vertices);
		Set<V> partition = new HashSet<V>();
		
		while(partition.size() < n) {
			V v = vertexList.get(random.nextInt(vertices.size()));
			double p = v.getNeighbours().size()/ksum * c;
			if(p > random.nextDouble()) {
				partition.add(v);
			}
		}
		
		return partition;
	}

}
