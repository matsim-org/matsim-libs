/* *********************************************************************** *
 * project: org.matsim.*
 * SocialAnalyzer.java
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
package playground.johannes.studies.netanalysis;

import org.matsim.contrib.common.gis.CartesianDistanceCalculator;
import org.matsim.contrib.socnetgen.sna.graph.analysis.GraphAnalyzer;
import org.matsim.contrib.socnetgen.socialnetworks.gis.GravityCostFunction;
import org.matsim.contrib.socnetgen.socialnetworks.gis.SpatialCostFunction;
import org.matsim.contrib.socnetgen.socialnetworks.graph.analysis.AnalyzerTaskComposite;
import org.matsim.contrib.socnetgen.socialnetworks.graph.social.SocialGraph;
import org.matsim.contrib.socnetgen.socialnetworks.graph.social.analysis.AgeAccessibilityTask;
import org.matsim.contrib.socnetgen.socialnetworks.graph.spatial.analysis.Accessibility;
import org.matsim.contrib.socnetgen.socialnetworks.survey.ivt2009.graph.io.SocialSparseGraphMLReader;

import java.io.IOException;

/**
 * @author illenberger
 *
 */
public class SocialAnalyzer {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		SocialSparseGraphMLReader reader = new SocialSparseGraphMLReader();
		SocialGraph graph = reader.readGraph(args[0]);
		
		String output = null;
		if(args.length > 1) {
			output = args[1];
		}
		
		SpatialCostFunction func = new GravityCostFunction(1.6, 0, new CartesianDistanceCalculator());
		
		AnalyzerTaskComposite task = new AnalyzerTaskComposite();
		task.addTask(new AgeAccessibilityTask(new Accessibility(func)));
				
		GraphAnalyzer.analyze(graph, task, output);
		

	}

}
