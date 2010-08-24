/* *********************************************************************** *
 * project: org.matsim.*
 * SampledAge.java
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
package playground.johannes.socialnetworks.snowball2.social.analysis;

import gnu.trove.TDoubleDoubleHashMap;

import java.util.Set;

import playground.johannes.socialnetworks.graph.social.SocialVertex;
import playground.johannes.socialnetworks.graph.social.analysis.Age;
import playground.johannes.socialnetworks.snowball2.analysis.SnowballPartitions;
import playground.johannes.socialnetworks.snowball2.social.SocialSampledVertexDecorator;

/**
 * @author illenberger
 *
 */
public class ObservedAge extends Age {

	@Override
	public TDoubleDoubleHashMap correlation(Set<? extends SocialVertex> vertices) {
		// TODO Auto-generated method stub
		return super.correlation(SnowballPartitions.<SocialSampledVertexDecorator<?>>createSampledPartition((Set<SocialSampledVertexDecorator<?>>)vertices));
	}

	@Override
	public void boxplot(Set<? extends SocialVertex> vertices, String file) {
		// TODO Auto-generated method stub
		super.boxplot(SnowballPartitions.<SocialSampledVertexDecorator<?>>createSampledPartition((Set<SocialSampledVertexDecorator<?>>)vertices), file);
	}

//	@Override
//	public Distribution distribution(Set<? extends SocialVertex> vertices) {
//		return super.distribution(SnowballPartitions.<SocialSampledVertexDecorator<?>>createSampledPartition((Set<SocialSampledVertexDecorator<?>>)vertices));
//	}

}
