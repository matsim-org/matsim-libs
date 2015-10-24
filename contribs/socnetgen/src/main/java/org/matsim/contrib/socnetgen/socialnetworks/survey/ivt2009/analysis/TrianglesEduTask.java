/* *********************************************************************** *
 * project: org.matsim.*
 * TrianglesEduTask.java
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
package org.matsim.contrib.socnetgen.socialnetworks.survey.ivt2009.analysis;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.matsim.contrib.socnetgen.sna.graph.Graph;
import org.matsim.contrib.socnetgen.sna.graph.Vertex;
import org.matsim.contrib.socnetgen.sna.graph.analysis.AnalyzerTask;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialGraph;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialVertex;
import org.matsim.contrib.socnetgen.sna.graph.social.analysis.EducationCategorized;
import org.matsim.contrib.socnetgen.sna.graph.social.analysis.Gender;

import java.util.List;
import java.util.Map;

/**
 * @author illenberger
 * 
 */
public class TrianglesEduTask extends AnalyzerTask {

	private static final Logger logger = Logger.getLogger(TrianglesEduTask.class);
	
	@Override
	public void analyze(Graph g, Map<String, DescriptiveStatistics> results) {
		SocialGraph graph = (SocialGraph) g;
		
		Map<SocialVertex, String> values = new EducationCategorized().values(graph.getVertices());
		double c_acad = triangles(graph, "academic", values);
		double c_nona = triangles(graph, "non-academic", values);
		
		logger.info(String.format("c_global academics = %1$s, c_global non-academics = %2$s.", c_acad, c_nona));
		
		values = new Gender().values(graph.getVertices());
		double c_male= triangles(graph, Gender.MALE, values);
		double c_female = triangles(graph, Gender.FEMALE, values);
		
		logger.info(String.format("c_global male = %1$s, c_global female = %2$s.", c_male, c_female));
	}

	private double triangles(SocialGraph graph, String attribute, Map<SocialVertex, String> attributes) {
		int n_tripples = 0;
		int n_triangles = 0;
		for (SocialVertex v : graph.getVertices()) {
			String val = attributes.get(v);
			if (val != null && val.equals(attribute)) {

				List<? extends Vertex> n1s = v.getNeighbours();
				for (int i = 0; i < n1s.size(); i++) {
					SocialVertex n1 = (SocialVertex) n1s.get(i);
					val = attributes.get(n1);
					if (val != null && val.equals(attribute)) {

						List<? extends Vertex> n2s = n1s.get(i).getNeighbours();
						for (int k = 0; k < n2s.size(); k++) {
							SocialVertex n2 = (SocialVertex) n2s.get(k);
							val = attributes.get(n2);
							if (val != null && val.equals(attribute)) {

								if (!n2s.get(k).equals(v)) {
									n_tripples++;
									if (n2s.get(k).getNeighbours().contains(v))
										n_triangles++;
								}
							}
						}
					}
				}
			}
		}

		return n_triangles / (double) n_tripples;
	}

}
