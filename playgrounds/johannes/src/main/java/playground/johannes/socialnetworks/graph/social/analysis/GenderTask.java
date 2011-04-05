/* *********************************************************************** *
 * project: org.matsim.*
 * GenderTask.java
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
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.analysis.ModuleAnalyzerTask;
import org.matsim.contrib.sna.util.TXTWriter;

import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.SocialVertex;

/**
 * @author illenberger
 *
 */
public class GenderTask extends ModuleAnalyzerTask<Gender> {
	
	public GenderTask() {
		setModule(Gender.getInstance());
	}
	
	public GenderTask(Gender module) {
		setModule(module);
	}
	
	@Override
	public void analyze(Graph g, Map<String, DescriptiveStatistics> statsMap) {
		SocialGraph graph = (SocialGraph) g;
		
		Map<SocialVertex, String> values = module.values(graph.getVertices());
		TObjectDoubleHashMap<String> hist = LinguisticHistogram.create(values.values());
		
		DescriptiveStatistics male = new DescriptiveStatistics();
		male.addValue(hist.get(Gender.MALE));
		String key = "n_male";
		statsMap.put(key, male);
		printStats(male, key);
		
		DescriptiveStatistics female = new DescriptiveStatistics();
		female.addValue(hist.get(Gender.FEMALE));
		key = "n_female";
		statsMap.put(key, female);
		printStats(female, key);
		
		DescriptiveStatistics r = new DescriptiveStatistics();
		r.addValue(module.correlation(graph.getEdges()));
		key = "r_gender";
		statsMap.put(key, r);
		printStats(r, key);
		
		if(outputDirectoryNotNull()) {
			try {
				TXTWriter.writeMap(hist, "gender", "n", String.format("%1$s/gender.txt", getOutputDirectory()));
				
				SocioMatrix<String> m = module.countsMatrix(graph.getVertices());
				m.toFile(String.format("%1$s/gender.countsMatrix.txt", getOutputDirectory()));
				
				SocioMatrixBuilder.normalizeTotalSum(m);
				m.toFile(String.format("%1$s/gender.countsMatrix.normTotal.txt", getOutputDirectory()));
				
				m = module.countsMatrix(graph.getVertices());
				SocioMatrixBuilder.normalizeRowSum(m);
				m.toFile(String.format("%1$s/gender.countsMatrix.normRow.txt", getOutputDirectory()));
				
				
				m = module.probaMatrix(graph.getVertices());
				m.toFile(String.format("%1$s/gender.probaMatrix.txt", getOutputDirectory()));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
