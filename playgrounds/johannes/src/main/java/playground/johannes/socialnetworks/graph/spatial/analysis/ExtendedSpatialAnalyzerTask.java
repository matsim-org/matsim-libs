/* *********************************************************************** *
 * project: org.matsim.*
 * ExtendedSpatialAnalyzerTask.java
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
package playground.johannes.socialnetworks.graph.spatial.analysis;

import java.io.IOException;

import org.matsim.contrib.sna.graph.analysis.AnalyzerTask;
import org.matsim.contrib.sna.graph.analysis.GraphAnalyzer;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.io.SpatialGraphMLReader;

import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.gis.GravityCostFunction;
import playground.johannes.socialnetworks.graph.analysis.AnalyzerTaskComposite;

/**
 * @author illenberger
 *
 */
public class ExtendedSpatialAnalyzerTask extends AnalyzerTaskComposite {

	public ExtendedSpatialAnalyzerTask() {
		addTask(new AcceptanceProbabilityTask());
		Accessibility access = new Accessibility(new GravityCostFunction(1.5, 0, new CartesianDistanceCalculator()));
		CachedAccessibility cachedAccess = new CachedAccessibility(access);
		addTask(new EdgeLengthAccessibilityTask(cachedAccess));
		addTask(new TransitivityAccessibilityTask(cachedAccess));
	}
	
	public static void main(String[] args) throws IOException {
		SpatialGraphMLReader reader = new SpatialGraphMLReader();
		SpatialGraph graph = reader.readGraph(args[0]);
		
		String output = null;
		if(args.length > 1) {
			output = args[1];
		}
		
		AnalyzerTask task = new ExtendedSpatialAnalyzerTask();
		if(output != null)
			task.setOutputDirectoy(output);
		
		GraphAnalyzer.analyze(graph, task, output);
	}
}
