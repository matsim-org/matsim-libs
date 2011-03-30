/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityChoice2.java
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
package playground.johannes.socialnetworks.sim.locationChoice;

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TIntArrayList;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.contrib.sna.math.FixedSampleSizeDiscretizer;
import org.matsim.contrib.sna.math.Histogram;
import org.matsim.contrib.sna.util.TXTWriter;

import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.gis.DistanceCalculator;
import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.SocialVertex;

/**
 * @author illenberger
 *
 */
public class ActivityChoice2 implements PlanStrategyModule {

	private final String type = "leisure";
	
	private final Random random;
	
	private final Map<Person, SocialVertex> vertexMapping;
	
	private final ActivityMover mover;
//	
//	private DescriptiveStatistics dists;
//	
//	private DistanceCalculator distCalc = new CartesianDistanceCalculator();
	
	public ActivityChoice2(SocialGraph graph, ActivityMover mover, Random random) {
		vertexMapping = new HashMap<Person, SocialVertex>(graph.getVertices().size());
		for(SocialVertex vertex : graph.getVertices()) {
			vertexMapping.put(vertex.getPerson().getPerson(), vertex);
		}
		
		this.random = random;
		this.mover = mover;
	}
	
	@Override
	public void prepareReplanning() {
//		dists = new DescriptiveStatistics();
	}

	@Override
	public void handlePlan(Plan plan) {
		TIntArrayList indices = new TIntArrayList(plan.getPlanElements().size());
		/*
		 * retrieve all potential activity indices
		 */
		for(int i = 0; i < plan.getPlanElements().size(); i += 2) {
			Activity act = (Activity) plan.getPlanElements().get(i);
			if(type.equals(act.getType())) {
				indices.add(i);
			}
		}
		if (!indices.isEmpty()) {
			/*
			 * randomly select one index
			 */
			int idx = indices.get(random.nextInt(indices.size()));
			/*
			 * randomly draw new location
			 */
			SocialVertex v_i = vertexMapping.get(plan.getPerson());
			SocialVertex v_j = v_i.getNeighbours().get(random.nextInt(v_i.getNeighbours().size()));
			Id link = ((Activity) v_j.getPerson().getPerson().getPlans().get(0).getPlanElements().get(0)).getLinkId();
			/*
			 * move activity
			 */
			mover.moveActivity(plan, idx, link);

//			double d = distCalc.distance(v_i.getPoint(), v_j.getPoint());
//			dists.addValue(d);
		}
	}

	@Override
	public void finishReplanning() {
//		TDoubleDoubleHashMap hist = Histogram.createHistogram(dists, FixedSampleSizeDiscretizer.create(dists.getValues(), 1, 50), true);
//		try {
//			TXTWriter.writeMap(hist, "d", "p", "/Users/jillenberger/Work/socialnets/locationChoice/output/choice.txt");
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

}
