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

package playground.mmoyo.utils.calibration;

import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;

/**
 * return zero for all plans, excepts for "stayHome" type-plans for which the returned values is a little bit lower: -1e-9  

 */
public class NullifyingScoringFunctionFactoryStayHomeMinimalScore implements ScoringFunctionFactory {
	public NullifyingScoringFunctionFactoryStayHomeMinimalScore() {
	}
	
	@Override
	public ScoringFunction createNewScoringFunction(final Person person) {
		return new NullifyingScoringFunctionStayHomeMinimalScore(((PlanImpl)person).getType());
	}
}

class NullifyingScoringFunctionStayHomeMinimalScore implements ScoringFunction {
	protected double score;
	protected double startTime;
	final String strSTAY_HOME ="stayHome";
	
	String planType;
	
	public NullifyingScoringFunctionStayHomeMinimalScore(final String planType) {
		this.planType = planType;
	}
	
	@Override
	public void agentStuck(final double time) {}

	@Override
	public void addMoney(final double amount) {}

	@Override
	public void finish() {}

	@Override
	public double getScore() {
		score = !planType.equals(strSTAY_HOME)? 0.0 : -1e-9; 
		return score;
	}

	@Override
	public void handleActivity(Activity activity) {
	}

	@Override
	public void handleLeg(Leg leg) {}

	@Override
	public void handleEvent(Event event) {}
}