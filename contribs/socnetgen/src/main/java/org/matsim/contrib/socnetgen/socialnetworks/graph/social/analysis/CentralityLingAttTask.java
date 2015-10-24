/* *********************************************************************** *
 * project: org.matsim.*
 * CentralityGenderTask.java
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
package org.matsim.contrib.socnetgen.socialnetworks.graph.social.analysis;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.matsim.contrib.socnetgen.sna.graph.Graph;
import org.matsim.contrib.socnetgen.sna.graph.analysis.ModuleAnalyzerTask;
import org.matsim.contrib.socnetgen.socialnetworks.graph.analysis.AttributePartition;
import org.matsim.contrib.socnetgen.socialnetworks.graph.analysis.Centrality;
import org.matsim.contrib.socnetgen.socialnetworks.graph.social.SocialGraph;
import org.matsim.contrib.socnetgen.socialnetworks.graph.social.SocialVertex;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author illenberger
 *
 */
public class CentralityLingAttTask<M extends AbstractLinguisticAttribute> extends ModuleAnalyzerTask<M> {

	private static final Logger logger = Logger.getLogger(CentralityLingAttTask.class);
	
	public CentralityLingAttTask(M module) {
		setModule(module);
	}
	
	@Override
	public void analyze(Graph g, Map<String, DescriptiveStatistics> results) {
		SocialGraph graph = (SocialGraph) g;
		
		Map<SocialVertex, String> values = module.values(graph.getVertices());
		
		Map<String, Set<SocialVertex>> categories = new AttributePartition().partition(values);
		
		Centrality centrality = new Centrality();
		
		for(Entry<String, Set<SocialVertex>> cat : categories.entrySet()) {
			centrality.init(graph, cat.getValue(), cat.getValue());
			logger.info(String.format("APL for %1$s = %2$s.", cat.getKey(), centrality.getAPL().getMean()));
		}
	}

}
