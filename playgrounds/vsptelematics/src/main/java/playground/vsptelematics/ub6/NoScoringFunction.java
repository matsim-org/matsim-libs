/* *********************************************************************** *
 * project: org.matsim.*
 * NoScoringFunction
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
package playground.vsptelematics.ub6;

import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.scoring.ScoringFunction;


/**
 * @author dgrether
 *
 */
public class NoScoringFunction implements ScoringFunction {

	private Plan plan;

	public NoScoringFunction(Plan plan) {
		this.plan = plan;
	}

	/**
	 * @see org.matsim.core.scoring.ScoringFunction#handleActivity(org.matsim.api.core.v01.population.Activity)
	 */
	@Override
	public void handleActivity(Activity activity) {
		// TODO Auto-generated method stub

	}

	/**
	 * @see org.matsim.core.scoring.ScoringFunction#handleLeg(org.matsim.api.core.v01.population.Leg)
	 */
	@Override
	public void handleLeg(Leg leg) {
		// TODO Auto-generated method stub

	}

	/**
	 * @see org.matsim.core.scoring.ScoringFunction#agentStuck(double)
	 */
	@Override
	public void agentStuck(double time) {
		// TODO Auto-generated method stub

	}

	/**
	 * @see org.matsim.core.scoring.ScoringFunction#addMoney(double)
	 */
	@Override
	public void addMoney(double amount) {
		// TODO Auto-generated method stub

	}

	/**
	 * @see org.matsim.core.scoring.ScoringFunction#finish()
	 */
	@Override
	public void finish() {
		// TODO Auto-generated method stub

	}

	/**
	 * @see org.matsim.core.scoring.ScoringFunction#getScore()
	 */
	@Override
	public double getScore() {
		double oldScore = this.plan.getScore();
		this.plan.setScore(null); //prevents msa in events2score
		return oldScore;
	}

	/**
	 * @see org.matsim.core.scoring.ScoringFunction#handleEvent(org.matsim.api.core.v01.events.Event)
	 */
	@Override
	public void handleEvent(Event event) {
		// TODO Auto-generated method stub

	}

}
