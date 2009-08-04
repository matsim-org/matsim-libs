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
package playground.johannes.socialnetworks.survey.ivt2009;

import java.util.HashSet;
import java.util.Set;

/**
 * @author illenberger
 *
 */
public class SnowballPartitions {

	public static Set<SampledVertex> createSampledPartition(SampledGraph g) {
		Set<SampledVertex> vertices = new HashSet<SampledVertex>();
		for(SampledVertex vertex : g.getVertices()) {
			if(vertex.isSampled())
				vertices.add(vertex);
		}
		return vertices;
	}
	
	public static Set<SampledVertex> createSampledPartition(SampledGraph g, int itertation) {
		Set<SampledVertex> vertices = new HashSet<SampledVertex>();
		for(SampledVertex vertex : g.getVertices()) {
			if(vertex.getIterationSampled() == itertation)
				vertices.add(vertex);
		}
		return vertices;
	}
}
