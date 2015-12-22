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

package playground.pbouman.crowdedness;

import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.scoring.ScoringFunction;

import playground.pbouman.crowdedness.events.CrowdedPenaltyEvent;
import playground.pbouman.crowdedness.events.PersonCrowdednessEvent;

/**
 * The CrowdedScoringFunction adds disutilities for crowdedness in PT-vehicles
 * on top the scores calculated by some other ScoringFunction.
 * 
 * @author nagel
 * @author pbouman
 *
 */
public class CrowdedScoringFunction implements ScoringFunction {
	
	private final static double standingPenalty = 16 / 3600d;
	private final static double totalCrowdednessPenalty = 8 / 3600d;
	private final static double sittingCrowdednessPenalty = 2 / 3600d;
	private final static double standingCrowdednessPenalty = 6 / 3600d;
	
	private ScoringFunction delegate ;
	private double score;


	private EventsManager events;

	public CrowdedScoringFunction(ScoringFunction delegate, EventsManager events) {

		this.delegate = delegate;
		this.events = events;
	}


	@Override
	public void handleEvent(Event event) {
		if ( event instanceof PersonCrowdednessEvent ) {
			PersonCrowdednessEvent pce = (PersonCrowdednessEvent) event;
			
			double duration = pce.getDuration();
			boolean isSitting = pce.isSitting();
			double totalCrowdedness = pce.getTotalCrowdedness();
			double seatCrowdedness = pce.getSeatCrowdedness();
			double standingCrowdedness = pce.getStandCrowdedness();
			
			double penalty = 0;
			
			// We just multiply the penalty per second with the duration and the transformed crowdedness factor.
			
			// First we apply a penalty for the total crowdedness of the vehicle
			penalty += duration * transform(totalCrowdedness) * totalCrowdednessPenalty;
						
			if (isSitting)
			{
				// If we are sitting, we add an additional penalty depending on the crowdedness of the seats.
				penalty += duration * transform(seatCrowdedness) * sittingCrowdednessPenalty;
			}
			else
			{
				// If we are standing, we have a linear disutility for standing
				// Also, we have a disutility for the crowdedness of the space we are standing in.
				penalty += duration * standingPenalty;
				penalty += duration * transform(standingCrowdedness) * standingCrowdednessPenalty;
			}
			
			score -= penalty;
			
			if (events != null)
			{
				// If we have an EventManager, report the penalty calculated to it
				events.processEvent(new CrowdedPenaltyEvent(pce.getTime(), pce.getPersonId(), penalty));
			}
			
			//this.score += -1.0 ;
			// unit of this is utils.  1 util is very approximately one Euro.
		}

	}


	// This is just some transformation prevents the
	// crowdedness from being a linear factor.
	// However, it probably does not make a lot of sense.
	private double transform(double crowdedness)
	{
		double result;
		if (crowdedness < 0.5)
		{
			result = crowdedness * crowdedness;
		}
		else
		{
			result = 0.25 + Math.sqrt(crowdedness);
		}		
		return result;
	}
	
	
	@Override
	public void addMoney(double amount) {
		delegate.addMoney(amount);
	}

	@Override
	public void agentStuck(double time) {
		delegate.agentStuck(time);
	}

	@Override
	public void finish() {
		delegate.finish();
	}

	@Override
	public double getScore() {
		return this.score + delegate.getScore();
	}

	@Override
	public void handleActivity(Activity activity) {
		delegate.handleActivity(activity);
	}

	@Override
	public void handleLeg(Leg leg) {
		delegate.handleLeg(leg);
		
	}
	

}
