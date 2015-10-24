/* *********************************************************************** *
 * project: org.matsim.*
 * MentalEngine.java
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
package playground.johannes.coopsim.mental;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.utils.collections.Tuple;
import playground.johannes.coopsim.mental.choice.ActivityGroupSelector;
import playground.johannes.coopsim.mental.choice.ChoiceSelector;
import playground.johannes.coopsim.mental.planmod.Choice2ModAdaptor;
import playground.johannes.coopsim.mental.planmod.PlanModEngine;
import playground.johannes.coopsim.mental.planmod.SingleThreadedModEngine;
import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.SocialVertex;

import java.util.*;

/**
 * @author illenberger
 *
 */
public class MentalEngine {

	private final Random random;
	
	private final ChoiceSelector choiceSelector;
	
	private final PlanModEngine modEngine;
	
	private int acceptedStates;
	
	private double piSum;
	
	private double totalPiSum;
	
	private final boolean includeAlters;
	
	private final double beta_start = 100;
	
	private final double beta_target = 1;
	
	private final double beta_burnin = 0;
	
	private long iterCounter = 0;
	
	public MentalEngine(SocialGraph graph, ChoiceSelector choiceSelector, Choice2ModAdaptor adaptor, Random random, boolean includeAlters) {
		this.choiceSelector = choiceSelector;
		this.random = random;
		this.includeAlters = includeAlters;
		modEngine = new SingleThreadedModEngine(adaptor);
	}
	
	public MentalEngine(SocialGraph graph, ChoiceSelector choiceSelector, PlanModEngine modEngine, Random random, boolean includeAlters) {
		this.choiceSelector = choiceSelector;
		this.random = random;		
		this.modEngine = modEngine;
		this.includeAlters = includeAlters;
	}
	
	public List<SocialVertex> nextState() {
		/*
		 * make choices
		 */
		Map<String, Object> choices = new HashMap<String, Object>();
		choices = choiceSelector.select(choices);
		/*
		 * get plans do modify
		 */
		@SuppressWarnings("unchecked")
		List<SocialVertex> egos = (List<SocialVertex>) choices.get(ActivityGroupSelector.KEY);
		List<Plan> plans = new ArrayList<Plan>(egos.size());
		for(SocialVertex v : egos) {
			Plan plan = v.getPerson().getPerson().createCopyOfSelectedPlanAndMakeSelected();
			if(plan == null)
				throw new NullPointerException("Outch! This person appears to have no selected plan!");
			plans.add(plan);
		}
		/*
		 * apply modifications
		 */
		modEngine.run(plans, choices);

		return egos;
	}
	
	public boolean acceptRejectState(Collection<SocialVertex> egos, List<Tuple<Plan, Double>> alter1Scores) {
		boolean accept;
		/*
		 * get new and old plans
		 */
		List<Plan> newState = new ArrayList<Plan>(egos.size());
		List<Plan> oldState = new ArrayList<Plan>(egos.size());
		
		for(SocialVertex ego : egos) {
			Person person = ego.getPerson().getPerson();
			if(person.getPlans().get(0).isSelected()) {
				newState.add(person.getPlans().get(0));
				oldState.add(person.getPlans().get(1));
			} else {
				newState.add(person.getPlans().get(1));
				oldState.add(person.getPlans().get(0));
			}
		}
		/*
		 * get scores
		 */
		double newScore = 0;
		for(int i = 0; i < newState.size(); i++)
			newScore +=  newState.get(i).getScore();
		
		if(includeAlters) {
			for(Tuple<Plan, Double> tuple : alter1Scores) {
				newScore += tuple.getFirst().getScore();
			}
		}
		
		double oldScore = 0;
		for(int i = 0; i < oldState.size(); i++)
			oldScore += oldState.get(i).getScore();
		
		if(includeAlters) {
			for(Tuple<Plan, Double> tuple : alter1Scores) {
				oldScore += tuple.getSecond();
			}
		}
		/*
		 * calculate transition probability
		 */
		double beta = 1.0;
		if(iterCounter < beta_burnin) {
			beta = (beta_target - beta_start)/beta_burnin * iterCounter + beta_start;
		}
		double delta = oldScore - newScore;
		double pi = 1 / (1 + Math.exp(beta * delta));

		piSum += pi;
		totalPiSum += pi;
		/*
		 * accept/reject
		 */
		List<Plan> remove = null;
		if(random.nextDouble() < pi) {
			/*
			 * accept state
			 */
			remove = oldState;
			acceptedStates++;
			accept = true;
		} else {
			/*
			 * reject state
			 */
			remove = newState;
			accept = false;
		}
		/*
		 * remove plans
		 */
		for(int i = 0; i < remove.size(); i++) {
			Plan plan = remove.get(i);
			Person person = plan.getPerson();
			person.getPlans().remove(plan);
			person.setSelectedPlan(person.getPlans().get(0));
		}
		
		iterCounter++;
		
		return accept;
	}
	
	public int getAcceptedStates() {
		return acceptedStates;
	}
	
	public double getTransitionProbaSum() {
		return piSum;
	}
	
	public void clearStatistics() {
		acceptedStates = 0;
		piSum = 0;
	}
	
	public double getTotalPiSum() {
		return totalPiSum;
	}
	
	public void cleatTotalPiSum() {
		totalPiSum = 0;
	}
	
	public void finalize() {
		try {
			super.finalize();
		} catch (Throwable e) {
			e.printStackTrace();
		}
		modEngine.finalize();
	}
}
