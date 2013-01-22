/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.pbouman.crowdedness;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.scoring.ScoringFunction;

/**
 * @author nagel
 *
 */
public class CrowdedScoringFunction implements ScoringFunction {
	
	ScoringFunction delegate ;
	private double score;
	
	public void addMoney(double amount) {
		delegate.addMoney(amount);
	}

	public void agentStuck(double time) {
		delegate.agentStuck(time);
	}

	public void finish() {
		delegate.finish();
	}

	public double getScore() {
		return this.score+ delegate.getScore();
	}

	public void handleActivity(Activity activity) {
		delegate.handleActivity(activity);
	}

	public void handleLeg(Leg leg) {
		delegate.handleLeg(leg);
	}

	public void reset() {
		delegate.reset();
		this.score = 0. ;
	}

	public CrowdedScoringFunction(ScoringFunction delegate) {
//		delegate = new CharyparNagelScoringFunctionFactory(config, network).createNewScoringFunction(plan) ;
	}

	@Override
	public void handleEvent(Event event) {
		if ( event instanceof PersonCrowdednessEvent ) {
			//...
			this.score += -1.0 ;
			// unit of this is utils.  1 util is very approximately one Euro.
		}

	}

}
