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
package playground.ikaddoura.analysis.vtts;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.utils.misc.Time;

import playground.vsp.congestion.handlers.MarginalSumScoringFunction;


/**
 * This handler calculates the VTTS for each agent and each trip.
 * 
 * @author ikaddoura
 *
 */
public class VTTSHandler implements ActivityStartEventHandler, ActivityEndEventHandler, PersonDepartureEventHandler {

	private final static Logger log = Logger.getLogger(VTTSHandler.class);

	private static int incompletedPlanWarning = 0;
	
	private final Scenario scenario;
	
	private Set<Id<Person>> departedPersonIds = new HashSet<>();
	private Map<Id<Person>, Double> personId2currentActivityStartTime = new HashMap<Id<Person>, Double>();
	private Map<Id<Person>, Double> personId2firstActivityEndTime = new HashMap<Id<Person>, Double>();
	private Map<Id<Person>, String> personId2currentActivityType = new HashMap<Id<Person>, String>();
	private Map<Id<Person>, String> personId2firstActivityType = new HashMap<Id<Person>, String>();
	
	private Map<Id<Person>, List<Double>> personId2VTTSh = new HashMap<>();

	private MarginalSumScoringFunction marginaSumScoringFunction;
	
	public VTTSHandler(Scenario scenario) {
		
		this.scenario = scenario;
		this.marginaSumScoringFunction = new MarginalSumScoringFunction(CharyparNagelScoringParameters.getBuilder(scenario.getConfig().planCalcScore()).create());
	}

	@Override
	public void reset(int iteration) {
		
		incompletedPlanWarning = 0;
		
		this.departedPersonIds.clear();
		this.personId2currentActivityStartTime.clear();
		this.personId2firstActivityEndTime.clear();
		this.personId2currentActivityType.clear();
		this.personId2firstActivityType.clear();
		
		this.personId2VTTSh.clear();
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		this.departedPersonIds.add(event.getPersonId());
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
			computeVTTS(event.getPersonId(), event.getTime(), event.getLinkId());
			
			// ... update the status of the 'current' activity...
			this.personId2currentActivityType.remove(event.getPersonId());
			this.personId2currentActivityStartTime.remove(event.getPersonId());
			
			// ... and remove all processed congestion events. 
			this.departedPersonIds.remove(event.getPersonId());
	
		} else {
			// This is the first activity. The first and last / overnight activity are / is considered in a final step.
			// Therefore, the relevant information has to be stored.
			this.personId2firstActivityEndTime.put(event.getPersonId(), event.getTime());
			this.personId2firstActivityType.put(event.getPersonId(), event.getActType());
		}
	}

	/*
	 * This method has to be called after parsing the events. Here, the the last / overnight activity is taken into account.
	 */
	public void computeFinalVTTS() {
		for (Id<Person> affectedPersonId : this.departedPersonIds) {
			computeVTTS(affectedPersonId, Time.UNDEFINED_TIME, null);
		}
	}
	
	private void computeVTTS(Id<Person> personId, double activityEndTime, Id<Link> linkId) {
		
		double activityDelayDisutilityOneSec = 0.;
		
		// First, check if the plan completed is completed, i.e. if the agent has arrived at an activity
		if (this.personId2currentActivityType.containsKey(personId) && this.personId2currentActivityStartTime.containsKey(personId)) {
			// Yes, the plan seems to be completed.
			
			if (activityEndTime == Time.UNDEFINED_TIME) {
				// The end time is undefined...
											
				// ... now handle the first and last OR overnight activity. This is figured out by the scoring function itself (depending on the activity types).
					
				ActivityImpl activityMorning = new ActivityImpl(this.personId2firstActivityType.get(personId), linkId);
				activityMorning.setEndTime(this.personId2firstActivityEndTime.get(personId));
				
				ActivityImpl activityEvening = new ActivityImpl(this.personId2currentActivityType.get(personId), linkId);
				activityEvening.setStartTime(this.personId2currentActivityStartTime.get(personId));
					
				activityDelayDisutilityOneSec = marginaSumScoringFunction.getOvernightActivityDelayDisutility(activityMorning, activityEvening, 1.0);
				
			} else {
				// The activity has an end time indicating a 'normal' activity.
				
				ActivityImpl activity = new ActivityImpl(this.personId2currentActivityType.get(personId), linkId);
				activity.setStartTime(this.personId2currentActivityStartTime.get(personId));
				activity.setEndTime(activityEndTime);	
				activityDelayDisutilityOneSec = marginaSumScoringFunction.getNormalActivityDelayDisutility(activity, 1.);
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
			activityDelayDisutilityOneSec = (1.0 / 3600.) * this.scenario.getConfig().planCalcScore().getPerforming_utils_hr();
		}
		
		// Calculate the agent's trip delay disutility (could be done similar to the activity delay disutility).
		double tripDelayDisutilityOneSec = (1.0 / 3600.) * this.scenario.getConfig().planCalcScore().getTraveling_utils_hr() * (-1);
		
		// Translate the disutility into monetary units.
		double delayCostPerSec_usingActivityDelayOneSec = (activityDelayDisutilityOneSec + tripDelayDisutilityOneSec) / this.scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();
		
		// store the VTTS for analysis purposes
		if (this.personId2VTTSh.containsKey(personId)) {
			this.personId2VTTSh.get(personId).add(delayCostPerSec_usingActivityDelayOneSec * 3600);
		} else {
			List<Double> vTTSh = new ArrayList<>();
			vTTSh.add(delayCostPerSec_usingActivityDelayOneSec * 3600.);
			this.personId2VTTSh.put(personId, vTTSh);
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

}