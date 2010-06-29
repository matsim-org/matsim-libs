/* *********************************************************************** *
 * project: org.matsim.*
 * ObservedGravityGamma.java
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
package playground.johannes.socialnetworks.survey.ivt2009.analysis;

import gnu.trove.TObjectDoubleHashMap;

import java.util.Set;

import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.math.Distribution;
import org.matsim.contrib.sna.snowball.SampledVertex;

import playground.johannes.socialnetworks.graph.spatial.analysis.GravityGamma;
import playground.johannes.socialnetworks.snowball2.analysis.SnowballPartitions;

/**
 * @author illenberger
 *
 */
public class ObservedGravityGamma extends GravityGamma {

	@Override
	public TObjectDoubleHashMap<SpatialVertex> values(Set<? extends SpatialVertex> vertices) {
		return super.values((Set<? extends SpatialVertex>) SnowballPartitions.createSampledPartition((Set<? extends SampledVertex>)vertices));
	}

	@Override
	public Distribution distribution(Set<? extends SpatialVertex> vertices) {
		return super.distribution((Set<? extends SpatialVertex>) SnowballPartitions.createSampledPartition((Set<? extends SampledVertex>)vertices));
	}

}
