/* *********************************************************************** *
 * project: org.matsim.*
 * OnlyTimeDependentScoringFunction.java
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

package org.matsim.core.scoring.functions;

import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.scoring.ScoringFunction;

/**
 * A Scoring Function that only respects the travel time.
 * 
 * @author cdobler
 */
public class OnlyTravelTimeDependentScoringFunction implements ScoringFunction {
	
	private double score;

	public OnlyTravelTimeDependentScoringFunction() {
	}

	@Override
	public void handleActivity(Activity activity) {

	}

	@Override
	public void handleLeg(Leg leg) {
		score -= leg.getTravelTime().seconds();
	}

	@Override
	public void agentStuck(final double time) {
	}

	@Override
	public void addMoney(final double amount) {
	}

	@Override
	public void addScore(final double amount) {
	}


	@Override
	public void finish() {
		
	}

	@Override
	public double getScore() {
		return score;
	}

	public void reset() {		
		score = 0.0;
	}

	@Override
	public void handleEvent(Event event) {

	}
	
}
