/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.vsp.congestion.handlers;

import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.SumScoringFunction.BasicScoring;

/**
 * @author ikaddoura
 *
 */
public class MarginalSumScoringFunction implements ScoringFunction {
	
	private SumScoringFunction delegate = new SumScoringFunction() ;
	
	public final double getActivityDelayDisutility(Activity activity, double delay) {
	
		Activity pseudoActivityWithoutDelay = new ActivityImpl(activity);
		pseudoActivityWithoutDelay.setStartTime(activity.getStartTime() - delay);
				
		delegate.handleActivity(activity);
		double score1 = delegate.getScore();
		
		delegate.handleActivity(pseudoActivityWithoutDelay);
		double score2 = delegate.getScore();
		
		double activityDelayDisutility = score2 - score1;
		
		return activityDelayDisutility;
	}
	
	@Override
	public final void handleActivity(Activity activity) {
		delegate.handleActivity(activity);
	}

	@Override
	public final void handleLeg(Leg leg) {
		throw new RuntimeException("Not implemented. Aborting...");
	}

	@Override
	public void addMoney(double amount) {
		throw new RuntimeException("Not implemented. Aborting...");
	}

	@Override
	public void agentStuck(double time) {
		throw new RuntimeException("Not implemented. Aborting...");
	}

	@Override
	public void handleEvent(Event event) {
		throw new RuntimeException("Not implemented. Aborting...");
	}

	@Override
	public void finish() {
		throw new RuntimeException("Not implemented. Aborting...");
		// maybe I need this?
//		delegate.finish();
	}

	@Override
	public double getScore() {
		throw new RuntimeException("Not implemented. Aborting...");
	}

	public void addScoringFunction(BasicScoring scoringFunction) {
		delegate.addScoringFunction(scoringFunction);
	}

}
