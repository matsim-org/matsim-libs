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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.noise.personLinkMoneyEvents.PersonLinkMoneyEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scoring.functions.ScoringParameters;
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

	private static int incompletedPlanWarning = 0;
	
	private final Scenario scenario;
	private final EventsManager events;
	
	private double factor = 1.0;
	
	private double amountSum = 0.;
	
	private Map<Id<Person>, Double> affectedPersonId2delayToProcess = new HashMap<Id<Person>, Double>();
	private Map<Id<Person>, List<CongestionEvent>> affectedPersonId2congestionEventsToProcess = new HashMap<>();
	private Map<Id<Person>, Double> personId2currentActivityStartTime = new HashMap<Id<Person>, Double>();
	private Map<Id<Person>, Double> personId2firstActivityEndTime = new HashMap<Id<Person>, Double>();
	private Map<Id<Person>, String> personId2currentActivityType = new HashMap<Id<Person>, String>();
	private Map<Id<Person>, String> personId2firstActivityType = new HashMap<Id<Person>, String>();
	
	private Map<Id<Person>, List<Double>> personId2VTTSh = new HashMap<>();

	private MarginalSumScoringFunction marginaSumScoringFunction;
	
	public AdvancedMarginalCongestionPricingHandler(EventsManager eventsManager, Scenario scenario) {
		this(eventsManager, scenario, 1.0);
	}
	
	public AdvancedMarginalCongestionPricingHandler(EventsManager eventsManager, Scenario scenario, double factor) {
		log.info("Using the toll factor " + factor);
		log.info("Using different VTTS to translate delays into monetary units. Computing the actual activity delay costs for each agent.");
		
		this.events = eventsManager;
		this.scenario = scenario;
		this.factor = factor;
		
		this.marginaSumScoringFunction =
				new MarginalSumScoringFunction(
						new ScoringParameters.Builder(scenario.getConfig().planCalcScore(), scenario.getConfig().planCalcScore().getScoringParameters(null), scenario.getConfig().scenario()).build());
	}

	@Override
	public void reset(int iteration) {
		
		incompletedPlanWarning = 0;
		
		this.amountSum = 0.;
		this.personId2currentActivityStartTime.clear();
		this.affectedPersonId2congestionEventsToProcess.clear();
		this.personId2firstActivityEndTime.clear();
		this.personId2currentActivityType.clear();
		this.personId2firstActivityType.clear();
		this.affectedPersonId2delayToProcess.clear();
		
		this.personId2VTTSh.clear();
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
						
			// ... now process all congestion events thrown during the trip to the activity which has just ended, ...
			processCongestionEventsForAffectedPerson(event.getPersonId(), event.getTime(), event.getLinkId());
			
			// ... update the status of the 'current' activity...
			this.personId2currentActivityType.remove(event.getPersonId());
			this.personId2currentActivityStartTime.remove(event.getPersonId());
			
			// ... and remove all processed congestion events. 
			this.affectedPersonId2congestionEventsToProcess.remove(event.getPersonId());
			this.affectedPersonId2delayToProcess.remove(event.getPersonId());
	
		} else {
			// This is the first activity. The first and last / overnight activity are / is considered in a final step.
			// Therefore, the relevant information has to be stored.
			this.personId2firstActivityEndTime.put(event.getPersonId(), event.getTime());
			this.personId2firstActivityType.put(event.getPersonId(), event.getActType());
		}
	}

	/*
	 * This method has to be called after the MobSim. Here, the disutility from being delayed at the last / overnight activity is taken into account.
	 */
	public void processFinalCongestionEvents() {
		for (Id<Person> affectedPersonId : this.affectedPersonId2congestionEventsToProcess.keySet()) {
			processCongestionEventsForAffectedPerson(affectedPersonId, Time.UNDEFINED_TIME, null);
		}
	}
	
	private void processCongestionEventsForAffectedPerson(Id<Person> personId, double activityEndTime, Id<Link> linkId) {
		
		// First, compute the agent's total (affected) delay from the previous trip.
		double totalDelayThisPerson = 0.0;
		if (this.affectedPersonId2delayToProcess.containsKey(personId)) {
			totalDelayThisPerson = this.affectedPersonId2delayToProcess.get(personId);
		}
		
		if (totalDelayThisPerson > 0.) {
			// The agent was delayed on the previous trip...
			// ... now calculate the agent's disutility from being late at the current activity: score(arrivalTime_ohneDelay) - score(arrivalTime_withDelay)			
			double activityDelayDisutility = 0.;
			
			// First, check if the plan completed is completed, i.e. if the agent has arrived at an activity (after being delayed)
			if (this.personId2currentActivityType.containsKey(personId) && this.personId2currentActivityStartTime.containsKey(personId)) {
				// Yes, the plan seems to be completed.
				
				if (activityEndTime == Time.UNDEFINED_TIME) {
					// The end time is undefined...
												
					// ... now handle the first and last OR overnight activity. This is figured out by the scoring function itself (depending on the activity types).
						
					Activity activityMorning = PopulationUtils.createActivityFromLinkId(this.personId2firstActivityType.get(personId), linkId);
					activityMorning.setEndTime(this.personId2firstActivityEndTime.get(personId));
					
					Activity activityEvening = PopulationUtils.createActivityFromLinkId(this.personId2currentActivityType.get(personId), linkId);
					activityEvening.setStartTime(this.personId2currentActivityStartTime.get(personId));
						
					activityDelayDisutility = marginaSumScoringFunction.getOvernightActivityDelayDisutility(activityMorning, activityEvening, totalDelayThisPerson);
					
				} else {
					// The activity has an end time indicating a 'normal' activity.
					
					Activity activity = PopulationUtils.createActivityFromLinkId(this.personId2currentActivityType.get(personId), linkId);
					activity.setStartTime(this.personId2currentActivityStartTime.get(personId));
					activity.setEndTime(activityEndTime);	
					activityDelayDisutility = marginaSumScoringFunction.getNormalActivityDelayDisutility(activity, totalDelayThisPerson);
				}
				
			} else {
				// No, there is no information about the current activity which indicates that the trip (with the delay) was not completed.
				
				if (incompletedPlanWarning <= 10) {
					log.warn("Agent " + personId + " has not yet completed the plan/trip (the agent is probably stucking). Cannot compute the disutility of being late at this activity. "
							+ "Something like the disutility of not arriving at the activity is required. Try to avoid this by setting a smaller stuck time period.");
					log.warn("Setting the disutilty of being delayed on the previous trip using the config parameters; assuming the marginal disutility of being delayed at the (hypothetical) activity to be equal to beta_performing: " + this.scenario.getConfig().planCalcScore().getPerforming_utils_hr());
				
					if (incompletedPlanWarning == 10) {
						log.warn("Additional warnings of this type are suppressed.");
					}
					incompletedPlanWarning++;
				}
				activityDelayDisutility = (totalDelayThisPerson / 3600.) * this.scenario.getConfig().planCalcScore().getPerforming_utils_hr();
			}
			
			// Calculate the agent's trip delay disutility (could be done similar to the activity delay disutility).
			double tripDelayDisutility = (totalDelayThisPerson / 3600.) * this.scenario.getConfig().planCalcScore().getModes().get(TransportMode.car).getMarginalUtilityOfTraveling() * (-1);
			
			// Translate the disutility into monetary units.
			double totalDelayCost = (activityDelayDisutility + tripDelayDisutility) / this.scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();
			double delayCostPerSecond = totalDelayCost / totalDelayThisPerson;
			
			// store the VTTS for analysis purposes
			if (this.personId2VTTSh.containsKey(personId)) {
				this.personId2VTTSh.get(personId).add(delayCostPerSecond * 3600);
			} else {
				List<Double> vTTSh = new ArrayList<>();
				vTTSh.add(delayCostPerSecond * 3600.);
				this.personId2VTTSh.put(personId, vTTSh);
			}
			// Go through the congestion events and charge each causing agent his/her contribution to the delay cost: caused delay * activity delay cost per second + caused delay * trip delay cost per second
			
			for (CongestionEvent congestionEvent : this.affectedPersonId2congestionEventsToProcess.get(personId)) {
				
				double amount = this.factor * congestionEvent.getDelay() * delayCostPerSecond * (-1);
				this.amountSum = this.amountSum + amount;
				
				if (activityEndTime == Time.UNDEFINED_TIME) {
					activityEndTime = this.scenario.getConfig().qsim().getEndTime();
				}
				
				PersonMoneyEvent moneyEvent = new PersonMoneyEvent(activityEndTime, congestionEvent.getCausingAgentId(), amount);				
				this.events.processEvent(moneyEvent);
				
				PersonLinkMoneyEvent linkMoneyEvent = new PersonLinkMoneyEvent(activityEndTime, congestionEvent.getCausingAgentId(), congestionEvent.getLinkId(), amount, congestionEvent.getEmergenceTime(), "congestion");
				this.events.processEvent(linkMoneyEvent);
			}
		}
	}
	
	public void printAvgVTTSperPerson(String fileName) {
		
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("person Id;VTTS (money/hour)");
			bw.newLine();
			
			for (Id<Person> personId : this.personId2VTTSh.keySet()){
				double vttsSum = 0.;
				double counter = 0;
				for (Double vTTS : this.personId2VTTSh.get(personId)){
					vttsSum = vttsSum + vTTS;
					counter++;
				}
				bw.write(personId + ";" + (vttsSum / counter) );
				bw.newLine();	
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void printVTTS(String fileName) {
		
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("person Id;VTTS (money/hour)");
			bw.newLine();
			
			for (Id<Person> personId : this.personId2VTTSh.keySet()){
				for (Double vTTS : this.personId2VTTSh.get(personId)){
					bw.write(personId + ";" + vTTS);
					bw.newLine();		
				}
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public double getAmountSum() {
		return amountSum;
	}
	
}