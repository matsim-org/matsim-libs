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

package org.matsim.contrib.locationchoice.bestresponse;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.SumScoringFunction;

public class DCActivityWOFacilitiesScoringFunction implements SumScoringFunction.ActivityScoring {
	static final Logger log = Logger.getLogger(DCActivityWOFacilitiesScoringFunction.class);	
	private DestinationScoring destinationChoiceScoring;	
	private double score = 0. ;
	private final Person person;
	
	public DCActivityWOFacilitiesScoringFunction(Person person, DestinationChoiceContext lcContext) {
		this.destinationChoiceScoring = new DestinationScoring(lcContext);
		this.person = person ; 
	}
	
	@Override
	public void finish() {		
	}

	@Override
	public double getScore() {
		return this.score ;
	}
	
	private int activityIndex = 0 ;

	@Override
	public void handleFirstActivity(Activity act) {
		activityIndex = 0 ;
		this.score += destinationChoiceScoring.getDestinationScore(act, BestReplyLocationChoiceStrategyModule.useScaleEpsilonFromConfig, activityIndex, person.getId() );
	}

	@Override
	public void handleActivity(Activity act) {
		activityIndex++ ;
		this.score += destinationChoiceScoring.getDestinationScore(act, BestReplyLocationChoiceStrategyModule.useScaleEpsilonFromConfig, activityIndex, person.getId() );
	}

	@Override
	public void handleLastActivity(Activity act) {
		activityIndex++ ;
		this.score += destinationChoiceScoring.getDestinationScore(act, BestReplyLocationChoiceStrategyModule.useScaleEpsilonFromConfig, activityIndex, person.getId() );
	}
}
