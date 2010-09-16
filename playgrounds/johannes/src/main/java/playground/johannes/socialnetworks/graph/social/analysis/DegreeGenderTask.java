/* *********************************************************************** *
 * project: org.matsim.*
 * DegreeGenderTask.java
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
import gnu.trove.TObjectDoubleIterator;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.Degree;
import org.matsim.contrib.sna.graph.analysis.ModuleAnalyzerTask;

import playground.johannes.socialnetworks.graph.social.SocialVertex;

/**
 * @author illenberger
 *
 */
public class DegreeGenderTask extends ModuleAnalyzerTask<Degree> {

	private static final Logger logger = Logger.getLogger(ModuleAnalyzerTask.class);
	
	@Override
	public void analyze(Graph g, Map<String, Double> stats) {
//		SocialGraph graph = (SocialGraph) g;
		
		int sumMale = 0;
		int cntMale = 0;
		int sumFemale = 0;
		int cntFemale = 0;
		
		TObjectDoubleHashMap<Vertex> values = module.values(g.getVertices());
		TObjectDoubleIterator<Vertex> it = values.iterator();
		for(int i = 0; i < values.size(); i++) {
			it.advance();
			SocialVertex v = (SocialVertex) it.key();
			double k = it.value();
			
			if(v.getPerson().getPerson().getSex() != null) {
			if(v.getPerson().getPerson().getSex().equalsIgnoreCase("m")) {
				sumMale += k;
				cntMale++;
			} else if (v.getPerson().getPerson().getSex().equalsIgnoreCase("f")){
				sumFemale += k;
				cntFemale++;
			}
			}
		}
		
		double k_mean_male = sumMale/(double)cntMale;
		double k_mean_female = sumFemale/(double)cntFemale;
		
		logger.info(String.format("k_mean_male = %1$s, k_mean_female = %2$s.", k_mean_male, k_mean_female));
	}

}
