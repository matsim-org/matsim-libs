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
import java.util.Iterator;
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
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

import playground.vsp.congestion.events.CongestionEvent;


/**
 * This handler calculates agent money events based on marginal congestion events.
 * The causing agent has to pay a toll depending on the number of affected agents and the external delay in sec.
 * The delay is monetized using the behavioral parameters from the scenario and taking into account for the agent-specific Value of Travel Time Savings (VTTS)
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
	
	private List<CongestionEvent> congestionEventsToProcess = new ArrayList<CongestionEvent>();
	private Map<Id<Person>, Double> personId2activityStartTime = new HashMap<Id<Person>, Double>();
	private Map<Id<Person>, Double> personId2firstActivityEndTime = new HashMap<Id<Person>, Double>();
	private Map<Id<Person>, String> personId2currentActivityType = new HashMap<Id<Person>, String>();

	public AdvancedMarginalCongestionPricingHandler(EventsManager eventsManager, Scenario scenario) {
		this.events = eventsManager;
		this.scenario = scenario;
		
		params = new CharyparNagelScoringParameters(scenario.getConfig().planCalcScore());
		
		log.info("Computing agent-specific activity delay costs.");
	}

	@Override
	public void reset(int iteration) {
		this.amountSum = 0.;
		this.personId2activityStartTime.clear();
		this.congestionEventsToProcess.clear();
		this.personId2firstActivityEndTime.clear();
		this.personId2currentActivityType.clear();
	}

	@Override
	public void handleEvent(CongestionEvent event) {
		congestionEventsToProcess.add(event);
//		System.out.println("***");
//		System.out.println("Updated congestion events to process (after adding): ");
//		for (CongestionEvent congestionEvent : this.congestionEventsToProcess) {
//			System.out.println(congestionEvent.toString());
//		}
//		System.out.println("***");
		
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		System.out.println(event.getActType());
		if (this.personId2activityStartTime.containsKey(event.getPersonId())) {
			// not the first activity
//			System.out.println("not the first activity");
			processCongestionEvents(event.getPersonId(), this.personId2activityStartTime.get(event.getPersonId()), event.getTime(), event.getLinkId());
			removeProcessedCongestionEvents(event.getPersonId());
		} else {
			// the first activity
//			System.out.println("first activity");
			this.personId2firstActivityEndTime.put(event.getPersonId(), event.getTime());
		}
	}

	private void removeProcessedCongestionEvents(Id<Person> personId) {
		for (Iterator<CongestionEvent> iterator = this.congestionEventsToProcess.iterator(); iterator.hasNext();) {
		    CongestionEvent event = iterator.next();
		    if (event.getAffectedAgentId().toString().equals(personId.toString())) {
		        iterator.remove();
		    }
		}
//		System.out.println("***");
//		System.out.println("Updated congestion events to process (after removing): ");
//		for (CongestionEvent congestionEvent : this.congestionEventsToProcess) {
//			System.out.println(congestionEvent.toString());
//		}
//		System.out.println("***");
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		this.personId2activityStartTime.put(event.getPersonId(), event.getTime());
		this.personId2currentActivityType.put(event.getPersonId(), event.getActType());
	}
	
	public void processFinalCongestionEvents() {
		for (CongestionEvent congestionEvent : this.congestionEventsToProcess) {
			processCongestionEvents(congestionEvent.getAffectedAgentId(), this.personId2activityStartTime.get(congestionEvent.getAffectedAgentId()), this.personId2firstActivityEndTime.get(congestionEvent.getAffectedAgentId()), null);
		}
	}
	
	private void processCongestionEvents(Id<Person> personId, double activityStartTime, double activityEndTime, Id<Link> linkId) {
		
		double totalDelayThisPerson = 0.;
		for (CongestionEvent congestionEvent : this.congestionEventsToProcess) {
			if (congestionEvent.getAffectedAgentId().toString().equals(personId.toString())) {
				totalDelayThisPerson = totalDelayThisPerson + congestionEvent.getDelay();
			}
		}
		
		if (totalDelayThisPerson > 0.) {
			// the agent was delayed on the previous trip
			
			// calculate the agent's activity delay disutility: activityScore(arrivalTime_ohneDelay) - activityScore(arrivalTime_withDelay)
			MarginalSumScoringFunction marginaSumScoringFunction = new MarginalSumScoringFunction();
			marginaSumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params));
			ActivityImpl activity = new ActivityImpl(this.personId2currentActivityType.get(personId), linkId);
			activity.setStartTime(this.personId2activityStartTime.get(personId));
			activity.setEndTime(activityEndTime);	
			double activityDelayDisutility = marginaSumScoringFunction.getActivityDelayDisutility(activity, totalDelayThisPerson);
			
			// calculate the agent's trip delay disutility (could be done similar to the activity delay disutility)
			double tripDelayDisutility = (totalDelayThisPerson / 3600.) * this.scenario.getConfig().planCalcScore().getTraveling_utils_hr() * (-1);
			
			// translate disutility into monetary units
			double totalDelayCost = (activityDelayDisutility + tripDelayDisutility) / this.scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();
			double delayCostPerSecond = totalDelayCost / totalDelayThisPerson;
			
			// Go through the congestion events and charge each causing agent: caused delay * activity delay cost per second + caused delay * trip delay cost per second
			for (CongestionEvent congestionEvent : this.congestionEventsToProcess) {
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