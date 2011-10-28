/* *********************************************************************** *
 * project: org.matsim.*
 * ObservedAcceptanceProbability.java
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

import java.util.Set;


import playground.johannes.sna.graph.spatial.SpatialVertex;
import playground.johannes.sna.math.DescriptivePiStatistics;
import playground.johannes.sna.snowball.SampledVertex;
import playground.johannes.sna.snowball.analysis.SnowballPartitions;
import playground.johannes.socialnetworks.graph.spatial.analysis.AcceptanceProbability;

import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class ObservedAcceptanceProbability extends AcceptanceProbability {

	private static ObservedAcceptanceProbability instance;
	
	public static ObservedAcceptanceProbability getInstance() {
		if(instance == null)
			instance = new ObservedAcceptanceProbability();
		return instance;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public DescriptivePiStatistics distribution(Set<? extends SpatialVertex> vertices, Set<Point> choiceSet) {
		return super.distribution((Set<? extends SpatialVertex>) SnowballPartitions.createSampledPartition((Set<? extends SampledVertex>)vertices), choiceSet);
	}

}
