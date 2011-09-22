/* *********************************************************************** *
 * project: org.matsim.*
 * EvalEngine.java
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
package playground.johannes.coopsim.eval;

import gnu.trove.TObjectDoubleHashMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.events.handler.EventHandler;

import playground.johannes.coopsim.pysical.Trajectory;
import playground.johannes.coopsim.pysical.VisitorTracker;
import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.SocialVertex;


/**
 * @author illenberger
 * 
 */
public class EvalEngine {
	
//	private final EventsToScore scorer;

//	private final JointActivityScoringFactory factory;

	private final Evaluator evaluator;
	
	public EvalEngine(SocialGraph graph, VisitorTracker tracker, PlanCalcScoreConfigGroup config, double beta_join, Map<String, Map<SocialVertex, Double>> desiredDurations, Map<String, Double> priorities) {
//		factory = new JointActivityScoringFactory(graph, tracker, config, beta_join);
//		
//		Population pop = new PopulationImpl(null);
//		for(SocialVertex v : graph.getVertices())
//			pop.addPerson(v.getPerson().getPerson());
//		
//		scorer = new EventsToScore(pop, factory);
		
		Map<String, Map<Person, Double>> durations = new HashMap<String, Map<Person, Double>>();
		for(Entry<String, Map<SocialVertex, Double>> entry : desiredDurations.entrySet()) {
			Map<Person, Double> map = new HashMap<Person, Double>();
			for(Entry<SocialVertex, Double> entry2 : entry.getValue().entrySet()) {
				map.put(entry2.getKey().getPerson().getPerson(), entry2.getValue());
			}
			durations.put(entry.getKey(), map);
		}
		
		double beta_act = config.getPerforming_utils_hr() / 3600.0;
		double beta_leg = config.getTraveling_utils_hr() / 3600.0;
		
		evaluator = new EvaluatorComposite();
		((EvaluatorComposite)evaluator).addComponent(new LegEvaluator(beta_leg));
		((EvaluatorComposite)evaluator).addComponent(new ActivityEvaluator(beta_act, durations, priorities));
		((EvaluatorComposite)evaluator).addComponent(new JointActivityEvaluator(beta_join));
		((EvaluatorComposite)evaluator).addComponent(new ActivityOvertimeEvaluator(10, durations));
	}
	
	public List<EventHandler> getEventHandler() {
		List<EventHandler> handlers = new ArrayList<EventHandler>(2);
//		handlers.add(scorer);
//		handlers.add(tracker);
		return handlers;
	}

	public void init() {
//		scorer.reset(0);
//		factory.resetAccumulators();
//		tracker.reset(0);
	}

	public void run() {
//		scorer.finish();
	}
	
	public void evaluate(Set<Trajectory> trajectories) {
		for(Trajectory t : trajectories) {
			double score = evaluator.evaluate(t);
			
			t.getPerson().getSelectedPlan().setScore(score);
		}
	}
	
	public TObjectDoubleHashMap<Person> getJointActivityScores() {
//		Map<SocialVertex, JointActivityScoring> scorers = factory.getJointActivityScorers();
//		TObjectDoubleHashMap<Person> values = new TObjectDoubleHashMap<Person>(scorers.size());
//		
//		for(Entry<SocialVertex, JointActivityScoring> entry : scorers.entrySet()) {
//			values.put(entry.getKey().getPerson().getPerson(), entry.getValue().getScore());
//		}
//		
//		return values;
		return null;
	}
}
