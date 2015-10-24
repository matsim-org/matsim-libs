/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialAnalyzerTask.java
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
package org.matsim.contrib.socnetgen.socialnetworks.survey.ivt2009.analysis;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.matsim.contrib.socnetgen.sna.gis.GravityCostFunction;
import org.matsim.contrib.socnetgen.sna.graph.analysis.AnalyzerTaskComposite;
import org.matsim.contrib.socnetgen.sna.graph.spatial.analysis.Accessibility;
import org.matsim.contrib.socnetgen.sna.graph.spatial.analysis.EdgeLengthCategoryTask;
import org.matsim.contrib.socnetgen.sna.snowball.spatial.analysis.ObservedAccessibility;

import java.util.Set;

/**
 * @author illenberger
 *
 */
public class ObsSpatialAnalyzerTask extends AnalyzerTaskComposite {
	
	public ObsSpatialAnalyzerTask(Set<Point> points, Geometry boundary) {
//		addTask(new CoordAvailTask());
//		EdgeLengthTask distanceTask = new EdgeLengthTask();
//		distanceTask.setModule(EdgeLength.getInstance());
//		EdgeLength.getInstance().setIgnoreZero(true);
//		addTask(new EdgeLengthTask());
		
//		AcceptanceProbabilityTask acceptTask = new AcceptanceProbabilityTask(points);
//		acceptTask.setModule(ObservedAcceptanceProbability.getInstance());
//		addTask(acceptTask);
		
		Accessibility access = new ObservedAccessibility(new GravityCostFunction(1.4, 0));
		access.setTargets(points);
		
//		addTask(new DegreeAccessibilityTask(access));
//		addTask(new EdgeLengthAccessibilityTask(access));
		
//		AcceptancePropaCategoryTask t = new AcceptancePropaCategoryTask(access);
//		t.setBoundary(boundary);
//		t.setDestinations(points);
//		addTask(t);
//		AcceptanceProbaDegreeTask t = new AcceptanceProbaDegreeTask();
//		t.setDestinations(points);
//		addTask(t);
		EdgeLengthCategoryTask t = new EdgeLengthCategoryTask(access);
//		t.setDestinations(points);
		addTask(t);
//		
//		TransitivityAccessibilityTask tatask = new TransitivityAccessibilityTask(access);
//		addTask(tatask);
//		
//		addTask(new F2FFreqEdgeLengthTask());
//		addTask(new TripTask());
//		addTask(new F2FFrequencyTask());
	}
}
