/* *********************************************************************** *
 * project: org.matsim.*
 * ObservedAnalyzerTask.java
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

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.sna.gis.ZoneLayer;

import playground.johannes.socialnetworks.graph.analysis.AnalyzerTaskArray;
import playground.johannes.socialnetworks.graph.analysis.AnalyzerTaskComposite;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class ObservedAnalyzerTask extends AnalyzerTaskComposite {
	
	public ObservedAnalyzerTask(ZoneLayer zones, Set<Point> choiceSet, Network network, Geometry boundary) {
		AnalyzerTaskArray array = new AnalyzerTaskArray();
		array.addAnalyzerTask(new ObsTopologyAnalyzerTask(), "topo");
		array.addAnalyzerTask(new SnowballAnalyzerTask(), "snowball");
		array.addAnalyzerTask(new ObsSpatialAnalyzerTask(choiceSet, boundary), "spatial");
		array.addAnalyzerTask(new ObservedSocialAnalyzerTask(), "social");
		addTask(array);
		
		
		
//		
//		SpatialCostFunction costFunction = new GravityCostFunction(1.6, 0, new CartesianDistanceCalculator());
////		Accessibility accessibility = new Accessibility(costFunction, choiceSet);
//		SpatialPropertyDegreeTask spxkTask = new SpatialPropertyDegreeTask(costFunction, choiceSet);
//		spxkTask.setModule(new ObservedDegree());
//		spxkTask.setDiscretizer(new LinearDiscretizer(5.0));
//		addTask(spxkTask);
//		
//		SpatialPropertyAccessibilityTask spxaTask = new SpatialPropertyAccessibilityTask(costFunction, choiceSet);
//		spxaTask.setModule(new ObservedAccessibility());
//		spxaTask.setDiscretizer(new LinearDiscretizer(1.0));
//		addTask(spxaTask);
//		
//		SocialPropertyDegreeTask xkTask = new SocialPropertyDegreeTask();
//		xkTask.setDiscretizer(new LinearDiscretizer(5.0));
//		xkTask.setModule(new ObservedDegree());
//		addTask(xkTask);
//		
//
//		addTask(new AcceptFactorTask(choiceSet));
//		EdgeCostsTask costs = new EdgeCostsTask(null);
//		costs.setModule(new ObservedEdgeCosts(new GravityEdgeCostFunction(1.6, 0.0)));
//		addTask(costs);
		
//		
		
//		DegreeEdgeLengthTask kdTask = new DegreeEdgeLengthTask();
//		kdTask.setModule(new ObservedDegree());
//		addTask(kdTask);
//		
//		DegreeAgeTask daTask = new DegreeAgeTask();
//		daTask.setModule(new ObservedDegree());
//		addTask(daTask);
		
//		
//		
//		AccessibilityPartitioner partitioner = new AccessibilityPartitioner(costFunction, choiceSet);
//		partitioner.setModule(new ObservedAccessibility());
//		addTask(partitioner);
		
//		AccessibilityTask t = new AccessibilityTask(costFunction, choiceSet);
//		t.setModule(new ObservedAccessibility());
//		addTask(t);
////		DegreeAccessabilityTask task = new DegreeAccessabilityTask(choiceSet, costFunction);
//		task.setModule(new ObservedDegree());
//		addTask(task);
//		
//		DistanceAccessibilityTask distAccessTask = new DistanceAccessibilityTask(choiceSet, costFunction);
//		distAccessTask.setModule(new ObservedDistance());
//		addTask(distAccessTask);
		
//		addTask(new DensityAccessibilityTask(choiceSet, costFunction));
		
//		DegreeGridTask gridTask = new DegreeGridTask();
//		gridTask.setModule(new ObservedDegree());
//		addTask(gridTask);
		
//		addTask(new TravelTimeTask(network));
//		
//		addTask(new FrequencyTask());
//		addTask(new GenderTask());
//		
//		DegreeGenderTask kgTask = new DegreeGenderTask();
//		kgTask.setModule(new ObservedDegree());
//		addTask(kgTask);
		
//		
		
//		addTask(new FrequencyDistanceTask(choiceSet));
		
//		addTask(new EducationTask());
//		addTask(new EdgeTypeTask(choiceSet));
//		addTask(new DistanceSocioAttribute(choiceSet));
//		addTask(new IncomeTask());
//		addTask(new CivilStatusTask());
//		addTask(new playground.johannes.socialnetworks.survey.ivt2009.analysis.DegreeGenderTask());
//		addTask(new DistanceAge());
//		
//		addTask(new FrequencyDegree());
//		
//		addTask(new GyrationRadiusTask());
	}

}
