/* *********************************************************************** *
 * project: org.matsim.*
 * SptialAnalyzer.java
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
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.io.SpatialGraphMLReader;

import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.gis.GravityCostFunction;
import playground.johannes.socialnetworks.gis.SpatialCostFunction;
import playground.johannes.socialnetworks.graph.analysis.AnalyzerTaskComposite;
import playground.johannes.socialnetworks.graph.spatial.analysis.Accessibility;
import playground.johannes.socialnetworks.graph.spatial.analysis.SpatialAnalyzerTask;

/**
 * @author illenberger
 *
 */
public class SpatialAnalyzer {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		SpatialGraphMLReader reader = new SpatialGraphMLReader();
		SpatialGraph graph = reader.readGraph(args[0]);
		
		String output = null;
		if(args.length > 1) {
			output = args[1];
		}
		
		SpatialCostFunction func = new GravityCostFunction(1.4, 0, new CartesianDistanceCalculator());
		
		AnalyzerTaskComposite task = new AnalyzerTaskComposite();
		Accessibility access = new Accessibility(func);
//		task.addTask(new AcceptancePropaCategoryTask(access));
//		task.addTask(new TransitivityAccessibilityTask(access));
		task.addTask(new SpatialAnalyzerTask());
//		task.addTask(new ExtendedSpatialAnalyzerTask());
//		task.addTask(new AgeAccessibilityTask(func));
		
//		Accessibility access = new Accessibility(func);
//		task.addTask(new AcceptancePropaCategoryTask(access));
		
//		SpatialPropertyDegreeTask xkTask = new SpatialPropertyDegreeTask(func, null);
//		task.addTask(xkTask);
		
//		task.addTask(new DegreeNormConstantTask());
		
		if(output != null)
			task.setOutputDirectoy(output);
		
		GraphAnalyzer.analyze(graph, task, output);
		
//		if(output != null)
//			GraphAnalyzer.writeStats(stats, output + "/summary.txt");
	
	}

}
