/* *********************************************************************** *
 * project: org.matsim.*
 * ObservedMeanDistanceAll.java
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
package org.matsim.contrib.socnetgen.socialnetworks.survey.ivt2009.analysis.deprecated;

import gnu.trove.TObjectDoubleHashMap;
import org.matsim.contrib.socnetgen.sna.graph.Vertex;
import org.matsim.contrib.socnetgen.sna.snowball.SampledVertex;
import org.matsim.contrib.socnetgen.sna.snowball.analysis.SnowballPartitions;

import java.util.Set;

/**
 * @author illenberger
 *
 */
public class ObservedMeanDistanceAll extends MeanDistanceAll {

	@Override
	public TObjectDoubleHashMap<Vertex> values(Set<? extends Vertex> vertices) {
		// TODO Auto-generated method stub
		return super.values(SnowballPartitions.createSampledPartition((Set<SampledVertex>)vertices));
	}

}
