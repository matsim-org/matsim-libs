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

import java.util.Collection;
import java.util.HashSet;
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
				vertices.add(vertex);
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
}
