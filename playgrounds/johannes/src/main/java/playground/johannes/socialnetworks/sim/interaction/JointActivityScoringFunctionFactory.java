/* *********************************************************************** *
 * project: org.matsim.*
 * JointActivityScoringFunctionFactory.java
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
package playground.johannes.socialnetworks.sim.interaction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.charyparNagel.ActivityScoringFunction;
import org.matsim.core.scoring.charyparNagel.AgentStuckScoringFunction;
import org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory;
import org.matsim.core.scoring.charyparNagel.LegScoringFunction;
import org.matsim.core.scoring.charyparNagel.MoneyScoringFunction;

import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.SocialVertex;

/**
 * @author illenberger
 *
 */
public class JointActivityScoringFunctionFactory extends CharyparNagelScoringFunctionFactory {

	private VisitorTracker tracker;
	
	private Map<Person, SocialVertex> vertexMapping;

	private Map<Person, SocialSFAccumulator> accumulators;
	
	private Set<Person> egos;
	
	private final double beta_join;
	/**
	 * @param config
	 */
	public JointActivityScoringFunctionFactory(VisitorTracker tracker, SocialGraph graph, PlanCalcScoreConfigGroup config, double beta) {
		super(config);
		this.beta_join = beta;
		this.tracker = tracker;
		
		vertexMapping = new HashMap<Person, SocialVertex>();
		for(SocialVertex vertex : graph.getVertices()) {
			vertexMapping.put(vertex.getPerson().getPerson(), vertex);
		}
		
		accumulators = new HashMap<Person, SocialSFAccumulator>();
	}

	public void setCurrentEgos(Set<Person> egos) {
		this.egos = egos;
	}
	
	@Override
	public ScoringFunction createNewScoringFunction(Plan plan) {
		SocialSFAccumulator scoringFunctionAccumulator = accumulators.get(plan.getPerson());
		
		if(scoringFunctionAccumulator == null) {
			SocialVertex ego = vertexMapping.get(plan.getPerson());
			Set<Person> alters = new HashSet<Person>(ego.getNeighbours().size());
			for(SocialVertex alter : ego.getNeighbours()) {
				alters.add(alter.getPerson().getPerson());
			}
			
			scoringFunctionAccumulator = new SocialSFAccumulator(alters);
			
			scoringFunctionAccumulator.addScoringFunction(new ActivityScoringFunction(plan, getParams()));
			scoringFunctionAccumulator.addScoringFunction(new LegScoringFunction(plan, getParams()));
			scoringFunctionAccumulator.addScoringFunction(new MoneyScoringFunction(getParams()));
			scoringFunctionAccumulator.addScoringFunction(new AgentStuckScoringFunction(getParams()));
			
//			for(Person alter : alters) {
			for(SocialVertex alter : ego.getNeighbours()) {
				JointActivityScorer scorer = new JointActivityScorer(ego, alter, tracker, beta_join);
				scoringFunctionAccumulator.addScoringFunction(scorer, alter.getPerson().getPerson());
			}
			
			accumulators.put(plan.getPerson(), scoringFunctionAccumulator);
		}
		
		if (egos == null) {
			scoringFunctionAccumulator.setMode(ScoringMode.EGO);
		} else {
			if (egos.contains(plan.getPerson())) {
				scoringFunctionAccumulator.setMode(ScoringMode.EGO);
			} else {
				Person ego = isAlter(plan.getPerson());
				if (ego != null) {
					scoringFunctionAccumulator.setEgo(ego);
					scoringFunctionAccumulator.setMode(ScoringMode.ALTER);
				} else {
					scoringFunctionAccumulator.setMode(ScoringMode.UNAFFECTED);
				}
			}
		}
		scoringFunctionAccumulator.reset();
		
		return scoringFunctionAccumulator;
	}
	
	private Person isAlter(Person person) {
		SocialVertex vertex = vertexMapping.get(person);
		
		for(SocialVertex neighbour : vertex.getNeighbours()) {
			if(egos.contains(neighbour.getPerson().getPerson())) {
				return neighbour.getPerson().getPerson();
			}
		}
		
		return null;
	}
	
	public DescriptiveStatistics visitorStatistics() {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		for(SocialSFAccumulator accumulator : accumulators.values()) {
			stats.addValue(accumulator.visitors());
		}
		
		return stats;
	}

	public DescriptiveStatistics joinTimeStatistics() {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		for(SocialSFAccumulator accumulator : accumulators.values()) {
			for(double time : accumulator.joinTimes())
				stats.addValue(time);
		}
		
		return stats;
	}
	
	public DescriptiveStatistics socialScoreStatistics() {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		for(SocialSFAccumulator accumulator : accumulators.values()) {
			stats.addValue(accumulator.totalSocialScore());
		}
		
		return stats;
	}
}
