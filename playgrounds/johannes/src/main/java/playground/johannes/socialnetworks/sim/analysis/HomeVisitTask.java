/* *********************************************************************** *
 * project: org.matsim.*
 * HomeVisitTask.java
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
package playground.johannes.socialnetworks.sim.analysis;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.sna.math.DummyDiscretizer;

import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.SocialVertex;
import playground.johannes.socialnetworks.sim.interaction.VisitorTracker;

/**
 * @author illenberger
 *
 */
public class HomeVisitTask extends TrajectoryAnalyzerTask {

	private static final Logger logger = Logger.getLogger(HomeVisitTask.class);
	
	private VisitorTracker tracker;
	
	private Map<Person, SocialVertex> vertexMapping;
	
	public HomeVisitTask(VisitorTracker tracker, SocialGraph graph) {
		this.tracker = tracker;
		
		vertexMapping = new HashMap<Person, SocialVertex>();
		for(SocialVertex v : graph.getVertices()) {
			vertexMapping.put(v.getPerson().getPerson(), v);
		}
	}
	
	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		DescriptiveStatistics homeStats = new DescriptiveStatistics();
		int homeActs = 0;
		int hostNotAtHome = 0;
		
		for(Trajectory trajectory : trajectories) {
			Activity home = (Activity) trajectory.getElements().get(0);
			Activity act = (Activity) trajectory.getElements().get(2);
			Set<Person> alters = new HashSet<Person>();
			SocialVertex ego = vertexMapping.get(trajectory.getPerson());
			for(SocialVertex vertex : ego.getNeighbours()) {
				alters.add(vertex.getPerson().getPerson());
			}
			
			if(home.getFacilityId().equals(act.getFacilityId())) {
				homeActs++;
				int sum = 0;
				for(Person alter : alters) {
					double time = tracker.timeOverlap(trajectory.getPerson(), alter);
					if(time > 0)
						sum++;
				}
				
				homeStats.addValue(sum);
			} else {
				boolean meet = false;
				boolean ishome = false;
				for(Person alter : alters) {
					double time = tracker.timeOverlap(trajectory.getPerson(), alter);
					if(time > 0) {
						meet = true;
						Activity alterHome = (Activity) alter.getSelectedPlan().getPlanElements().get(0);
						if(act.getFacilityId().equals(alterHome.getFacilityId())) {
							ishome = true;
						} else {
							ishome = false;
						}
					} else {
						meet = false;
					}
					
					if(meet && ishome)
						break;
				}
				
				if(meet && !ishome) {
					hostNotAtHome++;
				}
				
				int visitedHomeAlters = 0;
				for(Person alter : alters) {
					double time = tracker.timeOverlap(trajectory.getPerson(), alter);
					if(time > 0) {
						Activity alterHome = (Activity) alter.getSelectedPlan().getPlanElements().get(0);
						Activity alterLeisure = (Activity) alter.getSelectedPlan().getPlanElements().get(2);
						if(alterHome.getFacilityId().equals(alterLeisure.getFacilityId()))
							visitedHomeAlters++;
					}
				}
				if(visitedHomeAlters > 1) {
					logger.warn(String.format("Visited %1$s alters with one trip!", visitedHomeAlters));
				}
					
			}
		}
		
		logger.info(String.format("%1$s persones stayed at home.", homeActs));
		try {
			writeHistograms(homeStats, new DummyDiscretizer(), "homeVisits", false);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		logger.info(String.format("%1$s hosts not at home.", hostNotAtHome));
	}

}
