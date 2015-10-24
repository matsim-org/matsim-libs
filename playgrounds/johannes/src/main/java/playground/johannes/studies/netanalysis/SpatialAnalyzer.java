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

import org.matsim.contrib.common.gis.CartesianDistanceCalculator;
import org.matsim.contrib.socnetgen.sna.gis.GravityCostFunction;
import org.matsim.contrib.socnetgen.sna.gis.SpatialCostFunction;
import org.matsim.contrib.socnetgen.sna.graph.analysis.AnalyzerTaskComposite;
import org.matsim.contrib.socnetgen.sna.graph.analysis.GraphAnalyzer;
import org.matsim.contrib.socnetgen.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.socnetgen.sna.graph.spatial.analysis.AcceptancePropaCategoryTask;
import org.matsim.contrib.socnetgen.sna.graph.spatial.analysis.Accessibility;
import org.matsim.contrib.socnetgen.sna.graph.spatial.analysis.EdgeLengthDegreeTask;
import org.matsim.contrib.socnetgen.sna.graph.spatial.analysis.TransitivityAccessibilityTask;
import org.matsim.contrib.socnetgen.sna.graph.spatial.io.SpatialGraphMLReader;
import org.matsim.contrib.socnetgen.sna.util.MultiThreading;

import java.io.IOException;

/**
 * @author illenberger
 *
 */
public class SpatialAnalyzer {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		MultiThreading.setNumAllowedThreads(8);
		
		SpatialGraphMLReader reader = new SpatialGraphMLReader();
		SpatialGraph graph = reader.readGraph(args[0]);
		
		String output = null;
		if(args.length > 1) {
			output = args[1];
		}
		
		SpatialCostFunction func = new GravityCostFunction(1.4, 0, new CartesianDistanceCalculator());
		
		AnalyzerTaskComposite task = new AnalyzerTaskComposite();
		task.addTask(new EdgeLengthDegreeTask());
		Accessibility access = new Accessibility(func);
//		task.addTask(new AcceptanceProbabilityTask());
		task.addTask(new AcceptancePropaCategoryTask(access));
		task.addTask(new TransitivityAccessibilityTask(access));
//		EdgeLength.getInstance().setIgnoreZero(true);
//		task.addTask(new SpatialAnalyzerTask());
//		task.addTask(new ExtendedSpatialAnalyzerTask());
//		task.addTask(new AgeAccessibilityTask(access));
		
//		Accessibility access = new Accessibility(func);
//		task.addTask(new AcceptancePropaCategoryTask(access));
		
//		SpatialPropertyDegreeTask xkTask = new SpatialPropertyDegreeTask(func, null);
//		task.addTask(xkTask);
		
//		task.addTask(new DegreeNormConstantTask());
		
		if(output != null)
			task.setOutputDirectoy(output);
		
		GraphAnalyzer.analyze(graph, task, output);	
	}

}
