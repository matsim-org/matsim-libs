/* *********************************************************************** *
 * project: org.matsim.*
 * SampledDegree.java
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
package playground.johannes.socialnetworks.snowball2;

import gnu.trove.TObjectDoubleHashMap;

import java.util.Collection;
import java.util.Set;

import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.math.Distribution;
import org.matsim.contrib.sna.snowball.SampledGraph;
import org.matsim.contrib.sna.snowball.SampledVertex;
import org.matsim.contrib.sna.snowball.spatial.SampledSpatialSparseGraph;

import playground.johannes.socialnetworks.graph.analysis.Degree;

/**
 * @author illenberger
 *
 */
public class SampledDegree extends Degree {

	@Override
	public Distribution distribution(Set<? extends Vertex> vertices) {
		Set<SampledVertex> set = SnowballPartitions.<SampledVertex>createSampledPartition((Collection<SampledVertex>)vertices);
		return super.distribution(set);
	}

	@Override
	public TObjectDoubleHashMap<? extends SampledVertex> values(Collection<? extends Vertex> vertices) {
		return (TObjectDoubleHashMap<? extends SampledVertex>) super.values(SnowballPartitions.<SampledVertex>createSampledPartition((Collection<SampledVertex>) vertices));
	}
	
	public static void test() {
		SampledGraph graph = new SampledSpatialSparseGraph(null);
		
		SampledDegree degree = new SampledDegree();
		double k_mean = degree.distribution(graph.getVertices()).mean();
	}

}
