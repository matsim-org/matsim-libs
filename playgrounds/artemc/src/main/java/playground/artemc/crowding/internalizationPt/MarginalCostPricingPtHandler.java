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
package playground.artemc.crowding.internalizationPt;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.scenario.ScenarioImpl;

import playground.artemc.crowding.events.PersonCrowdednessEvent;
import playground.artemc.crowding.newScoringFunctions.PersonScore;
import playground.artemc.crowding.newScoringFunctions.ScoreTracker;
import playground.artemc.crowding.newScoringFunctions.VehicleScore;

/**
 * @author ikaddoura
 * 
 * This class retrieve the In Vehicle Time Delay events (from TransferDelayInVehicleEvent)
 * and the Capacity Constraint events (from CapacityDelayEvent) and add a monetary externality
 * at each agent who cause these externalities
 * 
 * @author grerat
 * 
 */
public class MarginalCostPricingPtHandler implements TransferDelayInVehicleEventHandler, CapacityDelayEventHandler {

	private final static Logger log = Logger.getLogger(MarginalCostPricingPtHandler.class);

	private final EventsManager events;
	private final ScenarioImpl scenario;
	private ScoreTracker scoreTracker;
	private final double vtts_inVehicle;
	private final double vtts_waiting;
	
	// TODO: make configurable
	private final double operatorCostPerVehHour = 39.93; // = 33 * 1.21 (overhead)

	public MarginalCostPricingPtHandler(EventsManager eventsManager, ScenarioImpl scenario, ScoreTracker scoreTracker) {
		this.events = eventsManager;
		this.scenario = scenario;
		this.scoreTracker = scoreTracker;
		this.vtts_inVehicle = (this.scenario.getConfig().planCalcScore().getTravelingPt_utils_hr() - this.scenario.getConfig().planCalcScore().getPerforming_utils_hr()) / this.scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();	
		this.vtts_waiting = (this.scenario.getConfig().planCalcScore().getMarginalUtlOfWaitingPt_utils_hr() - this.scenario.getConfig().planCalcScore().getPerforming_utils_hr()) / this.scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();
		
		log.info("VTTS_inVehicleTime: " + vtts_inVehicle);
		log.info("VTTS_waiting: " + vtts_waiting);
	}

	@Override
	public void reset(int iteration) {
	}

