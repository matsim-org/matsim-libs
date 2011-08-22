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
package playground.johannes.socialnetworks.survey.ivt2009.analysis;

import java.util.Set;

import playground.johannes.socialnetworks.gis.GravityCostFunction;
import playground.johannes.socialnetworks.graph.analysis.AnalyzerTaskComposite;
import playground.johannes.socialnetworks.graph.spatial.analysis.AcceptancePropaCategoryTask;
import playground.johannes.socialnetworks.graph.spatial.analysis.Accessibility;
import playground.johannes.socialnetworks.graph.spatial.analysis.DegreeAccessibilityTask;
import playground.johannes.socialnetworks.snowball2.spatial.analysis.ObservedAccessibility;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class ObsSpatialAnalyzerTask extends AnalyzerTaskComposite {
	
	public ObsSpatialAnalyzerTask(Set<Point> points, Geometry boundary) {
		
		
//		addTask(new EpsilonTask(points));
		
//		EdgeLengthTask distanceTask = new EdgeLengthTask();
//		distanceTask.setModule(EdgeLength.getInstance());
//		addTask(new EdgeLengthTask());
		
//		AcceptanceProbabilityTask acceptTask = new AcceptanceProbabilityTask(points);
//		acceptTask.setModule(ObservedAcceptanceProbability.getInstance());
//		addTask(acceptTask);
		
		Accessibility access = new ObservedAccessibility(new GravityCostFunction(1.4, 0));
		access.setTargets(points);
		addTask(new DegreeAccessibilityTask(access));
		
		
//		addTask(new EdgeLengthAccessibilityTask(access));
//		
//		addTask(new TripTask());
		
//		AcceptancePropaCategoryTask t = new AcceptancePropaCategoryTask(access);
//		t.setBoundary(boundary);
//		t.setDestinations(points);
//		addTask(t);
//		
//		TransitivityAccessibilityTask tatask = new TransitivityAccessibilityTask(access);
//		addTask(tatask);
//		PropConstAccessibilityTask t = new PropConstAccessibilityTask();
//		t.setTargets(points);
//		addTask(t);
		
//		DegreeNormConstantTask kcTask = new DegreeNormConstantTask();
//		ObservedNormConstant norm = new ObservedNormConstant();
//		norm.setDestinations(points);
//		kcTask.setModule(norm);
//		addTask(kcTask);
		
//		GravityCostFunction func = new GravityCostFunction(1.4, 0, new CartesianDistanceCalculator());
////////		
//		SpatialPropertyDegreeTask xkTask = new SpatialPropertyDegreeTask(func, points);
//		xkTask.setModule(new ObservedDegree());
//		xkTask.setDiscretizer(new LinearDiscretizer(5.0));
//		addTask(xkTask);
//		NormConstAcceptPropConstTask t = new NormConstAcceptPropConstTask();
//		t.setDestinations(points);
//		addTask(t);
		
//		addTask(new AcceptFactorTask(choiceSet));
//		DegreeDensityTask kRhoTask = new DegreeDensityTask(zones);
//		kRhoTask.setModule(new ObservedDegree());
//		addTask(kRhoTask);
	}
}
