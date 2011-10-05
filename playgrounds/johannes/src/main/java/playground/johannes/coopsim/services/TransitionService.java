/* *********************************************************************** *
 * project: org.matsim.*
 * TransitionService.java
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
package playground.johannes.coopsim.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PersonImpl;

import playground.johannes.socialnetworks.graph.social.SocialVertex;

/**
 * @author illenberger
 *
 */
public class TransitionService implements SimService<Boolean> {

	private final StateService stateService;
	
	private final Random random;
	
	private Boolean accept;
	
	public TransitionService(StateService stateService, Random random) {
		this.stateService = stateService;
		this.random = random;
	}
	
	@Override
	public void init() {
	}

	@Override
	public void run() {
		List<SocialVertex> egos = stateService.get();
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

//		piSum += pi;
		/*
		 * accept/reject
		 */
		List<Plan> remove = null;
		if(random.nextDouble() < pi) {
			/*
			 * accept state
			 */
			remove = oldState;
//			acceptedStates++;
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
		
	}

	@Override
	public Boolean get() {
		return accept;
	}

	@Override
	public void terminate() {
	}

}
