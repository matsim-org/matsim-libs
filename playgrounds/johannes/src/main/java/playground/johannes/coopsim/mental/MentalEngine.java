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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PersonImpl;

import playground.johannes.coopsim.mental.choice.ActivityGroupSelector;
import playground.johannes.coopsim.mental.choice.ChoiceSelector;
import playground.johannes.coopsim.mental.planmod.Choice2ModAdaptor;
import playground.johannes.coopsim.mental.planmod.PlanModifier;
import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.SocialVertex;

/**
 * @author illenberger
 *
 */
public class MentalEngine {

	private final Random random;
	
	private final ChoiceSelector choiceSelector;
	
	private final Choice2ModAdaptor adaptor;
	
	private int acceptedStates;
	
	private double piSum;
	
	public MentalEngine(SocialGraph graph, ChoiceSelector choiceSelector, Choice2ModAdaptor adaptor, Random random) {
		this.choiceSelector = choiceSelector;
		this.adaptor = adaptor;
		this.random = random;
	}
	
	public Set<SocialVertex> nextState() {
		/*
		 * make choices
		 */
		Map<String, Object> choices = new HashMap<String, Object>();
		choices = choiceSelector.select(choices);
		/*
		 * convert choices to plan modifiers 
		 */
		PlanModifier mod = adaptor.convert(choices);
		/*
		 * get plans do modify
		 */
		@SuppressWarnings("unchecked")
		Set<SocialVertex> egos = (Set<SocialVertex>) choices.get(ActivityGroupSelector.KEY);
		Set<Plan> plans = new HashSet<Plan>();
		for(SocialVertex v : egos) {
			Plan plan = v.getPerson().getPerson().copySelectedPlan();
			if(plan == null)
				throw new NullPointerException("Outch! This person appears to have no selected plan!");
			plans.add(plan);
		}
		/*
		 * apply modifications
		 */
		for(Plan plan : plans)
			mod.apply(plan);
		
		return egos;
	}
	
	public boolean acceptRejectState(Set<SocialVertex> egos) {
		boolean accept;
		/*
		 * get new and old plans
		 */
		Set<Plan> newState = new HashSet<Plan>();
		Set<Plan> oldState = new HashSet<Plan>();
		
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
		for(Plan plan : newState)
			newScore +=  plan.getScore();
		
		double oldScore = 0;
		for(Plan plan : oldState)
			oldScore += plan.getScore();
		/*
		 * calculate transition probability
		 */
		double delta = oldScore - newScore;
		double pi = 1 / (1 + Math.exp(delta));
		piSum += pi;
		/*
		 * accept/reject
		 */
		Set<Plan> remove = null;
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
		for(Plan plan : remove) {
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
	}
}
