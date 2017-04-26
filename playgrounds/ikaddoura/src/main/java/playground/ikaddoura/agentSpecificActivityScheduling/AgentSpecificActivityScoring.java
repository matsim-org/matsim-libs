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
import org.matsim.core.scoring.functions.ScoringParameters;

/**
*
* The default {@link CharyparNagelActivityScoring} except that opening and closing times are taken from the person attributes and
* that the late arrival penalty which comes on top of the opportunity cost of time is computed taking the latest start time from the person attributes.
* 
* @author ikaddoura
*/

public class AgentSpecificActivityScoring implements org.matsim.core.scoring.SumScoringFunction.ActivityScoring {
		
	private final CharyparNagelActivityScoring delegate;
	private final AgentSpecificOpeningIntervalCalculator openingIntervalCalculator;
	private final CountActEventHandler actCounter;
	private final ScoringParameters parameters;
	private final Person person;
	private final double tolerance;
	
	private double lateArrivalScore = 0.;
		
	public AgentSpecificActivityScoring(ScoringParameters parameters, Person person, CountActEventHandler actCounter, double tolerance) {
		this.parameters = parameters;
		this.person = person;
		this.actCounter = actCounter;
		this.tolerance = tolerance;
		
		this.openingIntervalCalculator = new AgentSpecificOpeningIntervalCalculator(this.person, this.actCounter, this.tolerance);
		this.delegate = new CharyparNagelActivityScoring(this.parameters, this.openingIntervalCalculator);
	}

	@Override
	public void finish() {
		this.delegate.finish();
	}

	@Override
	public double getScore() {
		return this.delegate.getScore() + lateArrivalScore;
	}

	@Override
	public void handleFirstActivity(Activity act) {
		this.delegate.handleFirstActivity(act);
	}

	@Override
	public void handleActivity(Activity act) {
		this.delegate.handleActivity(act);
		this.lateArrivalScore += computeLateArrivalPenalty(act);
	}

	@Override
	public void handleLastActivity(Activity act) {
		this.delegate.handleLastActivity(act);
		this.lateArrivalScore += computeLateArrivalPenalty(act);
	}
	
	private double computeLateArrivalPenalty(Activity act) {
		double tmpScore = 0.;
		
		int activityCounter = this.actCounter.getActivityCounter(person.getId());
		
		// get the original start/end times from survey / initial demand which is written in the person attributes
		String activityOpeningIntervals = (String) person.getAttributes().getAttribute("OpeningClosingTimes");	
		String activityOpeningTimes[] = activityOpeningIntervals.split(";");
	
		double latestStartTime = Double.valueOf(activityOpeningTimes[activityCounter * 2]) + tolerance;
		
		if ((latestStartTime  >= 0) && (act.getStartTime() > latestStartTime)) {
			tmpScore += this.parameters.marginalUtilityOfLateArrival_s * (act.getStartTime() - latestStartTime);
		}
		
		return tmpScore ;
	}

}

