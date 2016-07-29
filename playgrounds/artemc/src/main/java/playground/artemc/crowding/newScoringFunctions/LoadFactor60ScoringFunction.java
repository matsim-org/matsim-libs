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

package playground.artemc.crowding.newScoringFunctions;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.scoring.ScoringFunction;

import playground.artemc.crowding.events.CrowdedPenaltyEvent;
import playground.artemc.crowding.events.PersonCrowdednessEvent;

/**
 * The LoadFactor60ScoringFunction adds disutilities for crowdedness in PT-vehicles
 * on top the scores calculated by some other ScoringFunction. 
 * 
 * @author nagel
 * @author pbouman
 *
 * Implementation of the 4th Model of Tirachni's (M4). Disutilities are added if the load factor > 0.6.
 * @author grereat)
 * 
 */
public class LoadFactor60ScoringFunction implements ScoringFunction {
	
//	private final static double loadFactorBeta = 0.006 / 60d; //beta_lf_b, Tirachini, p.44
//	private final static double standingPenalty = 16 / 3600d; // documentation ???

	private final static double loadFactorBeta = ((0.66 + 0.48) * 6.0 / 11.0) / 3600.0; //beta_lf_b, Tirachini, p.44, ratio of travel time and crowded travel time: 6/11

	private ScoringFunction delegate ;
	private double score;
	private ScoreTracker scoreTracker;
	private EventsManager events;
	private Id personId;
	
	double marginalUtilityOfMoney;
	double opportunityCostOfPtTravel; 
	
	public LoadFactor60ScoringFunction(ScoringFunction delegate, EventsManager events) {
		this.delegate = delegate;
		this.events = events;
	}
	
	public LoadFactor60ScoringFunction(ScoringFunction delegate, EventsManager events, ScoreTracker scoreTracker, Scenario scenario) {

		this.delegate = delegate;
		this.events = events;
		this.scoreTracker = scoreTracker;
		this.opportunityCostOfPtTravel = - scenario.getConfig().planCalcScore().getModes().get(TransportMode.pt).getMarginalUtilityOfTraveling() + scenario.getConfig().planCalcScore().getPerforming_utils_hr();
		this.marginalUtilityOfMoney = scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();
	}


