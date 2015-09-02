/* *********************************************************************** *
 * project: org.matsim.*
 * EducationTask.java
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
package playground.johannes.socialnetworks.graph.social.analysis;

import gnu.trove.TObjectDoubleHashMap;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import playground.johannes.sna.graph.Graph;
import playground.johannes.sna.graph.analysis.ModuleAnalyzerTask;
import playground.johannes.sna.util.TXTWriter;
import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.SocialVertex;

/**
 * @author illenberger
 * 
 */
public class EducationTask extends ModuleAnalyzerTask<Education> {

	public EducationTask() {
		setModule(Education.getInstance());
	}
	
	public EducationTask(Education module) {
		setModule(module);
	}
	
	@Override
	public void analyze(Graph g, Map<String, DescriptiveStatistics> statsMap) {
		SocialGraph graph = (SocialGraph) g;
		
		if(module instanceof EducationCategorized) {
			double r = ((EducationCategorized)module).correlation(graph.getEdges());
			DescriptiveStatistics d = singleValueStats("r_edu", r, statsMap);
			printStats(d, "r_edu");
		}
		
		if (getOutputDirectory() != null) {
			try {
				
				
				Map<SocialVertex, String> values = module.values(graph.getVertices());
				TObjectDoubleHashMap<String> hist = LinguisticHistogram.create(values.values());
				
				TXTWriter.writeMap(hist, "education", "n", String.format("%1$s/education.txt", getOutputDirectory()));
				
				SocioMatrix<String> m = module.countsMatrix(graph.getVertices());
				m.toFile(String.format("%1$s/education.countsMatrix.txt", getOutputDirectory()));
				
				SocioMatrixBuilder.normalizeTotalSum(m);
				m.toFile(String.format("%1$s/education.countsMatrix.normTotal.txt", getOutputDirectory()));
				
				m = module.countsMatrix(graph.getVertices());
				SocioMatrixBuilder.normalizeRowSum(m);
				m.toFile(String.format("%1$s/education.countsMatrix.normRow.txt", getOutputDirectory()));
				
				
				m = module.probaMatrix(graph.getVertices());
				m.toFile(String.format("%1$s/education.probaMatrix.txt", getOutputDirectory()));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}
