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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PersonImpl;

import playground.johannes.coopsim.mental.choice.ActivityGroupSelector;
import playground.johannes.coopsim.mental.choice.ChoiceSelector;
import playground.johannes.coopsim.mental.planmod.Choice2ModAdaptor;
import playground.johannes.coopsim.mental.planmod.PlanModEngine;
import playground.johannes.coopsim.mental.planmod.SingleThreadedModEngine;
import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.SocialVertex;

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
	
//	private long choiceTime;
//	
//	private long modTime;
//	
//	private long prepareModTime;
	
	public MentalEngine(SocialGraph graph, ChoiceSelector choiceSelector, Choice2ModAdaptor adaptor, Random random) {
		this.choiceSelector = choiceSelector;
		this.random = random;
		
		modEngine = new SingleThreadedModEngine(adaptor);
	}
	
	public MentalEngine(SocialGraph graph, ChoiceSelector choiceSelector, PlanModEngine modEngine, Random random) {
		this.choiceSelector = choiceSelector;
		this.random = random;		
		this.modEngine = modEngine;
	}
	
	public List<SocialVertex> nextState() {
//		long time = System.currentTimeMillis();
		/*
		 * make choices
		 */
		Map<String, Object> choices = new HashMap<String, Object>();
		choices = choiceSelector.select(choices);
		
//		choiceTime += System.currentTimeMillis() - time;
		/*
		 * get plans do modify
		 */
//		time = System.currentTimeMillis();
		@SuppressWarnings("unchecked")
		List<SocialVertex> egos = (List<SocialVertex>) choices.get(ActivityGroupSelector.KEY);
		List<Plan> plans = new ArrayList<Plan>(egos.size());
		for(SocialVertex v : egos) {
			Plan plan = v.getPerson().getPerson().copySelectedPlan();
			if(plan == null)
				throw new NullPointerException("Outch! This person appears to have no selected plan!");
			plans.add(plan);
		}
//		prepareModTime += System.currentTimeMillis() - time;
		/*
		 * apply modifications
		 */
//		time = System.currentTimeMillis();
		modEngine.run(plans, choices);
//		modTime += System.currentTimeMillis() - time;
		return egos;
	}
	
	public boolean acceptRejectState(Collection<SocialVertex> egos) {
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
		
		double oldScore = 0;
		for(int i = 0; i < oldState.size(); i++)
			oldScore += oldState.get(i).getScore();
		/*
		 * calculate transition probability
		 */
		double delta = oldScore - newScore;
		double pi = 1 / (1 + Math.exp(delta));
//		if(Double.isNaN(pi))
//			System.err.println();
		piSum += pi;
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
			PersonImpl person = (PersonImpl) plan.getPerson();
			person.getPlans().remove(plan);
			person.setSelectedPlan(person.getPlans().get(0));
		}
		
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
		
//		System.out.println(String.format("Choice time = %1$s, modifier time = %2$s, prepare mod time = %3$s.", choiceTime, modTime, prepareModTime));
//		choiceTime = 0;
//		modTime = 0;
		
	}
}
