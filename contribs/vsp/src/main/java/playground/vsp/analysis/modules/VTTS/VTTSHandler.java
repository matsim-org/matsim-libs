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

package org.matsim.vtts;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.commons.math.stat.StatUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.StageActivityTypeIdentifier;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.utils.collections.Tuple;


/**
 * This analysis computes the effective value of travel time savings (VTTS) for each agent and each trip.
 * The basic idea is to repeat the scoring for an earlier arrival time (or shorter travel time) and to compute the score difference.
 * The score difference is used to compute the agent's trip-specific VTTS applying a linearization.
 *
 * @author ikaddoura
 *
 */
public class VTTSHandler implements ActivityStartEventHandler, ActivityEndEventHandler, PersonDepartureEventHandler, TransitDriverStartsEventHandler {


	private static final Logger log = LogManager.getLogger( VTTSHandler.class );
	private static int incompletedPlanWarning = 0;
	private static int noCarVTTSWarning = 0;
	private static int noTripVTTSWarning = 0;
	private static int noTripNrWarning = 0;

	private final Scenario scenario;
	private int currentIteration;


	private final Set<Id<Person>> personIdsToBeIgnored = new HashSet<>();

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

	private final double defaultVTTS_moneyPerHour; // for the car mode!

	private final ScoringParametersForPerson scoringParametersForPerson;


	@Inject
	VTTSHandler( Scenario scenario, ScoringParametersForPerson scoringParametersForPerson ) {
		// yyyy it would (presumably) be much better to pull the scoring function from injection.  Rather than self-constructing the
		// scoring function here, where we need to rely on having the same ("default") scoring function in the model implementation.
		// Which we almost surely do not have (e.g. bicycle scoring addition, bus penalty addition, ...).  Also see a similar comment further
		// down, where the local scoring fct is constructed.  kai, gr, jul'25

		if (scenario.getConfig().scoring().getMarginalUtilityOfMoney() == 0.) {
			log.warn("The marginal utility of money must not be 0.0. The VTTS is computed in Money per Time.");
		}


//		this.vttsCalculationMethod = vttsCalculationMethod;
//		this.modesToBeSkipped = helpLegModes;
//		this.stageActivitySubString = stageActivitySubString;
		this.scenario = scenario;
		this.scoringParametersForPerson = scoringParametersForPerson;
		this.currentIteration = Integer.MIN_VALUE;
		this.defaultVTTS_moneyPerHour =
				(this.scenario.getConfig().scoring().getPerforming_utils_hr()
						 + this.scenario.getConfig().scoring().getModes().get( TransportMode.car ).getMarginalUtilityOfTraveling() * (-1.0)
				) / this.scenario.getConfig().scoring().getMarginalUtilityOfMoney();
	}