	@Override
	public void handleEvent(Event event) {
		if( event instanceof ActivityEndEvent && personId == null ){
			ActivityEndEvent aee = (ActivityEndEvent) event;
			personId = aee.getPersonId();
		}

		if ( event instanceof PersonCrowdednessEvent ) {
			PersonCrowdednessEvent pce = (PersonCrowdednessEvent) event;
	
			if(!scoreTracker.getPersonScores().containsKey(pce.getPersonId())){
				scoreTracker.getPersonScores().put(pce.getPersonId(), new PersonScore(pce.getPersonId()));
			}
			
			if(!scoreTracker.getVehicleExternalities().containsKey(pce.getVehicleId())){
				scoreTracker.getVehicleExternalities().put(pce.getVehicleId(), new VehicleScore(pce.getVehicleId()));
			}
			
			
			Id facilityId = pce.getFacilityId();
			Id vehicleId = pce. getVehicleId();
			Id personId = pce.getPersonId();
	
			double scoringTime = pce.getTime();			
			double travelTime = pce.getTravelTime();
			double dwellTime = pce.getDwellTime();
			boolean isSitting = pce.isSitting();
			int sitters = pce.getSitters();
			int standees = pce.getStandees();
			double loadFactor = pce.getloadFactor();
			
			double penalty = 0;
			double standingPenalty = 0;
			double seatingPenalty = 0;
			double externality = 0;
			double monetaryExternality = 0;
			
			// Use of the Tirachini's formula
			
			// The crowding disutilities arise when the load factor reach 60%
			seatingPenalty += (travelTime + dwellTime) * loadFactorBeta * Math.max(loadFactor - 0.6, 0);
		
			standingPenalty += (travelTime + dwellTime) * loadFactorBeta * Math.max(loadFactor - 0.6, 0);
			
			if (isSitting)
			{
				penalty = seatingPenalty;

			}
			else
			{
				penalty = standingPenalty;

			}
			score -= penalty;
			
			
			// Calculate Externalities
			externality = (standingPenalty * standees + seatingPenalty * sitters) / (standees + sitters);
			monetaryExternality = externality / marginalUtilityOfMoney;

			if (events != null)
			{
				// If we have an EventManager, report the penalty calculated to it
				// "getTime", "getVehicleId" and "externality" have beed added at the initial class
				events.processEvent(new CrowdedPenaltyEvent(pce.getTime(), personId, vehicleId, penalty, externality));
			
				//Paying for externality
				PersonMoneyEvent e = new PersonMoneyEvent(pce.getTime(),personId, -monetaryExternality);
				events.processEvent(e);
			}
			
			// FILLING THE SCORETRACKER
			// Set the total crowdedness and the sum of the externalities produced during the simulation
			scoreTracker.setTotalCrowdednessUtility(scoreTracker.getTotalCrowdednessUtility()-penalty);
			scoreTracker.setTotalCrowdednessExternalityCharges(scoreTracker.getTotalCrowdednessExternalityCharges() + monetaryExternality);
			scoreTracker.setTotalMoneyPaid(scoreTracker.getTotalMoneyPaid() + monetaryExternality);

			// Set an objet PersonScore() per agent (score, time when scoring, trip duration, facility of alighting,
			// crowding utility, externalitites and money paid)
			PersonScore personScore = scoreTracker.getPersonScores().get(personId);
			personScore.setScoringTime(scoringTime);
			personScore.setTripDuration(personScore.getTripDuration() + (travelTime + dwellTime));
			personScore.setFacilityOfAlighting(facilityId);
			personScore.setCrowdingUtility(personScore.getCrowdingUtility()-penalty);
			personScore.setCrowdednessExternalityCharge(monetaryExternality+personScore.getCrowdednessExternalityCharge());
			personScore.setMoneyPaid(monetaryExternality + personScore.getMoneyPaid());

			// Set an objet VehicleExternalities() per vehicle (facilities with boarding/alighting, crowding penalty pro facility, 
			// externalities pro facility)
			VehicleScore vehicleScore = scoreTracker.getVehicleExternalities().get(vehicleId);
			if(!vehicleScore.getFacilityId().containsKey(vehicleId)) {
				Set<Id> facilities = new HashSet<Id>();
				facilities.add(facilityId);	
				scoreTracker.getVehicleExternalities().get(vehicleId).getFacilityId().put(vehicleId, facilities);
				vehicleScore.getFacilityTime().put(facilityId, scoringTime);
				vehicleScore.getDwellTime().put(facilityId, dwellTime);
				vehicleScore.getVehicleCrowdingCost().put(facilityId, -penalty);
				vehicleScore.getVehicleCrowdednessExternalityCharge().put(facilityId, monetaryExternality);
				vehicleScore.getVehicleMoneyPaid().put(facilityId, monetaryExternality);
			}

			else if(!vehicleScore.getVehicleCrowdingCost().containsKey(facilityId)){
				vehicleScore.getFacilityId().get(vehicleId).add(facilityId);
				vehicleScore.getFacilityTime().put(facilityId, scoringTime);
				vehicleScore.getDwellTime().put(facilityId, dwellTime);
				vehicleScore.getVehicleCrowdingCost().put(facilityId, -penalty);
				vehicleScore.getVehicleCrowdednessExternalityCharge().put(facilityId, monetaryExternality);
				vehicleScore.getVehicleMoneyPaid().put(facilityId, monetaryExternality);
			}

			else {
				vehicleScore.getFacilityTime().put(facilityId, scoringTime);
				vehicleScore.getDwellTime().put(facilityId, dwellTime);
				vehicleScore.getVehicleCrowdingCost().put(facilityId, vehicleScore.getVehicleCrowdingCost().get(facilityId)-penalty);
				vehicleScore.getVehicleCrowdednessExternalityCharge().put(facilityId, vehicleScore.getVehicleCrowdednessExternalityCharge().get(facilityId) + monetaryExternality);
				vehicleScore.getVehicleMoneyPaid().put(facilityId, vehicleScore.getVehicleMoneyPaid().get(facilityId) + monetaryExternality);
			}
		}

	}

	@Override
	public void addMoney(double amount) {
//		if(personId!=null)
//			scoreTracker.getPersonScores().get(personId).setMoneyPaid(amount + scoreTracker.getPersonScores().get(personId).getMoneyPaid());
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
	
	public ScoreTracker getScoreTracker() {
		return scoreTracker;
	}
	

}
