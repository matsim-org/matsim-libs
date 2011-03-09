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

import java.io.IOException;

import org.matsim.contrib.sna.graph.analysis.GraphAnalyzer;

import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.gis.GravityCostFunction;
import playground.johannes.socialnetworks.gis.SpatialCostFunction;
import playground.johannes.socialnetworks.graph.analysis.AnalyzerTaskComposite;
import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.analysis.AgeAccessibilityTask;
import playground.johannes.socialnetworks.graph.spatial.analysis.Accessibility;
import playground.johannes.socialnetworks.survey.ivt2009.graph.io.SocialSparseGraphMLReader;

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
