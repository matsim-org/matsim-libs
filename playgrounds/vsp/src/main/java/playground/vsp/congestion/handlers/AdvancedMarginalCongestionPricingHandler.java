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
package playground.vsp.congestion.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.utils.misc.Time;

import playground.vsp.congestion.events.CongestionEvent;


/**
 * This handler calculates agent money events based on marginal congestion events.
 * The causing agent has to pay a toll depending on the number of affected agents and the external delay in sec.
 * The delay is monetized using the behavioral parameters from the scenario and taking into account the agent-specific Value of Travel Time Savings (VTTS)
 * 
 * @author ikaddoura
 *
 */
public class AdvancedMarginalCongestionPricingHandler implements CongestionEventHandler, ActivityStartEventHandler, ActivityEndEventHandler {

	private final static Logger log = Logger.getLogger(AdvancedMarginalCongestionPricingHandler.class);

	private final Scenario scenario;
	private final EventsManager events;
	private final CharyparNagelScoringParameters params;
	
	private double amountSum = 0.;
	
	private Map<Id<Person>, Double> affectedPersonId2delayToProcess = new HashMap<Id<Person>, Double>();
	private Map<Id<Person>, List<CongestionEvent>> affectedPersonId2congestionEventsToProcess = new HashMap<>();
	private Map<Id<Person>, Double> personId2currentActivityStartTime = new HashMap<Id<Person>, Double>();
	private Map<Id<Person>, Double> personId2firstActivityEndTime = new HashMap<Id<Person>, Double>();
	private Map<Id<Person>, String> personId2currentActivityType = new HashMap<Id<Person>, String>();
	private Map<Id<Person>, String> personId2firstActivityType = new HashMap<Id<Person>, String>();

	public AdvancedMarginalCongestionPricingHandler(EventsManager eventsManager, Scenario scenario) {
		this.events = eventsManager;
		this.scenario = scenario;
		
		params = new CharyparNagelScoringParameters(scenario.getConfig().planCalcScore());
		log.info("Using different VTTS to translate delays into monetary units. Computing the actual activity delay costs for each agent.");
	}

	@Override
	public void reset(int iteration) {
		this.amountSum = 0.;
		this.personId2currentActivityStartTime.clear();
		this.affectedPersonId2congestionEventsToProcess.clear();
		this.personId2firstActivityEndTime.clear();
		this.personId2currentActivityType.clear();
		this.personId2firstActivityType.clear();
		this.affectedPersonId2delayToProcess.clear();
	}

	@Override
	public void handleEvent(CongestionEvent event) {
		
		// store the congestion event
		if (affectedPersonId2congestionEventsToProcess.containsKey(event.getAffectedAgentId())) {
			affectedPersonId2congestionEventsToProcess.get(event.getAffectedAgentId()).add(event);
		} else {
			List<CongestionEvent> congestionEventsToProcess = new ArrayList<>();
			congestionEventsToProcess.add(event);
			affectedPersonId2congestionEventsToProcess.put(event.getAffectedAgentId(), congestionEventsToProcess);
		}	
		
		// compute the total trip delay
		if (this.affectedPersonId2delayToProcess.containsKey(event.getAffectedAgentId())) {
			double delayUpdated = affectedPersonId2delayToProcess.get(event.getAffectedAgentId()) + event.getDelay();
			affectedPersonId2delayToProcess.put(event.getAffectedAgentId(), delayUpdated);
		} else {
			affectedPersonId2delayToProcess.put(event.getAffectedAgentId(), event.getDelay());
		}
		
	}
	
	@Override
	public void handleEvent(ActivityStartEvent event) {
		this.personId2currentActivityStartTime.put(event.getPersonId(), event.getTime());
		this.personId2currentActivityType.put(event.getPersonId(), event.getActType());
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (this.personId2currentActivityStartTime.containsKey(event.getPersonId())) {
			// This is not the first activity...
			
			// ... now process all congestion events thrown during the trip to the activity which has just ended...
			processCongestionEvents(event.getPersonId(), this.personId2currentActivityStartTime.get(event.getPersonId()), event.getTime(), event.getLinkId());
			// ... and remove all processed congestion events. 
			this.affectedPersonId2congestionEventsToProcess.remove(event.getPersonId());
	
		} else {
			// This is the first activity. In case the first activity is an overnight activity, being delayed at the overnight activity is considered in a final step.
			// Therefore, the relevant information has to be stored.
			this.personId2firstActivityEndTime.put(event.getPersonId(), event.getTime());
			this.personId2firstActivityType.put(event.getPersonId(), event.getActType());
		}
	}

