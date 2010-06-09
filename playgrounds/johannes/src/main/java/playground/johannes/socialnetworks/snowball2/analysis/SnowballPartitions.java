/* *********************************************************************** *
 * project: org.matsim.*
 * SnowballPartititions.java
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
package playground.johannes.socialnetworks.snowball2.analysis;

import gnu.trove.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.contrib.sna.snowball.SampledVertex;

/**
 * @author illenberger
 *
 */
public class SnowballPartitions {
	
	public static <V extends SampledVertex> Set<V> createSampledPartition(Collection<V> vertices) {
		Set<V> partition = new HashSet<V>();
		for(V vertex : vertices) {
			if(vertex.isSampled())
				partition.add(vertex);
		}
		return partition;
	}
	
	public static <V extends SampledVertex> Set<V> createSampledPartition(Collection<V> vertices, int iteration) {
		Set<V> partition = new HashSet<V>();
		for(V vertex : vertices) {
			if(vertex.getIterationSampled() == iteration)
				partition.add(vertex);
		}
		return partition;
	}
	
	public static <V extends SampledVertex> Set<V> createDetectedPartition(Collection<V> vertices, int iteration) {
		Set<V> partition = new HashSet<V>();
		for(V vertex : vertices) {
			if(vertex.getIterationDetected() == iteration)
				vertices.add(vertex);
		}
		return partition;
	}
	
	public static <V extends SampledVertex> List<Set<V>> createSampledPartitions(Collection<V> vertices) {
		TIntObjectHashMap<Set<V>> partitions = new TIntObjectHashMap<Set<V>>();
		for(V vertex : vertices) {
			int it = vertex.getIterationSampled();
			Set<V> partition = partitions.get(it);
			if(partition == null) {
				partition = new HashSet<V>();
				partitions.put(it, partition);
			}
			
			partition.add(vertex);
		}
		
		List<Set<V>> list = new ArrayList<Set<V>>(partitions.size());
		for(int i = 0; i < partitions.size()-1; i++) {
			list.add(partitions.get(i));
		}
		
		return list;
	}
}
