/* *********************************************************************** *
 * project: org.matsim.*
 * ObservedTransitivity.java
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
import org.matsim.contrib.sna.graph.analysis.Transitivity;
import org.matsim.contrib.sna.math.Distribution;
import org.matsim.contrib.sna.snowball.SampledVertex;
import org.matsim.contrib.sna.snowball.analysis.SnowballPartitions;


/**
 * @author illenberger
 *
 */
public class ObservedTransitivity extends Transitivity {

	@SuppressWarnings("unchecked")
	@Override
	public <V extends Vertex> TObjectDoubleHashMap<V> localClusteringCoefficients(Collection<V> vertices) {
		return (TObjectDoubleHashMap<V>) super.localClusteringCoefficients(SnowballPartitions.<SampledVertex>createSampledPartition((Collection<SampledVertex>)vertices));
	}

	@SuppressWarnings("unchecked")
	@Override
	public Distribution localClusteringDistribution(Set<? extends Vertex> vertices) {
		return super.localClusteringDistribution(SnowballPartitions.<SampledVertex>createSampledPartition((Collection<SampledVertex>)vertices));
	}

}