	@Override
	public void reset(int iteration) {

		this.currentIteration = iteration;
		log.warn("Resetting VTTS information from previous iteration.");

		incompletedPlanWarning = 0;
		noCarVTTSWarning = 0;

		this.personIdsToBeIgnored.clear();
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
	public void handleEvent(TransitDriverStartsEvent event) {
		personIdsToBeIgnored.add(event.getDriverId());
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {

		if (isModeToBeSkipped(event.getRoutingMode()) || this.personIdsToBeIgnored.contains(event.getPersonId())) {
			// skip

		} else {
			this.departedPersonIds.add(event.getPersonId());
			this.personId2currentTripMode.put(event.getPersonId(), event.getRoutingMode());


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
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {

		if ( StageActivityTypeIdentifier.isStageActivity( event.getActType() ) || this.personIdsToBeIgnored.contains(event.getPersonId() )) {
			// skip
		} else {
			this.personId2currentActivityStartTime.put(event.getPersonId(), event.getTime());
			this.personId2currentActivityType.put(event.getPersonId(), event.getActType());
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {

		if (event.getActType().equals(VrpAgentLogic.BEFORE_SCHEDULE_ACTIVITY_TYPE)) {
			this.personIdsToBeIgnored.add(event.getPersonId());
		}

		if ( StageActivityTypeIdentifier.isStageActivity( event.getActType() ) || this.personIdsToBeIgnored.contains(event.getPersonId() )) {
			// skip
		} else {
			if (this.personId2currentActivityStartTime.containsKey(event.getPersonId())) {
				// This is not the first activity...

				// ... now process all congestion events thrown during the trip to the activity which has just ended, ...
				computeVTTS(event.getPersonId(), event.getTime());

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
	}

	/*
	 * This method has to be called after parsing the events. Here, the the last / overnight activity is taken into account.
	 */
	public void computeFinalVTTS() {
		for (Id<Person> affectedPersonId : this.departedPersonIds) {
			computeVTTS(affectedPersonId, Double.NEGATIVE_INFINITY);
		}
	}

	private void computeVTTS(Id<Person> personId, double activityEndTime){

		Person person = scenario.getPopulation().getPersons().get( personId );

	//	this.scoringParametersForPerson.getScoringParameters( person ).marginalUtilityOfMoney;

		if( this.personId2currentTripMode.get( personId ) == null ){
			// No mode stored for this person and trip. This indicates that the current trip mode was skipped.
			// Thus, do not compute any VTTS for this trip.
			return;
		}
		double activityDelayDisutilityOneSec = 0.;

		// First, check if the plan completed is completed, i.e. if the agent has arrived at an activity
		person = this.scenario.getPopulation().getPersons().get( personId );
		if( this.personId2currentActivityType.containsKey( personId ) && this.personId2currentActivityStartTime.containsKey( personId ) ){
			// the second condition was already tested earlier.

			String subpop = PopulationUtils.getSubpopulation( person );

			final org.matsim.vtts.MarginalSumScoringFunction marginalSumScoringFunction =
					new org.matsim.vtts.MarginalSumScoringFunction(
							new ScoringParameters.Builder( scenario.getConfig().scoring(), scenario.getConfig().scoring().getScoringParameters( subpop ), scenario.getConfig().scenario() ).build() );
			// yyyy it would (presumably) be much better to pull the scoring function from injection.  Rather than self-constructing the
			// scoring function here, where we need to rely on having the same ("default") scoring function in the model implementation.
			// Which we almost surely do not have (e.g. bicycle scoring addition, bus penalty addition, ...).  kai, gr, jul'25

			if( activityEndTime == Double.NEGATIVE_INFINITY ){
				// The end time is undefined...

				// ... now handle the first and last OR overnight activity. This is figured out by the scoring function itself (depending on the activity types).

				Activity activityMorning = PopulationUtils.createActivityFromLinkId( this.personId2firstActivityType.get( personId ), null );
				activityMorning.setEndTime( this.personId2firstActivityEndTime.get( personId ) );

				Activity activityEvening = PopulationUtils.createActivityFromLinkId( this.personId2currentActivityType.get( personId ), null );
				activityEvening.setStartTime( this.personId2currentActivityStartTime.get( personId ) );

				activityDelayDisutilityOneSec = marginalSumScoringFunction.getOvernightActivityDelayDisutility( activityMorning, activityEvening, 1.0 );

			} else{
				// The activity has an end time indicating a 'normal' activity.

				Activity activity = PopulationUtils.createActivityFromLinkId( this.personId2currentActivityType.get( personId ), null );
				activity.setStartTime( this.personId2currentActivityStartTime.get( personId ) );
				activity.setEndTime( activityEndTime );
				activityDelayDisutilityOneSec = marginalSumScoringFunction.getNormalActivityDelayDisutility( activity, 1.0 );
			}

		} else{
			// No, there is no information about the current activity which indicates that the trip (with the delay) was not completed.

			if( incompletedPlanWarning <= 10 ){
				log.warn( "Agent " + personId + " has not yet completed the plan/trip (the agent is probably stucking). Cannot compute the disutility of being late at this activity. "
								  + "Something like the disutility of not arriving at the activity is required. Try to avoid this by setting a smaller stuck time period." );
				log.warn(
						"Setting the disutilty of being delayed on the previous trip using the config parameters; assuming the marginal disutility of being delayed at the (hypothetical) activity to be equal to beta_performing: " + this.scenario.getConfig().scoring().getPerforming_utils_hr() );

				if( incompletedPlanWarning == 10 ){
					log.warn( Gbl.FUTURE_SUPPRESSED );
				}
				incompletedPlanWarning++;
			}
			activityDelayDisutilityOneSec = (1.0 / 3600.) * this.scenario.getConfig().scoring().getPerforming_utils_hr();
		}

		// Calculate the agent's trip delay disutility.
		// (Could be done similarly to the activity delay disutility. As long as it is computed linearly, the following should be okay.)
		String mode = this.personId2currentTripMode.get( personId );
		double marginalUtilityOfTraveling = 0.;
		if( this.scenario.getConfig().scoring().getModes().get( mode ) != null ){
			marginalUtilityOfTraveling = this.scenario.getConfig().scoring().getModes().get( mode ).getMarginalUtilityOfTraveling();
		} else{
			log.warn( "Could not identify the marginal utility of traveling for mode " + mode + ". "
							  + "Setting this value to zero. (Probably using subpopulations...)" );
		}
		double tripDelayDisutilityOneSec = (1.0 / 3600.) * marginalUtilityOfTraveling * (-1);

		// Translate the disutility into monetary units.
		double marginalUtilityOfMoney = scoringParametersForPerson.getScoringParameters(scenario.getPopulation().getPersons().get(personId)).marginalUtilityOfMoney;
		double delayCostPerSec_usingActivityDelayOneSec = (activityDelayDisutilityOneSec + tripDelayDisutilityOneSec) / marginalUtilityOfMoney;

		// store the VTTS for analysis purposes
		if( this.personId2VTTSh.containsKey( personId ) ){

			this.personId2VTTSh.get( personId ).add( delayCostPerSec_usingActivityDelayOneSec * 3600 );
			this.personId2TripNr2VTTSh.get( personId ).put( this.personId2currentTripNr.get( personId ), delayCostPerSec_usingActivityDelayOneSec * 3600 );
			this.personId2TripNr2Mode.get( personId ).put( this.personId2currentTripNr.get( personId ), this.personId2currentTripMode.get( personId ) );

		} else{

			List<Double> vTTSh = new ArrayList<>();
			vTTSh.add( delayCostPerSec_usingActivityDelayOneSec * 3600. );
			this.personId2VTTSh.put( personId, vTTSh );

			Map<Integer, Double> tripNr2VTTSh = new HashMap<>();
			tripNr2VTTSh.put( this.personId2currentTripNr.get( personId ), delayCostPerSec_usingActivityDelayOneSec * 3600. );
			this.personId2TripNr2VTTSh.put( personId, tripNr2VTTSh );

			Map<Integer, String> tripNr2Mode = new HashMap<>();
			tripNr2Mode.put( this.personId2currentTripNr.get( personId ), this.personId2currentTripMode.get( personId ) );
			this.personId2TripNr2Mode.put( personId, tripNr2Mode );
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
		printVTTS(fileName, TransportMode.car);
	}

	public void printVTTS(String fileName, String mode) {

		File file = new File(fileName);

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("person Id;TripNr;Mode;VTTS (money/hour)");
			bw.newLine();

			for (Id<Person> personId : this.personId2TripNr2VTTSh.keySet()){
				for (Integer tripNr : this.personId2TripNr2VTTSh.get(personId).keySet()){
					if (this.personId2TripNr2Mode.get(personId).get(tripNr).equals(mode)) {
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

					if (this.personId2TripNr2Mode.get(id).get(tripNrOfGivenTime).equals(TransportMode.car)) {
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

	public void printVTTSstatistics(String fileName, String mode, Tuple<Double, Double> fromToTime_sec) {

		List<Double> vttsFiltered = new ArrayList<>();

		for (Id<Person> personId : this.personId2TripNr2VTTSh.keySet()){
			for (Integer tripNr : this.personId2TripNr2VTTSh.get(personId).keySet()){

				boolean considerTrip = true;

				if (mode != null) {
					if (this.personId2TripNr2Mode.get(personId).get(tripNr).equals(mode)) {
						// consider this trip
					} else {
						considerTrip = false;
					}
				}

				if (fromToTime_sec != null) {
					if (this.personId2TripNr2DepartureTime.get(personId).get(tripNr) >= fromToTime_sec.getFirst()
								&& this.personId2TripNr2DepartureTime.get(personId).get(tripNr) < fromToTime_sec.getSecond()) {
						// consider this trip
					} else {
						considerTrip = false;
					}
				}

				if (considerTrip) {
					vttsFiltered.add(this.personId2TripNr2VTTSh.get(personId).get(tripNr));
				}

			}
		}

		double[] vttsArray = new double[vttsFiltered.size()];

		int counter = 0;
		for (Double vtts : vttsFiltered) {
			vttsArray[counter] = vtts;
			counter++;
		}

		File file = new File(fileName);

		try {

			BufferedWriter bw = new BufferedWriter(new FileWriter(file));

			bw.write("5% percentile ; " + StatUtils.percentile(vttsArray, 5.0));
			bw.newLine();

			bw.write("25% percentile ; " + StatUtils.percentile(vttsArray, 25.0));
			bw.newLine();

			bw.write("50% percentile (median) ; " + StatUtils.percentile(vttsArray, 50.0));
			bw.newLine();

			bw.write("75% percentile ; " + StatUtils.percentile(vttsArray, 75.0));
			bw.newLine();

			bw.write("95% percentile ; " + StatUtils.percentile(vttsArray, 95.0));
			bw.newLine();

			bw.write("mean ; " + StatUtils.mean(vttsArray));
			bw.newLine();

			bw.write("MIN ; " + StatUtils.min(vttsArray));
			bw.newLine();

			bw.write("MAX ; " + StatUtils.max(vttsArray));
			bw.newLine();

			bw.write("Variance ; " + StatUtils.variance(vttsArray));
			bw.newLine();

			bw.close();
			log.info("Output written to " + fileName);

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private boolean isModeToBeSkipped(String legMode) {
		/*for (String modeToBeSkipped : this.modesToBeSkipped) {
			if (legMode==null) {
				return true;
			}
			if (legMode.equals(modeToBeSkipped)) {
				return true;
			}

		} */
		return false;
	}


	private double computeAvgIncome(Population population) {

		log.info("reading income attribute using " + PersonUtils.class + " of all agents and compute global average.\n" +
						 "Make sure to set this attribute only to appropriate agents (i.e. true 'persons' and not freight agents) \n" +
						 "Income values <= 0 are ignored. Agents that have negative or 0 income will use the marginalUtilityOfMoney in their subpopulation's scoring params..");
		OptionalDouble averageIncome = population.getPersons().values().stream()
												 //consider only agents that have a specific income provided
												 .filter(person -> PersonUtils.getIncome(person) != null)
												 .mapToDouble(PersonUtils::getIncome)
												 .filter(dd -> dd > 0)
												 .average();

		if (averageIncome.isEmpty()) {
			throw new RuntimeException("you have enabled income dependent scoring but there is not a single income attribute in the population! " +
											   "If you are not aiming for person-specific marginalUtilityOfMoney, better use other PersonScoringParams, e.g. SubpopulationPersonScoringParams, which have higher performance." +
											   "Otherwise, please provide income attributes in the population...");
		} else {
			log.info("global average income is " + averageIncome);
			return averageIncome.getAsDouble();
		}
	}

}
