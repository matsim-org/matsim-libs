/* *********************************************************************** *
 * project: org.matsim.*
 * GenderEductionTask.java
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

import gnu.trove.TObjectDoubleHashMap;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.matsim.contrib.socnetgen.sna.graph.Graph;
import org.matsim.contrib.socnetgen.sna.graph.analysis.ModuleAnalyzerTask;
import playground.johannes.socialnetworks.graph.analysis.AttributePartition;
import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.SocialVertex;
import playground.johannes.socialnetworks.graph.social.analysis.Gender;
import playground.johannes.socialnetworks.graph.social.analysis.LinguisticHistogram;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author illenberger
 *
 */
public class GenderEductionTask extends ModuleAnalyzerTask<Gender> {

	private static final Logger logger = Logger.getLogger(GenderEductionTask.class);
	
	@Override
	public void analyze(Graph g, Map<String, DescriptiveStatistics> results) {
		SocialGraph graph = (SocialGraph) g;
		
		Map<String, Set<SocialVertex>> partitions = new AttributePartition().partition(new ObservedEducationCategorized().values(graph.getVertices()));

		for(Entry<String, Set<SocialVertex>> entry : partitions.entrySet()) {
			Map<SocialVertex, String> values = Gender.getInstance().values(entry.getValue());
			TObjectDoubleHashMap<String> hist = LinguisticHistogram.create(values.values());
			logger.info(String.format("Education level %1$s: %2$s male, %3$s female.", entry.getKey(), hist.get(Gender.MALE), hist.get(Gender.FEMALE)));
		}
	}

}
