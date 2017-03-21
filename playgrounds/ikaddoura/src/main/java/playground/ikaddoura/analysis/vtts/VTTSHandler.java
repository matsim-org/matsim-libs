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
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.utils.misc.Time;

import playground.vsp.congestion.handlers.MarginalSumScoringFunction;


/**
 * This handler calculates the value of travel time savings (VTTS) for each agent and each trip.
 * The idea is to repeat the scoring for an earlier arrival time (or shorter travel time) and compute the score difference.
 * The score difference is used to compute the agent's trip-specific VTTS.
 * 
 * In some cases, a VTTS cannot be computed, for example in case of an agent stuck event. In these cases, the default VTTS is returned.
 * 
 * 
 * @author ikaddoura
 *
 */
public class VTTSHandler implements ActivityStartEventHandler, ActivityEndEventHandler, PersonDepartureEventHandler {

	private final static Logger log = Logger.getLogger(VTTSHandler.class);
	private static int incompletedPlanWarning = 0;
	private static int noCarVTTSWarning = 0;
	private static int noTripVTTSWarning = 0;
	private static int noTripNrWarning = 0;
	
	private final Scenario scenario;
	private int currentIteration;
	
	private final Set<Id<Person>> departedPersonIds = new HashSet<>();
	private final Map<Id<Person>, Double> personId2currentActivityStartTime = new HashMap<>();
	private final Map<Id<Person>, Double> personId2firstActivityEndTime = new HashMap<>();
	private final Map<Id<Person>, String> personId2currentActivityType = new HashMap<>();
	private final Map<Id<Person>, String> personId2firstActivityType = new HashMap<>();
	private final Map<Id<Person>, Integer> personId2currentTripNr = new HashMap<>();
	private final Map<Id<Person>, String> personId2currentTripMode = new HashMap<>();
	
	private final Map<Id<Person>, List<Double>> personId2VTTSh = new HashMap<>();
	private final Map<Id<Person>, Map<Integer, Double>> personId2TripNr2VTTSh = new HashMap<>();
	private final Map<Id<Person>, Map<Integer, String>> personId2TripNr2Mode = new HashMap<>();
	
	// to get the trip number for any given time
	private final Map<Id<Person>, Map<Integer, Double>> personId2TripNr2DepartureTime = new HashMap<>();
		
	private final MarginalSumScoringFunction marginaSumScoringFunction;
	private final double defaultVTTS_moneyPerHour; // for the car mode!
	
	public VTTSHandler(Scenario scenario) {
		
		if (scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney() == 0.) {
			log.warn("The marginal utility of money must not be 0.0. The VTTS is computed in Money per Time. Aborting...");
		}
		this.scenario = scenario;
		this.currentIteration = Integer.MIN_VALUE;
		this.defaultVTTS_moneyPerHour =
				(this.scenario.getConfig().planCalcScore().getPerforming_utils_hr()
				+ this.scenario.getConfig().planCalcScore().getModes().get( TransportMode.car ).getMarginalUtilityOfTraveling() * (-1.0)
				) / this.scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();

		this.marginaSumScoringFunction =
				new MarginalSumScoringFunction(
						new ScoringParameters.Builder(scenario.getConfig().planCalcScore(), scenario.getConfig().planCalcScore().getScoringParameters(null), scenario.getConfig().scenario()).build());

	}