	@Override
	public void handleEvent(TransferDelayInVehicleEvent event) {
		
		TransferDelayInVehicleEvent tdive = (TransferDelayInVehicleEvent) event;
		
		if(!scoreTracker.getPersonScores().containsKey(tdive.getCausingAgent())){
			scoreTracker.getPersonScores().put(tdive.getCausingAgent(), new PersonScore(tdive.getCausingAgent()));
		}
		
		if(!scoreTracker.getVehicleExternalities().containsKey(tdive.getVehicleId())){
			scoreTracker.getVehicleExternalities().put(tdive.getVehicleId(), new VehicleScore(tdive.getVehicleId()));
		}
		
		// external delay effects among users
		double monetaryExternality = (event.getDelay() * event.getAffectedAgents() / 3600.0) * this.vtts_inVehicle;
		PersonMoneyEvent moneyEvent = new PersonMoneyEvent(event.getTime(), event.getCausingAgent(), monetaryExternality);
		this.events.processEvent(moneyEvent);
		
		Id personId = tdive.getCausingAgent();
		Id vehicleId =tdive.getVehicleId();
		Id facilityId = tdive.getFacilityId();
		double time = tdive.getTime();
		
		
		// FILL THE SCORETRACKER
		// Set the total crowdedness and the sum of the externalities produced during the simulation
		scoreTracker.setTotalInVehicleTimeDelayExternalityCharges(scoreTracker.getTotalInVehicleTimeDelayExternalityCharges() - monetaryExternality);
		scoreTracker.setTotalMoneyPaid(scoreTracker.getTotalMoneyPaid() - monetaryExternality);
		
		// Set an objet PersonScore() per agent (score, time when scoring, trip duration, facility of alighting,
		// crowding utility, externalitites and money paid)
		PersonScore personScore = scoreTracker.getPersonScores().get(personId);
		personScore.setScoringTime(time);
		personScore.setInVehicleTimeDelayExternalityCharge(-monetaryExternality+personScore.getInVehicleTimeDelayExternalityCharge());
		personScore.setMoneyPaid(-monetaryExternality + personScore.getMoneyPaid());
		
		// Set an objet VehicleExternalities() per vehicle (facilities with boarding/alighting, crowding penalty pro facility, 
		// externalities pro facility)
		VehicleScore vehicleScore = scoreTracker.getVehicleExternalities().get(vehicleId);
		if(!vehicleScore.getFacilityId().containsKey(vehicleId)) {
			Set<Id> facilities = new HashSet<Id>();
			facilities.add(facilityId);	
			vehicleScore.getFacilityId().put(vehicleId, facilities);
			vehicleScore.getFacilityTime().put(facilityId, time);
			vehicleScore.getVehicleInVehicleTimeDelayExternalityCharge().put(facilityId, -monetaryExternality);
			vehicleScore.getVehicleMoneyPaid().put(facilityId, -monetaryExternality);
			}
		
		else if(!vehicleScore.getVehicleCrowdingCost().containsKey(facilityId)){
			vehicleScore.getFacilityId().get(vehicleId).add(facilityId);
			vehicleScore.getFacilityTime().put(facilityId, time);
			vehicleScore.getVehicleInVehicleTimeDelayExternalityCharge().put(facilityId, -monetaryExternality);
			vehicleScore.getVehicleMoneyPaid().put(facilityId, -monetaryExternality);
		}
		
		else if(!vehicleScore.getVehicleInVehicleTimeDelayExternalityCharge().containsKey(facilityId)){
			vehicleScore.getFacilityTime().put(facilityId, time);
			vehicleScore.getVehicleInVehicleTimeDelayExternalityCharge().put(facilityId, -monetaryExternality);
			vehicleScore.getVehicleMoneyPaid().put(facilityId, vehicleScore.getVehicleMoneyPaid().get(facilityId)-monetaryExternality);
		}
		
		else { 
			vehicleScore.getFacilityTime().put(facilityId, time);
			vehicleScore.getVehicleInVehicleTimeDelayExternalityCharge().put(facilityId, vehicleScore.getVehicleInVehicleTimeDelayExternalityCharge().get(facilityId) - monetaryExternality);
			vehicleScore.getVehicleMoneyPaid().put(facilityId, vehicleScore.getVehicleMoneyPaid().get(facilityId) -monetaryExternality);
		}
	}

	
	@Override
	public void handleEvent(CapacityDelayEvent event) {
		
		CapacityDelayEvent cde = (CapacityDelayEvent) event;
		
		if(!scoreTracker.getPersonScores().containsKey(cde.getCausingAgentId())){
			scoreTracker.getPersonScores().put(cde.getCausingAgentId(), new PersonScore(cde.getCausingAgentId()));
		}
		
		if(!scoreTracker.getVehicleExternalities().containsKey(cde.getVehicleId())){
			scoreTracker.getVehicleExternalities().put(cde.getVehicleId(), new VehicleScore(cde.getVehicleId()));
		}
		
		double monetaryExternality = (event.getDelay() / 3600.0 ) * this.vtts_waiting;
		PersonMoneyEvent moneyEvent = new PersonMoneyEvent(event.getTime(), event.getCausingAgentId(), monetaryExternality);
		this.events.processEvent(moneyEvent);
		
		Id personId = cde.getCausingAgentId();
		Id vehicleId = cde.getVehicleId();
		Id facilityId = cde.getFacilityId();
		double time = cde.getTime();
		
		
		// FILL THE SCORETRACKER
		// Set the sum of the externalities produced during the simulation
		scoreTracker.setTotalCapacityConstraintsExternalityCharges(scoreTracker.getTotalCapacityConstraintsExternalityCharges() - monetaryExternality);
		scoreTracker.setTotalMoneyPaid(scoreTracker.getTotalMoneyPaid() -monetaryExternality);			
		
		// Set an objet PersonScore() per agent (score, time when scoring, trip duration, facility of alighting,
		// crowding utility, externalitites and money paid)
		PersonScore personScore = scoreTracker.getPersonScores().get(personId);
		personScore.setScoringTime(time);
		personScore.setCapacityConstraintsExternalityCharge(-monetaryExternality+personScore.getCapacityConstraintsExternalityCharge());
		personScore.setMoneyPaid(-monetaryExternality + personScore.getMoneyPaid());
		
		// Set an objet VehicleExternalities() per vehicle (facilities with boarding/alighting, crowding penalty pro facility, 
		// externalities pro facility)
		VehicleScore vehicleScore = scoreTracker.getVehicleExternalities().get(vehicleId);
		if(!vehicleScore.getFacilityId().containsKey(vehicleId)) {
			Set<Id> facilities = new HashSet<Id>();
			facilities.add(facilityId);	
			vehicleScore.getFacilityId().put(vehicleId, facilities);
			vehicleScore.getFacilityTime().put(facilityId, time);
			vehicleScore.getVehicleCapacityConstraintsExternalityCharge().put(facilityId, -monetaryExternality);
			vehicleScore.getVehicleMoneyPaid().put(facilityId, -monetaryExternality);
		}

		else if(!vehicleScore.getVehicleCrowdingCost().containsKey(facilityId)){
			vehicleScore.getFacilityId().get(vehicleId).add(facilityId);
			vehicleScore.getFacilityTime().put(facilityId, time);
			vehicleScore.getVehicleCapacityConstraintsExternalityCharge().put(facilityId, -monetaryExternality);
			vehicleScore.getVehicleMoneyPaid().put(facilityId, -monetaryExternality);
		}

		else if(!vehicleScore.getVehicleCapacityConstraintsExternalityCharge().containsKey(facilityId)){
			vehicleScore.getFacilityTime().put(facilityId, time);
			vehicleScore.getVehicleCapacityConstraintsExternalityCharge().put(facilityId, -monetaryExternality);
			vehicleScore.getVehicleMoneyPaid().put(facilityId, vehicleScore.getVehicleMoneyPaid().get(facilityId)-monetaryExternality);
		}

		else { 
			vehicleScore.getFacilityTime().put(facilityId, time);
			vehicleScore.getVehicleCapacityConstraintsExternalityCharge().put(facilityId, vehicleScore.getVehicleCapacityConstraintsExternalityCharge().get(facilityId) -monetaryExternality);
			vehicleScore.getVehicleMoneyPaid().put(facilityId, vehicleScore.getVehicleMoneyPaid().get(facilityId) -monetaryExternality);
		}

	}
	
	

}