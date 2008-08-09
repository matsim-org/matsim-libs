/* *********************************************************************** *
 * project: org.matsim.*
 * RoadPricingScoringFunction.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.roadpricing;

import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.scoring.ScoringFunction;

/**
 * A special {@linkplain ScoringFunction scoring function} that takes the paid tolls by the agents into
 * account when calculating the score of a plan.
 *
 * @author mrieser
 */
public class RoadPricingScoringFunction implements ScoringFunction {

	private final CalcPaidToll paidToll;
	private final ScoringFunction scoringFunction;
	private final Person person;
	private double toll = 0.0;

	public RoadPricingScoringFunction(final Plan plan, final CalcPaidToll paidToll, final ScoringFunction scoringFunction) {
		this.paidToll = paidToll;
		this.scoringFunction = scoringFunction;
		this.person = plan.getPerson();
	}

	public void finish() {
		this.scoringFunction.finish();
		this.toll = this.paidToll.getAgentToll(this.person.getId().toString());
	}

	public void agentStuck(final double time) {
		this.scoringFunction.agentStuck(time);
	}

	public void addUtility(final double amount) {
		this.scoringFunction.addUtility(amount);
	}

	public void endActivity(final double time) {
		this.scoringFunction.endActivity(time);
	}

	public void endLeg(final double time) {
		this.scoringFunction.endLeg(time);
	}

	public double getScore() {
		return this.scoringFunction.getScore() - this.toll;
	}

	public void reset() {
		this.scoringFunction.finish();
	}

	public void startActivity(final double time, final Act act) {
		this.scoringFunction.startActivity(time, act);
	}

	public void startLeg(final double time, final Leg leg) {
		this.scoringFunction.startLeg(time, leg);
	}

}
