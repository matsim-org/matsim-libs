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
package playground.artemc.crowding.internalization;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.scenario.MutableScenario;
import playground.artemc.crowding.newScoringFunctions.PersonScore;
import playground.artemc.crowding.newScoringFunctions.ScoreTracker;
import playground.artemc.crowding.newScoringFunctions.VehicleScore;

import java.util.HashSet;
import java.util.Set;

/**
 * @author ikaddoura
 *
 */
public class MarginalCostPricingPtHandler implements TransferDelayInVehicleEventHandler, TransferDelayWaitingEventHandler, CapacityDelayEventHandler {

	private final static Logger log = Logger.getLogger(MarginalCostPricingPtHandler.class);

	private final EventsManager events;
	private final MutableScenario scenario;
	private final double vtts_inVehicle;
	private final double vtts_waiting;
	private ScoreTracker scoreTracker;
	
	// TODO: make configurable
//	private final double operatorCostPerVehHour = 39.93; // = 33 * 1.21 (overhead)

	public MarginalCostPricingPtHandler(EventsManager eventsManager, MutableScenario scenario, ScoreTracker scoreTracker) {
		this.events = eventsManager;
		this.scenario = scenario;
		this.scoreTracker = scoreTracker;
		this.vtts_inVehicle = (this.scenario.getConfig().planCalcScore().getModes().get(TransportMode.pt).getMarginalUtilityOfTraveling() - this.scenario.getConfig().planCalcScore().getPerforming_utils_hr()) / this.scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();
		this.vtts_waiting = (this.scenario.getConfig().planCalcScore().getMarginalUtlOfWaitingPt_utils_hr() - this.scenario.getConfig().planCalcScore().getPerforming_utils_hr()) / this.scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();
		
		log.info("VTTS_inVehicleTime: " + vtts_inVehicle);
		log.info("VTTS_waiting: " + vtts_waiting);
	}

	@Override
	public void reset(int iteration) {
	}

	@Override
	public void handleEvent(TransferDelayInVehicleEvent event) {
		
		// external delay effects among users
		double amount1 = (event.getDelay() * event.getAffectedAgents() / 3600.0) * this.vtts_inVehicle;
		PersonMoneyEvent moneyEvent = new PersonMoneyEvent(event.getTime(), event.getCausingAgent(), amount1);
		this.events.processEvent(moneyEvent);

		scoreTracker.setTotalInVehicleTimeDelayExternalityCharges(scoreTracker.getTotalInVehicleTimeDelayExternalityCharges() + amount1);
		scoreTracker.setTotalMoneyPaid(scoreTracker.getTotalMoneyPaid() + amount1);

		if(!scoreTracker.getPersonScores().containsKey(event.getCausingAgent())){
			scoreTracker.getPersonScores().put(event.getCausingAgent(), new PersonScore(event.getCausingAgent()));
		}

		PersonScore personScore = scoreTracker.getPersonScores().get(event.getCausingAgent());
		personScore.setInVehicleTimeDelayExternalityCharge(personScore.getInVehicleTimeDelayExternalityCharge()+amount1);
		personScore.setMoneyPaid(personScore.getMoneyPaid()+amount1);

		//log.info("DwellDelay:  Agent: "+ event.getCausingAgent().toString()+"    Money: "+amount1);

//		// marginal operator cost
//		double amount2 = (event.getDelay() / 3600.0) * this.operatorCostPerVehHour * (-1);
//		AgentMoneyEvent moneyEvent2 = new AgentMoneyEvent(event.getTime(), event.getCausingAgent(), amount2);
//		this.events.processEvent(moneyEvent2);
	}

	@Override
	public void handleEvent(TransferDelayWaitingEvent event) {
		//double amount = (event.getDelay() * event.getAffectedAgentUnits() / 3600.0 ) * this.vtts_waiting;
		//PersonMoneyEvent moneyEvent = new PersonMoneyEvent(event.getTime(), event.getCausingAgent(), amount);
		//this.events.processEvent(moneyEvent);

	}

	@Override
	public void handleEvent(CapacityDelayEvent event) {
		double amount = (event.getDelay() / 3600.0 ) * this.vtts_waiting;
		PersonMoneyEvent moneyEvent = new PersonMoneyEvent(event.getTime(), event.getCausingAgentId(), amount);
		this.events.processEvent(moneyEvent);

		scoreTracker.setTotalCapacityConstraintsExternalityCharges(scoreTracker.getTotalCapacityConstraintsExternalityCharges() + amount);
		scoreTracker.setTotalMoneyPaid(scoreTracker.getTotalMoneyPaid() + amount);

		if(!scoreTracker.getPersonScores().containsKey(event.getCausingAgentId())){
			scoreTracker.getPersonScores().put(event.getCausingAgentId(), new PersonScore(event.getCausingAgentId()));
		}

		PersonScore personScore = scoreTracker.getPersonScores().get(event.getCausingAgentId());
		personScore.setCapacityConstraintsExternalityCharge(personScore.getCapacityConstraintsExternalityCharge() + amount);
		personScore.setMoneyPaid(personScore.getMoneyPaid()+amount);

		//log.info("CapacityDelay:  Agent: "+ event.getCausingAgentId().toString()+"    Money: "+amount);
	}

}