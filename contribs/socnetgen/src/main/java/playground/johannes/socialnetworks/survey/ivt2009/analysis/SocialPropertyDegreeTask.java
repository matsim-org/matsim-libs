/* *********************************************************************** *
 * project: org.matsim.*
 * SocialPropertyDegreeTask.java
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

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TDoubleObjectHashMap;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.DummyDiscretizer;
import org.matsim.contrib.common.stats.TXTWriter;
import playground.johannes.sna.graph.Graph;
import playground.johannes.sna.graph.analysis.Degree;
import playground.johannes.sna.graph.analysis.ModuleAnalyzerTask;
import playground.johannes.socialnetworks.graph.analysis.VertexPropertyCorrelation;
import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.analysis.Age;

import java.io.IOException;
import java.util.Map;

/**
 * @author illenberger
 *
 */
public class SocialPropertyDegreeTask extends ModuleAnalyzerTask<Degree> {

	private Discretizer discretizer = new DummyDiscretizer();
	
	public void setDiscretizer(Discretizer discretizer) {
		this.discretizer = discretizer;
	}
	
	@Override
	public void analyze(Graph g, Map<String, DescriptiveStatistics> statsMap) {
		if(outputDirectoryNotNull()) {
			try {
				SocialGraph graph = (SocialGraph) g;
				/*
				 * age-degree correlation
				 */
				TDoubleDoubleHashMap correl = VertexPropertyCorrelation.mean(Age.getInstance(), module, graph.getVertices(), discretizer);
				TXTWriter.writeMap(correl, "k", "age", getOutputDirectory() + "/age_k.mean.txt");
				
				TDoubleObjectHashMap<DescriptiveStatistics> stat = VertexPropertyCorrelation.statistics(Age.getInstance(), module, graph.getVertices(), discretizer);
				TXTWriter.writeBoxplotStats(stat, getOutputDirectory() + "/age_k.table.txt");
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}

}
