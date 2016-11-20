/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.agentSpecificActivityScheduling;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

/**
* @author ikaddoura
*/

public class AgentSpecificActivityScoring implements org.matsim.core.scoring.SumScoringFunction.ActivityScoring {
		
	private final CharyparNagelActivityScoring delegate;
	
	private final AgentSpecificOpeningIntervalCalculator openingIntervalCalculator;
	
	public AgentSpecificActivityScoring(CharyparNagelScoringParameters parameters, Person person, CountActEventHandler actCount, double tolerance) {
		openingIntervalCalculator = new AgentSpecificOpeningIntervalCalculator(person, actCount, tolerance);
		this.delegate = new CharyparNagelActivityScoring(parameters, openingIntervalCalculator);
	}

	@Override
	public void finish() {
		this.delegate.finish();
	}

	@Override
	public double getScore() {
		return this.delegate.getScore();
	}

	@Override
	public void handleFirstActivity(Activity act) {
		this.delegate.handleFirstActivity(act);
	}

	@Override
	public void handleActivity(Activity act) {
		this.delegate.handleActivity(act);
	}

	@Override
	public void handleLastActivity(Activity act) {
		this.delegate.handleLastActivity(act);
	}

}

