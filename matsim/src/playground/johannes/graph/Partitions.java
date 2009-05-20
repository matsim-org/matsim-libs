/* *********************************************************************** *
 * project: org.matsim.*
 * Partitions.java
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
package playground.johannes.graph;

import java.util.HashSet;
import java.util.Set;

import gnu.trove.TDoubleObjectHashMap;
import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

/**
 * @author illenberger
 *
 */
public class Partitions {

	public static <V extends Vertex> TDoubleObjectHashMap<Set<V>> createPartitions(TObjectDoubleHashMap<V> vertexValues, double binsize) {
		TDoubleObjectHashMap<Set<V>> partitions = new TDoubleObjectHashMap<Set<V>>();
		TObjectDoubleIterator<V> it = vertexValues.iterator();
		for(int i = vertexValues.size(); i > 0; i--) {
			it.advance();
			double bin = Math.floor(it.value()/binsize) * binsize;
			Set<V> partition = partitions.get(bin);
			if(partition == null) {
				partition = new HashSet<V>();
				partitions.put(bin, partition);
			}
			partition.add(it.key());
		}
		return partitions;
	}
	
	public static <V extends Vertex> TDoubleObjectHashMap<Set<V>> createDegreePartitions(Set<V> vertices) {
		TObjectDoubleHashMap<V> degrees = new TObjectDoubleHashMap<V>();
		for(V v : vertices)
			degrees.put(v, v.getNeighbours().size());
		
		return createPartitions(degrees, 1.0);
	}
}
