/* *********************************************************************** *
 * project: org.matsim.*
 * DegreeAgeTask.java
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
package playground.johannes.socialnetworks.graph.social.analysis;

import gnu.trove.TDoubleDoubleHashMap;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import playground.johannes.sna.graph.Graph;
import playground.johannes.sna.graph.analysis.Degree;
import playground.johannes.sna.graph.analysis.ModuleAnalyzerTask;
import playground.johannes.sna.math.LinearDiscretizer;
import playground.johannes.sna.util.TXTWriter;
import playground.johannes.socialnetworks.graph.analysis.VertexPropertyCorrelation;

/**
 * @author illenberger
 *
 */
public class DegreeAgeTask extends ModuleAnalyzerTask<Degree> {

	public DegreeAgeTask(Degree module) {
		setModule(module);
	}
	
	@Override
	public void analyze(Graph graph, Map<String, DescriptiveStatistics> results) {
		if(outputDirectoryNotNull()) {
			TDoubleDoubleHashMap hist = VertexPropertyCorrelation.mean(module, Age.getInstance(), graph.getVertices(), new LinearDiscretizer(5.0));
			try {
				TXTWriter.writeMap(hist, "age", "k", getOutputDirectory() + "/k_mean_age.txt");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
