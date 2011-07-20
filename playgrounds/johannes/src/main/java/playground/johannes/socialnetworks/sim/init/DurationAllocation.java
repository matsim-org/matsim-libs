/* *********************************************************************** *
 * project: org.matsim.*
 * DurationAllocation.java
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
package playground.johannes.socialnetworks.sim.init;

import java.util.Random;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;

/**
 * @author illenberger
 *
 */
public class DurationAllocation implements PlanStrategyModule {
	
	private UnivariateRealFunction pdf;

	private Random random;
	
	private int maxDuration;
	
	public DurationAllocation(UnivariateRealFunction pdf, int maxDuration, Random random) {
		this.pdf = pdf;
		this.maxDuration = maxDuration;
		this.random = random;
	}
	
	@Override
	public void prepareReplanning() {
	}

	@Override
	public void handlePlan(Plan plan) {
		if(plan.getPlanElements().size() > 5)
			throw new IllegalArgumentException("This plan has more than three activities.");
		else {
			boolean accept = false;
			while(!accept) {
				try {
					int t = random.nextInt(maxDuration);
					double p = pdf.value(t);
					
					if(p >= random.nextDouble()) {
						Activity current = (Activity) plan.getPlanElements().get(2);
						Leg fromLeg = (Leg) plan.getPlanElements().get(3);
						Activity next = (Activity) plan.getPlanElements().get(4);
						
						double fromTravTime = next.getStartTime() - current.getEndTime();
						
						current.setEndTime(current.getStartTime() + t);
						fromLeg.setDepartureTime(current.getEndTime());
						next.setStartTime(current.getEndTime() + fromTravTime);
						
						accept = true;
					}
				} catch (FunctionEvaluationException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void finishReplanning() {
	}

}
