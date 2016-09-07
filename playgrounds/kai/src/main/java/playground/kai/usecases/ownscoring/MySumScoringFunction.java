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
package playground.kai.usecases.ownscoring;

import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.SumScoringFunction.BasicScoring;

/**
 * @author nagel
 *
 */
public class MySumScoringFunction implements ScoringFunction {
	
	private SumScoringFunction delegate = new SumScoringFunction() ;
	private SumScoringFunction pDelegate = new SumScoringFunction() ;
	
	@Override
	public final void handleActivity(Activity activity) {
		double pScore = pDelegate.getScore();
		double score = delegate.getScore() ;
		
		delegate.handleActivity(activity);
		
		Activity pseudoActivity = PopulationUtils.createActivity(activity) ;
		pseudoActivity.setStartTime( pseudoActivity.getStartTime() - 1 ) ;
		pDelegate.handleActivity(pseudoActivity);
		
		// score change by regular activity
		double diff = delegate.getScore() - score ;
		
		// score change by pseudo activity
		double pDiff = pDelegate.getScore() - pScore ;
		
		// score diff caused by one second
		
		double diff1 = pDiff - diff ;
		
	}

	@Override
	public final void handleLeg(Leg leg) {
		delegate.handleLeg(leg);
		pDelegate.handleLeg(leg);
	}

	@Override
	public void addMoney(double amount) {
		delegate.addMoney(amount);
		pDelegate.addMoney(amount);
	}

	@Override
	public void agentStuck(double time) {
		delegate.agentStuck(time);
		pDelegate.agentStuck(time);
	}

	@Override
	public void handleEvent(Event event) {
		delegate.handleEvent(event);
		pDelegate.handleEvent(event);
	}

	@Override
	public void finish() {
		delegate.finish();
		pDelegate.finish();
	}

	@Override
	public double getScore() {
		return delegate.getScore() ;
	}

	public void addScoringFunction(BasicScoring scoringFunction) {
		delegate.addScoringFunction(scoringFunction);
		pDelegate.addScoringFunction(scoringFunction);
	}

}
