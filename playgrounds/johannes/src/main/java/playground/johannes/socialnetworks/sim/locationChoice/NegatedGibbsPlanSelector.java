/* *********************************************************************** *
 * project: org.matsim.*
 * GibbsPlanSelector.java
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

import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.replanning.selectors.PlanSelector;

/**
 * @author illenberger
 *
 */
public class NegatedGibbsPlanSelector implements PlanSelector, IterationStartsListener, IterationEndsListener {

	private static final Logger logger = Logger.getLogger(NegatedGibbsPlanSelector.class);
	
	private final double beta_join;
	
	private final Random random;
	
	private double scoreAccept;
	
	private double scoreReject;
	
	private int cntAccept;
	
	private int cntReject;
	
	public NegatedGibbsPlanSelector(double beta, Random random) {
		this.beta_join = beta;
		this.random = random;
	}
	
	@Override
	public Plan selectPlan(Person person) {
		
		if(person.getPlans().size() > 2)
			throw new IllegalArgumentException("Person has more than two plans!");
		
		Plan newPlan;
		Plan oldPlan;
		
		if(person.getPlans().get(0).isSelected()) {
			newPlan = person.getPlans().get(0);
			oldPlan = person.getPlans().get(1);
		} else if(person.getPlans().get(1).isSelected()) {
			newPlan = person.getPlans().get(1);
			oldPlan = person.getPlans().get(0);
		} else {
			throw new IllegalArgumentException("No selected plan!");
		}
		
		double newScore = newPlan.getScore();
		double oldScore = Double.NEGATIVE_INFINITY;
		if(oldPlan.getScore() != null)
			oldScore = oldPlan.getScore();
		
		
		double p = 1 / (1 + Math.exp(beta_join * (oldScore - newScore)));
		
		if(random.nextDouble() < p) {
			/*
			 * accept, i.e., remove the old plan
			 */
			cntAccept++;
			scoreAccept += oldPlan.getScore();
			return oldPlan;
		} else {
			/*
			 * reject, i.e., remove the new plan
			 */
			cntReject++;
			scoreReject += newPlan.getScore();
			return newPlan;
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		logger.info(String.format("Score of accepted plans: %1$s. (%2$s)", scoreAccept/(double)cntAccept, cntAccept));
		logger.info(String.format("Score of rejected plans: %1$s. (%2$s)", scoreReject/(double)cntReject, cntReject));
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		scoreReject = 0;
		scoreAccept = 0;
		cntAccept = 0;
		cntReject = 0;
	}

}
