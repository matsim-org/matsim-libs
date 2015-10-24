/* *********************************************************************** *
 * project: org.matsim.*
 * ObservedEducationCategorized.java
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
package playground.johannes.socialnetworks.survey.ivt2009.analysis;

import org.matsim.contrib.socnetgen.sna.snowball.analysis.SnowballPartitions;
import playground.johannes.socialnetworks.graph.social.SocialVertex;
import playground.johannes.socialnetworks.graph.social.analysis.EducationCategorized;
import playground.johannes.socialnetworks.graph.social.analysis.SocioMatrix;
import playground.johannes.socialnetworks.graph.social.analysis.SocioMatrixBuilder;
import playground.johannes.socialnetworks.snowball2.social.SocialSampledVertexDecorator;

import java.util.Map;
import java.util.Set;

/**
 * @author illenberger
 *
 */
public class ObservedEducationCategorized extends EducationCategorized {

	@Override
	public SocioMatrix<String> probaMatrix(Set<? extends SocialVertex> vertices) {
		Map<SocialVertex, String> egos = super.values(SnowballPartitions.<SocialSampledVertexDecorator<?>>createSampledPartition((Set<SocialSampledVertexDecorator<?>>)vertices));
		Map<SocialVertex, String> alters = super.values(vertices);
		return SocioMatrixBuilder.probaMatrix(egos, alters);
	}

	@Override
	public Map<SocialVertex, String> values(Set<? extends SocialVertex> vertices) {
		return super.values(SnowballPartitions.<SocialSampledVertexDecorator<?>>createSampledPartition((Set<SocialSampledVertexDecorator<?>>)vertices, 0));
	}

}
