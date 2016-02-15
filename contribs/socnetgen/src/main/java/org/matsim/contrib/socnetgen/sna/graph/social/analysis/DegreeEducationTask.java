/* *********************************************************************** *
 * project: org.matsim.*
 * DegreeEducationTask.java
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
package org.matsim.contrib.socnetgen.sna.graph.social.analysis;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.matsim.contrib.socnetgen.sna.graph.Graph;
import org.matsim.contrib.socnetgen.sna.graph.analysis.Degree;
import org.matsim.contrib.socnetgen.sna.graph.analysis.ModuleAnalyzerTask;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialGraph;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialVertex;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author illenberger
 *
 */
public class DegreeEducationTask extends ModuleAnalyzerTask<Degree> {

	private static final Logger logger = Logger.getLogger(DegreeEducationTask.class);
	
	private Education eduModule;
	
	public DegreeEducationTask(Degree module) {
		setModule(module);
		eduModule = Education.getInstance();
	}
	
	public DegreeEducationTask(Degree module, Education eduModule) {
		setModule(module);
		this.eduModule = eduModule;
	}
	
	@Override
	public void analyze(Graph g, Map<String, DescriptiveStatistics> results) {
		SocialGraph graph = (SocialGraph) g;
		Map<SocialVertex, String> values = eduModule.values(graph.getVertices());
		
		Map<String, Set<SocialVertex>> categories = new HashMap<String, Set<SocialVertex>>();
		
		for(Entry<SocialVertex, String> entry : values.entrySet()) {
			Set<SocialVertex> category = categories.get(entry.getValue());
			if(category == null) {
				category = new HashSet<SocialVertex>();
				categories.put(entry.getValue(), category);
			}
			
			category.add(entry.getKey());
		}
		
		for(Entry<String, Set<SocialVertex>> entry : categories.entrySet()) {
			double k_mean = module.statistics(entry.getValue()).getMean();
			logger.info(String.format("k_mean (%1$s) = %2$s.", entry.getKey(), k_mean));
		}
	}

}
