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

import playground.johannes.socialnetworks.graph.analysis.AnalyzerTaskComposite;
import playground.johannes.socialnetworks.graph.spatial.analysis.AcceptanceProbabilityTask;
import playground.johannes.socialnetworks.graph.spatial.analysis.DistanceTask;
import playground.johannes.socialnetworks.snowball2.spatial.analysis.ObservedDistance;

import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class ObsSpatialAnalyzerTask extends AnalyzerTaskComposite {
	
	public ObsSpatialAnalyzerTask(Set<Point> points) {
		DistanceTask distanceTask = new DistanceTask();
		distanceTask.setModule(ObservedDistance.getInstance());
		addTask(distanceTask);
		
		AcceptanceProbabilityTask acceptTask = new AcceptanceProbabilityTask(points);
		acceptTask.setModule(ObservedAcceptanceProbability.getInstance());
		addTask(acceptTask);
		
//		addTask(new AcceptFactorTask(choiceSet));
//		DegreeDensityTask kRhoTask = new DegreeDensityTask(zones);
//		kRhoTask.setModule(new ObservedDegree());
//		addTask(kRhoTask);
	}
}
