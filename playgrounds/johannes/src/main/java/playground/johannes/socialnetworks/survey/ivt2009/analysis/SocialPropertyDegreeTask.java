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

import java.io.IOException;
import java.util.Map;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.analysis.Degree;
import org.matsim.contrib.sna.graph.analysis.ModuleAnalyzerTask;
import org.matsim.contrib.sna.math.Discretizer;
import org.matsim.contrib.sna.math.DummyDiscretizer;
import org.matsim.contrib.sna.util.TXTWriter;

import playground.johannes.socialnetworks.graph.analysis.VertexPropertyCorrelation;
import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.analysis.Age;

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
	public void analyze(Graph g, Map<String, Double> stats) {
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
