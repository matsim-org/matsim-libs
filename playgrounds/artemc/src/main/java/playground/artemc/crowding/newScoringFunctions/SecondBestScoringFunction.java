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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.scoring.ScoringFunction;

import playground.artemc.crowding.BusFacilityInteractionEvent;
import playground.artemc.crowding.events.CrowdedPenaltyEvent;
import playground.artemc.crowding.events.PersonCrowdednessEvent;

/**
 * The SecondBestScoringFunction add fares depending on the trip duration, the time 
 * and the facility of alighting   
 * 
 * @author grerat
 * 
 */
public class SecondBestScoringFunction implements ScoringFunction {
	
	private ScoringFunction delegate ;
	private double score;
	private ScoreTracker scoreTracker;
	private EventsManager events;
	private Id personId;
	
	double marginalUtilityOfMoney;
	double opportunityCostOfPtTravel; 
	
	public SecondBestScoringFunction(ScoringFunction delegate) {
		this.delegate = delegate;
	}
	
	public SecondBestScoringFunction(ScoringFunction delegate, EventsManager events, ScoreTracker scoreTracker, Controler controler) {

		this.delegate = delegate;
		this.events = events;
		this.scoreTracker = scoreTracker;
		this.opportunityCostOfPtTravel = - controler.getConfig().planCalcScore().getTravelingPt_utils_hr() + controler.getConfig().planCalcScore().getPerforming_utils_hr();
		this.marginalUtilityOfMoney = controler.getConfig().planCalcScore().getMarginalUtilityOfMoney();
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
			
			
			Id vehicleId = pce. getVehicleId();
			Id personId = pce.getPersonId();
			PersonScore personScore = scoreTracker.getPersonScores().get(personId);
			String facilityOfAlighting = pce.getFacilityId().toString();
	
			double scoringTime = pce.getTime();		
			personScore.setScoringTime(scoringTime);
			double travelTime = pce.getTravelTime();
			double dwellTime = pce.getDwellTime();
			personScore.setTripDuration(personScore.getTripDuration()+ travelTime + dwellTime);
			double tripDuration = personScore.getTripDuration();
			List<String> facilitySmall = Arrays.asList ("1","2","3","4","5","6","7","8","9","10","11","12");
			List<String> facilityMedium = Arrays.asList("13","14","15","16","17");
			List<String> facilityHigh = Arrays.asList("18","19","20","21","22","23","24","25","26","27","28","29","30","31","32","33","34");
			
			double penalty = 0;
			double crowdingExtern = 0;
			double inVehTimeDelayExtern = 0;
			double capConstrExtern = 0;
			
			boolean isLeaving = pce.isLeaving();
			if(isLeaving == true) {
				if(scoringTime >= 23400 && scoringTime < 27000) {
					double a_crowding = -2.256e-007;
					double b_crowding = 0.0018564;
					double c_crowding = -0.0410816;
					crowdingExtern = a_crowding*tripDuration*tripDuration + b_crowding*tripDuration + c_crowding;
					
					double a_inVehTimeDelay = -1.439e-008;
					double b_inVehTimeDelay = -9.25e-005;
					double c_inVehTimeDelay = 0.254729;
					inVehTimeDelayExtern = a_inVehTimeDelay*tripDuration*tripDuration + b_inVehTimeDelay*tripDuration + c_inVehTimeDelay;
				}
				if(scoringTime >= 27000 && scoringTime < 30600){
					double a_crowding = -2.684e-007;
					double b_crowding = 0.0022329;
					double c_crowding = -0.207564;
					crowdingExtern = a_crowding*tripDuration*tripDuration + b_crowding*tripDuration + c_crowding;
					
					double a_inVehTimeDelay = -6.569e-008;
					double b_inVehTimeDelay = 6.065e-005;
					double c_inVehTimeDelay = 0.183248;
					inVehTimeDelayExtern = a_inVehTimeDelay*tripDuration*tripDuration + b_inVehTimeDelay*tripDuration + c_inVehTimeDelay;
				
				}
				if(scoringTime >= 30600 && scoringTime < 34200){
					double a_crowding = -2.998e-007;
					double b_crowding = 0.0022514;
					double c_crowding = -0.232516;
					crowdingExtern = a_crowding*tripDuration*tripDuration + b_crowding*tripDuration + c_crowding;
					
					double a_inVehTimeDelay = -4.725e-008;
					double b_inVehTimeDelay = 1.192e-005;
					double c_inVehTimeDelay = 0.205789;
					inVehTimeDelayExtern = a_inVehTimeDelay*tripDuration*tripDuration + b_inVehTimeDelay*tripDuration + c_inVehTimeDelay;
				
				}
				if(scoringTime >= 34200 && scoringTime < 37800){
					double a_crowding = -9.683e-008;
					double b_crowding = 0.0016766;
					double c_crowding = -0.0853464;
					crowdingExtern = a_crowding*tripDuration*tripDuration + b_crowding*tripDuration + c_crowding;
					
					double a_inVehTimeDelay = 1.789e-008;
					double b_inVehTimeDelay = -0.0001241;
					double c_inVehTimeDelay = 0.228548;
					inVehTimeDelayExtern = a_inVehTimeDelay*tripDuration*tripDuration + b_inVehTimeDelay*tripDuration + c_inVehTimeDelay;
				}
				
				if(facilityMedium.contains(facilityOfAlighting) && scoringTime >= 23400 && scoringTime < 34200){
					double a_capConstr = -8.061e-007;
					double b_capConstr = 0.0013432;
					double c_capConstr = 0.0684468;
					capConstrExtern = a_capConstr*tripDuration*tripDuration + b_capConstr*tripDuration + c_capConstr;
				}
				
				if(facilityHigh.contains(facilityOfAlighting) && scoringTime >= 23400 && scoringTime < 34200){
					double a_capConstr = -4.165e-007;
					double b_capConstr = 0.0014985;
					double c_capConstr = -0.449811;
					capConstrExtern = a_capConstr*tripDuration*tripDuration + b_capConstr*tripDuration + c_capConstr;
				}
				
				if(facilityHigh.contains(facilityOfAlighting) && scoringTime >= 34200 && scoringTime < 37800){
					double a_capConstr = 1.828e-007;
					double b_capConstr = -4.777e-005;
					double c_capConstr = 0.0475427;
					capConstrExtern = a_capConstr*tripDuration*tripDuration + b_capConstr*tripDuration + c_capConstr;
				}
				if(crowdingExtern < 0){
					crowdingExtern = 0;
				}
				if(inVehTimeDelayExtern < 0){
					inVehTimeDelayExtern = 0;
				}
				if(capConstrExtern < 0){
					capConstrExtern = 0;
				}
			
			if (events != null)
			{
				// If we have an EventManager, report the penalty calculated to it
				// "getTime", "getVehicleId" and "externality" have beed added at the initial class (guillaumer)
				events.processEvent(new CrowdedPenaltyEvent(pce.getTime(), personId, vehicleId, penalty, crowdingExtern+inVehTimeDelayExtern+capConstrExtern));
				
				//Paying for externality
				PersonMoneyEvent e = new PersonMoneyEvent(pce.getTime(),personId, -(crowdingExtern + inVehTimeDelayExtern + capConstrExtern));
				events.processEvent(e);
			}
			
			// FILLING THE SCORETRACKER
			// Set the total crowdedness and the sum of the externalities produced during the simulation
			scoreTracker.setTotalCrowdednessExternalityCharges(scoreTracker.getTotalCrowdednessExternalityCharges() + crowdingExtern);
			scoreTracker.setTotalInVehicleTimeDelayExternalityCharges(scoreTracker.getTotalInVehicleTimeDelayExternalityCharges() + inVehTimeDelayExtern);
			scoreTracker.setTotalCapacityConstraintsExternalityCharges(scoreTracker.getTotalCapacityConstraintsExternalityCharges() + capConstrExtern);
			scoreTracker.setTotalMoneyPaid(scoreTracker.getTotalMoneyPaid() + crowdingExtern + inVehTimeDelayExtern + capConstrExtern);

			// Set an objet PersonScore() per agent (score, time when scoring, trip duration, facility of alighting,
			// crowding utility, externalities and money paid)
			personScore.setFacilityOfAlighting(pce.getFacilityId());
			personScore.setVehicleId(vehicleId);
			personScore.setCrowdednessExternalityCharge(crowdingExtern);
			personScore.setInVehicleTimeDelayExternalityCharge(inVehTimeDelayExtern);
			personScore.setCapacityConstraintsExternalityCharge(capConstrExtern);
			personScore.setMoneyPaid(crowdingExtern + inVehTimeDelayExtern + capConstrExtern);
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
