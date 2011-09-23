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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;

import playground.johannes.coopsim.pysical.Trajectory;
import playground.johannes.coopsim.pysical.VisitorTracker;
import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.SocialVertex;


/**
 * @author illenberger
 * 
 */
public class EvalEngine {
	
	private final Evaluator evaluator;
	
	public EvalEngine(SocialGraph graph, VisitorTracker tracker, PlanCalcScoreConfigGroup config, double beta_join, Map<String, Map<SocialVertex, Double>> desiredDurations, Map<String, Double> priorities) {

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
		((EvaluatorComposite)evaluator).addComponent(new JointActivityEvaluator(beta_join, tracker, graph));
		((EvaluatorComposite)evaluator).addComponent(new ActivityOvertimeEvaluator(10, durations));
	}
	

	public void evaluate(Set<Trajectory> trajectories) {
		for(Trajectory t : trajectories) {
			double score = evaluator.evaluate(t);
			
			t.getPerson().getSelectedPlan().setScore(score);
		}
	}
}
