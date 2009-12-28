/* *********************************************************************** *
 * project: org.matsim.*
 * SampledDistance.java
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
package playground.johannes.socialnetworks.snowball2.spatial;

import java.util.Collection;

import playground.johannes.socialnetworks.graph.spatial.Distance;
import playground.johannes.socialnetworks.snowball2.SnowballPartitions;
import playground.johannes.socialnetworks.statistics.Distribution;

/**
 * @author illenberger
 *
 */
public class SampledDistance<V extends SampledSpatialVertex> extends Distance<V> {

	@Override
	public Distribution distribution(Collection<V> vertices) {
		return super.distribution(SnowballPartitions.createSampledPartition(vertices));
	}

	@Override
	public Distribution vertexAccumulatedDistribution(Collection<? extends V> vertices) {
		return super.vertexAccumulatedDistribution(SnowballPartitions.createSampledPartition(vertices));
	}

}
