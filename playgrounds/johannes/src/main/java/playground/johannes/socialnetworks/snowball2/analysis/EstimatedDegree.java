/* *********************************************************************** *
 * project: org.matsim.*
 * EstimatedDegree.java
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
package playground.johannes.socialnetworks.snowball2.analysis;

import gnu.trove.TObjectDoubleHashMap;

import java.util.Collection;
import java.util.Set;

import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.Degree;
import org.matsim.contrib.sna.math.Distribution;
import org.matsim.contrib.sna.snowball.SampledVertex;

import playground.johannes.socialnetworks.snowball2.sim.Estimator;

/**
 * @author illenberger
 *
 */
public class EstimatedDegree extends Degree {

	private Estimator estimator;
	
	public EstimatedDegree(Estimator estimator) {
		this.estimator = estimator;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Distribution distribution(Set<? extends Vertex> vertices) {
		Distribution distr = new Distribution();
		
		Set<SampledVertex> samples = SnowballPartitions.<SampledVertex>createSampledPartition((Set<SampledVertex>)vertices);
		for(SampledVertex vertex : samples) {
			distr.add(vertex.getNeighbours().size(), estimator.getWeight(vertex));
		}
		
		return distr;
	}

	@SuppressWarnings("unchecked")
	@Override
	public TObjectDoubleHashMap<Vertex> values(Collection<? extends Vertex> vertices) {
		return (TObjectDoubleHashMap<Vertex>) super.values(SnowballPartitions.<SampledVertex>createSampledPartition((Collection<SampledVertex>) vertices));
	}

}