	/*
	 * This method has to be called at the very end. Here being delayed at the last / overnight activity is taken into account.
	 */
	public void processFinalCongestionEvents() {
		for (Id<Person> affectedPersonId : this.affectedPersonId2congestionEventsToProcess.keySet()) {
			for (CongestionEvent congestionEvent : this.affectedPersonId2congestionEventsToProcess.get(affectedPersonId)) {			
				processCongestionEvents(congestionEvent.getAffectedAgentId(), this.personId2currentActivityStartTime.get(congestionEvent.getAffectedAgentId()), Time.UNDEFINED_TIME, null);
			}
		}
	}
	
	private void processCongestionEvents(Id<Person> personId, double activityStartTime, double activityEndTime, Id<Link> linkId) {
		
		// First, compute the agent's total (affected) delay from the previous trip.
		double totalDelayThisPerson = 0.0;
		if (this.affectedPersonId2delayToProcess.containsKey(personId)) {
			totalDelayThisPerson = this.affectedPersonId2delayToProcess.get(personId);
		}
		
		if (totalDelayThisPerson > 0.) {
			// The agent was delayed on the previous trip...
			
			// ... now calculate the agent's disutility from being late at the activity: score(arrivalTime_ohneDelay) - score(arrivalTime_withDelay)
			MarginalSumScoringFunction marginaSumScoringFunction = new MarginalSumScoringFunction();
			double activityDelayDisutility = 0.;
			if (activityEndTime == Time.UNDEFINED_TIME) {
				// The activity has an undefined end time. Thus, the activity is considered as the last activity...
				// ... now the first and last OR overnight activity can be handled. This is figured out by the scoring function itself (depending on the activity types).
				
				ActivityImpl activityMorning = new ActivityImpl(this.personId2firstActivityType.get(personId), linkId);
				activityMorning.setEndTime(this.personId2firstActivityEndTime.get(personId));
				
				ActivityImpl activityEvening = new ActivityImpl(this.personId2currentActivityType.get(personId), linkId);
				activityEvening.setStartTime(this.personId2currentActivityStartTime.get(personId));
				
				activityDelayDisutility = marginaSumScoringFunction.getOvernightActivityDelayDisutility(params, activityMorning, activityEvening, totalDelayThisPerson);
				
			} else {
				// The activity has an end time indicating a 'normal' activity.
				
				ActivityImpl activity = new ActivityImpl(this.personId2currentActivityType.get(personId), linkId);
				activity.setStartTime(this.personId2currentActivityStartTime.get(personId));
				activity.setEndTime(activityEndTime);	
				activityDelayDisutility = marginaSumScoringFunction.getNormalActivityDelayDisutility(params, activity, totalDelayThisPerson);
			}
			
			// Calculate the agent's trip delay disutility (could be done similar to the activity delay disutility).
			double tripDelayDisutility = (totalDelayThisPerson / 3600.) * this.scenario.getConfig().planCalcScore().getTraveling_utils_hr() * (-1);
			
			// Translate the disutility into monetary units.
			double totalDelayCost = (activityDelayDisutility + tripDelayDisutility) / this.scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();
			double delayCostPerSecond = totalDelayCost / totalDelayThisPerson;
			
			// Go through the congestion events and charge each causing agent his/her contribution to the delay cost: caused delay * activity delay cost per second + caused delay * trip delay cost per second
			for (CongestionEvent congestionEvent : this.affectedPersonId2congestionEventsToProcess.get(personId)) {
				if (congestionEvent.getAffectedAgentId().toString().equals(personId.toString())) {
					double amount = congestionEvent.getDelay() * delayCostPerSecond * (-1);
					
					this.amountSum = this.amountSum + amount;
					
					PersonMoneyEvent moneyEvent = new PersonMoneyEvent(activityEndTime, congestionEvent.getCausingAgentId(), amount);
					this.events.processEvent(moneyEvent);
					
				}
			}
		}
	}
	
	public double getAmountSum() {
		return amountSum;
	}
	
}