	@Override
	public void reset(int iteration) {
		
		this.currentIteration = iteration;
		log.warn("Resetting VTTS information from previous iteration.");
		
		incompletedPlanWarning = 0;
		noCarVTTSWarning = 0;
		
		this.departedPersonIds.clear();
		this.personId2currentActivityStartTime.clear();
		this.personId2firstActivityEndTime.clear();
		this.personId2currentActivityType.clear();
		this.personId2firstActivityType.clear();
		this.personId2currentTripNr.clear();
		this.personId2currentTripMode.clear();
		
		this.personId2VTTSh.clear();
		this.personId2TripNr2VTTSh.clear();
		this.personId2TripNr2Mode.clear();
		
		this.personId2TripNr2DepartureTime.clear();
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		this.departedPersonIds.add(event.getPersonId());
		
		this.personId2currentTripMode.put(event.getPersonId(), event.getLegMode());
		
		if (this.personId2currentTripNr.containsKey(event.getPersonId())){
			this.personId2currentTripNr.put(event.getPersonId(), this.personId2currentTripNr.get(event.getPersonId()) + 1);
			
		} else {
			this.personId2currentTripNr.put(event.getPersonId(), 1);
		}
		
		if (this.personId2TripNr2DepartureTime.containsKey(event.getPersonId())) {
			this.personId2TripNr2DepartureTime.get(event.getPersonId()).put(this.personId2currentTripNr.get(event.getPersonId()), event.getTime());
			
		} else {
			Map<Integer, Double> tripNr2departureTime = new HashMap<>();
			tripNr2departureTime.put(this.personId2currentTripNr.get(event.getPersonId()), event.getTime());
			this.personId2TripNr2DepartureTime.put(event.getPersonId(), tripNr2departureTime);
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
					
				Activity activityMorning = PopulationUtils.createActivityFromLinkId(this.personId2firstActivityType.get(personId), linkId);
				activityMorning.setEndTime(this.personId2firstActivityEndTime.get(personId));
				
				Activity activityEvening = PopulationUtils.createActivityFromLinkId(this.personId2currentActivityType.get(personId), linkId);
				activityEvening.setStartTime(this.personId2currentActivityStartTime.get(personId));
					
				activityDelayDisutilityOneSec = marginaSumScoringFunction.getOvernightActivityDelayDisutility(activityMorning, activityEvening, 1.0);
				
			} else {
				// The activity has an end time indicating a 'normal' activity.
				
				Activity activity = PopulationUtils.createActivityFromLinkId(this.personId2currentActivityType.get(personId), linkId);
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
		
		// Calculate the agent's trip delay disutility.
		// (Could be done similar to the activity delay disutility. As long as it is computed linearly, the following should be okay.)
		double tripDelayDisutilityOneSec = 0.;
		
		if (this.personId2currentTripMode.get(personId).equals("car")) {
			tripDelayDisutilityOneSec = (1.0 / 3600.) * this.scenario.getConfig().planCalcScore().getModes().get(TransportMode.car).getMarginalUtilityOfTraveling() * (-1);
			
		} else if (this.personId2currentTripMode.get(personId).equals("walk")) {
			tripDelayDisutilityOneSec = (1.0 / 3600.) * this.scenario.getConfig().planCalcScore().getModes().get(TransportMode.walk).getMarginalUtilityOfTraveling() * (-1);

		} else if (this.personId2currentTripMode.get(personId).equals("pt")) {
			tripDelayDisutilityOneSec = (1.0 / 3600.) * this.scenario.getConfig().planCalcScore().getModes().get(TransportMode.pt).getMarginalUtilityOfTraveling() * (-1);

		} else if (this.personId2currentTripMode.get(personId).equals("bike")) {
			tripDelayDisutilityOneSec = (1.0 / 3600.) * this.scenario.getConfig().planCalcScore().getModes().get(TransportMode.bike).getMarginalUtilityOfTraveling() * (-1);
			
		} else {
			tripDelayDisutilityOneSec = (1.0 / 3600.) * this.scenario.getConfig().planCalcScore().getModes().get(TransportMode.other).getMarginalUtilityOfTraveling() * (-1);
		}
		
		// Translate the disutility into monetary units.
		double delayCostPerSec_usingActivityDelayOneSec = (activityDelayDisutilityOneSec + tripDelayDisutilityOneSec) / this.scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();

		// store the VTTS for analysis purposes
		if (this.personId2VTTSh.containsKey(personId)) {
					
			this.personId2VTTSh.get(personId).add(delayCostPerSec_usingActivityDelayOneSec * 3600);
			this.personId2TripNr2VTTSh.get(personId).put(this.personId2currentTripNr.get(personId), delayCostPerSec_usingActivityDelayOneSec * 3600);
			this.personId2TripNr2Mode.get(personId).put(this.personId2currentTripNr.get(personId), this.personId2currentTripMode.get(personId));
	
		} else {

			List<Double> vTTSh = new ArrayList<>();
			vTTSh.add(delayCostPerSec_usingActivityDelayOneSec * 3600.);
			this.personId2VTTSh.put(personId, vTTSh);

			Map<Integer, Double> tripNr2VTTSh = new HashMap<>();
			tripNr2VTTSh.put(this.personId2currentTripNr.get(personId), delayCostPerSec_usingActivityDelayOneSec * 3600.);
			this.personId2TripNr2VTTSh.put(personId, tripNr2VTTSh);
			
			Map<Integer, String> tripNr2Mode = new HashMap<>();
			tripNr2Mode.put(this.personId2currentTripNr.get(personId), this.personId2currentTripMode.get(personId));
			this.personId2TripNr2Mode.put(personId, tripNr2Mode);
		}
	}
	
	public void printVTTS(String fileName) {
		
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("person Id;TripNr;Mode;VTTS (money/hour)");
			bw.newLine();
			
			for (Id<Person> personId : this.personId2TripNr2VTTSh.keySet()){
				for (Integer tripNr : this.personId2TripNr2VTTSh.get(personId).keySet()){
					bw.write(personId + ";" + tripNr + ";" + this.personId2TripNr2Mode.get(personId).get(tripNr) + ";" + this.personId2TripNr2VTTSh.get(personId).get(tripNr));
					bw.newLine();		
				}
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void printCarVTTS(String fileName) {
		
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("person Id;TripNr;Mode;VTTS (money/hour)");
			bw.newLine();
			
			for (Id<Person> personId : this.personId2TripNr2VTTSh.keySet()){
				for (Integer tripNr : this.personId2TripNr2VTTSh.get(personId).keySet()){
					if (this.personId2TripNr2Mode.get(personId).get(tripNr).equals("car")) {
						bw.write(personId + ";" + tripNr + ";" + this.personId2TripNr2Mode.get(personId).get(tripNr) + ";" + this.personId2TripNr2VTTSh.get(personId).get(tripNr));
						bw.newLine();			
					}
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

	public Map<Id<Person>, Map<Integer, Double>> getPersonId2TripNr2VTTSh() {
		return personId2TripNr2VTTSh;
	}

	public Map<Id<Person>, Map<Integer, String>> getPersonId2TripNr2Mode() {
		return personId2TripNr2Mode;
	}

	public double getAvgVTTSh(Id<Person> id) {
		double sum = 0.;
		int counter = 0;
		
		if (this.personId2VTTSh.containsKey(id)) {
			
			for (Double vtts : this.personId2VTTSh.get(id)) {
				sum += vtts;
				counter++;
			}
			
			double avgVTTS = sum / counter;
			return avgVTTS;

		} else {
			
			log.warn("Couldn't find any VTTS of person " + id + ". Using the default VTTS...");
			return this.defaultVTTS_moneyPerHour;
		}
	}
	
	public double getAvgVTTSh(Id<Person> id, String mode) {
		double sum = 0.;
		int counter = 0;
		
		if (this.personId2TripNr2VTTSh.containsKey(id)) {
			for (Integer tripNr : this.personId2TripNr2VTTSh.get(id).keySet()) {
				if (this.personId2TripNr2Mode.get(id).get(tripNr).equals(mode)) {
					sum += this.personId2TripNr2VTTSh.get(id).get(tripNr);
					counter++;
				}
			}
			
			if (counter == 0) {
				log.warn("Couldn't find any VTTS of person " + id + " with transport mode + " + mode + ". Using the default VTTS...");
				return this.defaultVTTS_moneyPerHour;
			
			} else {
				double avgVTTSmode = sum / counter;
				return avgVTTSmode;
			}
			
		} else {
			log.warn("Couldn't find any VTTS of person " + id + ". Using the default VTTS...");
			return this.defaultVTTS_moneyPerHour;
		}
	}
	
	/**
	 * 
	 * @param id
	 * @param time
	 * 
	 * This method returns the car mode VTTS in money per hour for a person at a given time and can for example be used to calculate a travel disutility during routing.
	 * Based on the time, the trip Nr is computed and based on the trip number the VTTS is looked up.
	 * In case there is no VTTS information available such as in the initial iteration before event handling, the default VTTS is returned.
	 * 
	 * @return
	 */
	public double getCarVTTS(Id<Person> id, double time) {
			
		if (this.personId2TripNr2DepartureTime.containsKey(id)) {
			
			int tripNrOfGivenTime = Integer.MIN_VALUE;
			double departureTime = Double.MAX_VALUE;
			for (Integer tripNr : this.personId2TripNr2DepartureTime.get(id).keySet()) {
				if (time >= this.personId2TripNr2DepartureTime.get(id).get(tripNr)) {					
					if (this.personId2TripNr2DepartureTime.get(id).get(tripNr) <= departureTime) {
						departureTime = this.personId2TripNr2DepartureTime.get(id).get(tripNr);
						tripNrOfGivenTime = tripNr;
					}
				}
			}
			
			if (tripNrOfGivenTime == Integer.MIN_VALUE) {
			
				if (noTripNrWarning <= 3) {
					log.warn("Could not identify the trip number of person " + id + " at time " + time + "."
							+ " Trying to use the average car VTTS...");
				}
				if (noTripNrWarning == 3) {
					log.warn("Additional warnings of this type are suppressed.");
				}
				noTripNrWarning++;
				return this.getAvgVTTSh(id, TransportMode.car);
			
			} else {
				if (this.personId2TripNr2VTTSh.containsKey(id)) {
					
					if (this.personId2TripNr2Mode.get(id).get(tripNrOfGivenTime) == TransportMode.car) {
						// everything fine
						double vtts = this.personId2TripNr2VTTSh.get(id).get(tripNrOfGivenTime);			
						return vtts;
						
					} else {

						
						if (noCarVTTSWarning <= 3) {
							log.warn("In the previous iteration at the given time " + time + " the agent " + id + " was performing a trip with a different mode (" + this.personId2TripNr2Mode.get(id).get(tripNrOfGivenTime) + ")."
									+ "Trying to use the average car VTTS.");
							if (noCarVTTSWarning == 3) {
								log.warn("Additional warnings of this type are suppressed.");
							}
							noCarVTTSWarning++;
						}
						return this.getAvgVTTSh(id, TransportMode.car);
					}
					
				} else {
					if (noTripVTTSWarning <= 3) {
						log.warn("Could not find the VTTS of person " + id + " and trip number " + tripNrOfGivenTime + " (time: " + time + ")."
								+ " Trying to use the average car VTTS...");
					}
					if (noTripVTTSWarning == 3) {
						log.warn("Additional warnings of this type are suppressed.");
					}
					noTripVTTSWarning++;
					return this.getAvgVTTSh(id, TransportMode.car);
				}
			} 
			
		} else {
			
			if (this.currentIteration == Integer.MIN_VALUE) {
				// the initial iteration before handling any events
				return this.defaultVTTS_moneyPerHour;
			} else {
				throw new RuntimeException("This is not the initial iteration and there is no information available from the previous iteration. Aborting...");
			}
		}
	}
}
