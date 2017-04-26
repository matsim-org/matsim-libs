/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.mrieser.svi.controller;

import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.deprecated.scoring.ScoringFunctionAccumulator;
import org.matsim.deprecated.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.deprecated.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.deprecated.scoring.functions.CharyparNagelLegScoring;
import org.matsim.deprecated.scoring.functions.CharyparNagelMoneyScoring;

public class MixedScoringFunction implements ScoringFunction {

	private final ScoringFunction delegate;
	
	public MixedScoringFunction(final ScoringParameters params) {
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(params));
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, null));
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelMoneyScoring(params));
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));
		this.delegate = scoringFunctionAccumulator;
	}

	@Override
	public void handleActivity(Activity activity) {
		this.delegate.handleActivity(activity);
	}

	@Override
	public void handleLeg(Leg leg) {
		this.delegate.handleLeg(leg);
	}

	@Override
	public void agentStuck(double time) {
		this.delegate.agentStuck(time);
	}

	@Override
	public void addMoney(double amount) {
		this.delegate.addMoney(amount);
	}

	@Override
	public void finish() {
		this.delegate.finish();
	}

	@Override
	public double getScore() {
		// TODO adapt score based on DynusT
		throw new RuntimeException("not yet implemented");
//		return this.delegate.getScore();
	}


	@Override
	public void handleEvent(Event event) {
		// TODO Auto-generated method stub
		
	}

}
