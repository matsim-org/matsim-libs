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

package org.matsim.application.analysis.population;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.StageActivityTypeIdentifier;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.utils.misc.OptionalTime;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.util.*;


/**
 * This analysis computes the effective value of travel time savings (VTTS) for each agent and each trip.
 * The basic idea is to repeat the scoring for an earlier arrival time (or shorter travel time) and to compute the score difference.
 * The score difference is used to compute the agent's trip-specific VTTS applying a linearization.
 *
 * @author ikaddoura
 *
 */
public final class VTTSHandlerKN implements ActivityStartEventHandler, ActivityEndEventHandler, PersonDepartureEventHandler, TransitDriverStartsEventHandler {
	// the constructor is package-private so having the class public final is ok. kai, nov'25

	public static class TripData {
		public double mUTTSh;
		public String actType;
		public double actTypDur_h;
		public double actDur_h;
		double VTTSh;
		String mode;
		double departureTime = Double.NaN;
	}

	private static class SimData {
		public double margUtlOfMoney;
		double currentActivityStartTime = Double.NaN;
		String currentActivityType;
//		String currentOrIncomingTripMode;
		List<TripData> trips = new ArrayList<>();
		double firstActivityEndTime = Double.NaN;
		String firstActivityType ;
	}
	private final Map<Id<Person>,SimData> simDataMap = new HashMap<>();

	private static final Logger log = LogManager.getLogger( VTTSHandlerKN.class );
	private static int incompletedPlanWarning = 0;
	private static int noCarVTTSWarning = 0;
	private static int noTripVTTSWarning = 0;
	private static int noTripNrWarning = 0;

	private final Scenario scenario;
	private int currentIteration;

	private final Set<Id<Person>> personIdsToBeIgnored = new HashSet<>();

	private final Set<Id<Person>> departedPersonIds = new HashSet<>();

	private final double defaultVTTS_moneyPerHour; // for the car mode!

	private final ScoringParametersForPerson scoringParametersForPerson;


	@Inject VTTSHandlerKN( Scenario scenario, ScoringParametersForPerson scoringParametersForPerson ) {
		// yyyy it would (presumably) be much better to pull the scoring function from injection.  Rather than self-constructing the
		// scoring function here, where we need to rely on having the same ("default") scoring function in the model implementation.
		// Which we almost surely do not have (e.g. bicycle scoring addition, bus penalty addition, ...).  Also see a similar comment further
		// down, where the local scoring fct is constructed.  kai, gr, jul'25

		if (scenario.getConfig().scoring().getMarginalUtilityOfMoney() == 0.) {
			log.warn("The marginal utility of money must not be 0.0. The VTTS is computed in Money per Time.");
		}

		this.scenario = scenario;
		this.scoringParametersForPerson = scoringParametersForPerson;
		this.currentIteration = Integer.MIN_VALUE;
		this.defaultVTTS_moneyPerHour =
				(this.scenario.getConfig().scoring().getPerforming_utils_hr()
						 + this.scenario.getConfig().scoring().getModes().get( TransportMode.car ).getMarginalUtilityOfTraveling() * (-1.0)
				) / this.scenario.getConfig().scoring().getMarginalUtilityOfMoney();
	}

	public Map<Id<Person>,List<TripData>> getTripDataMap() {
		Map<Id<Person>,List<TripData>> tripDataMap = new LinkedHashMap<>();
		for( Map.Entry<Id<Person>, SimData> simDataEntry : simDataMap.entrySet() ){
			Id<Person> personId = simDataEntry.getKey();
			List<TripData> tripData = simDataEntry.getValue().trips;
			tripDataMap.put( personId, Collections.unmodifiableList( tripData ) );
		}
		return Collections.unmodifiableMap( tripDataMap );
	}

	@Override
	public void reset(int iteration) {

		this.currentIteration = iteration;
		log.warn("Resetting VTTS information from previous iteration.");

		incompletedPlanWarning = 0;
		noCarVTTSWarning = 0;

		this.personIdsToBeIgnored.clear();
		this.departedPersonIds.clear();

		this.simDataMap.clear();
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		personIdsToBeIgnored.add(event.getDriverId());
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {

		final Id<Person> personId = event.getPersonId();
		if (isModeToBeSkipped(event.getRoutingMode() ) || this.personIdsToBeIgnored.contains( personId )){
			return;
		}

		if( isToBeIgnored( personId ) ) return;

		SimData simData = simDataMap.computeIfAbsent( personId, k -> new SimData() );
		// (freight departs w/o a preceeding activity end)

		if ( !this.departedPersonIds.contains( personId ) ){
			this.departedPersonIds.add( personId );

			// in this way, there is only one trip record per trip.  I don't know how this was achieved in the previous code.
			TripData tripData = new TripData();
			simData.trips.add( tripData );
			tripData.departureTime = event.getTime();
			tripData.mode = event.getRoutingMode();
		}
	}
	private boolean isToBeIgnored( Id<Person> personId ){
		Person person = scenario.getPopulation().getPersons().get( personId );
		if ( person==null ) {
			personIdsToBeIgnored.add( personId );
			return true;
		}
		return false;
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {

		final Id<Person> personId = event.getPersonId();

		if ( StageActivityTypeIdentifier.isStageActivity( event.getActType() ) || this.personIdsToBeIgnored.contains( personId )) {
			return;
		}

		if( isToBeIgnored( personId ) ) return;

		SimData simData = simDataMap.get( personId );
		simData.currentActivityStartTime = event.getTime();
		simData.currentActivityType = event.getActType();
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		final Id<Person> personId = event.getPersonId();

		if (event.getActType().equals(VrpAgentLogic.BEFORE_SCHEDULE_ACTIVITY_TYPE )) {
			this.personIdsToBeIgnored.add( personId );
		}

		if ( StageActivityTypeIdentifier.isStageActivity( event.getActType() ) || this.personIdsToBeIgnored.contains( personId )) {
			return;
		}

		SimData simData = simDataMap.get( personId );
		if ( simData==null ) {
			// not seen this person before; is also first activity!
			simData = new SimData();
			simData.firstActivityEndTime = event.getTime();
			simData.firstActivityType = event.getActType();
			simDataMap.put( personId, simData );
		} else {
			// this is NOT the first activity ...

			// ... now process the events thrown during the trip to the activity which has just ended, ...
			computeVTTS( personId, OptionalTime.defined( event.getTime() ) );

			simData.currentActivityType = null;
			simData.currentActivityStartTime = Double.NaN;

			this.departedPersonIds.remove( personId );
		}

	}

	/*
	 * This method has to be called after parsing the events. Here, the the last / overnight activity is taken into account.
	 */
	public void computeFinalVTTS() {
		for (Id<Person> affectedPersonId : this.departedPersonIds) {
			computeVTTS( affectedPersonId, OptionalTime.undefined() );
		}
	}

	public Table getTablesawTripsTable() {
		StringColumn personIds = StringColumn.create( HeadersKN.personId );
		IntColumn tripIndices = IntColumn.create( HeadersKN.tripIdx );
		StringColumn modes = StringColumn.create( HeadersKN.mode );
		StringColumn acts = StringColumn.create( HeadersKN.activity );
		DoubleColumn typDurs = DoubleColumn.create( HeadersKN.typicalDuration );
		DoubleColumn actDurs = DoubleColumn.create( HeadersKN.activityDuration );
		DoubleColumn muttsValues = DoubleColumn.create( HeadersKN.muttsh );
		DoubleColumn vttsValues = DoubleColumn.create( HeadersKN.vttsh );
		DoubleColumn mUoMs = DoubleColumn.create( HeadersKN.mUoM );
		Table table = Table.create( personIds, mUoMs, tripIndices, modes, acts, actDurs, typDurs, muttsValues, vttsValues );

		for( Map.Entry<Id<Person>, SimData> entry : simDataMap.entrySet() ){
			Id<Person> personId = entry.getKey();
			SimData simData = entry.getValue();
			for ( int ii=0 ; ii<simData.trips.size(); ii++ ) {
				TripData trip = simData.trips.get( ii );
				personIds.append( personId.toString() );
				tripIndices.append( ii );
				modes.append( trip.mode );
				muttsValues.append( trip.mUTTSh);
				vttsValues.append( trip.VTTSh );
				acts.append( trip.actType );
				actDurs.append( trip.actDur_h );
				typDurs.append( trip.actTypDur_h );
				mUoMs.append( simData.margUtlOfMoney );
			}
		}

		return table;
	}

	private static int mUoMCnt =0;

	private void computeVTTS(Id<Person> personId, OptionalTime activityEndTime ){

		final SimData simData = this.simDataMap.get( personId );
		if ( simData.trips.getLast().mode == null ) {
			// No mode stored for this person and trip. This indicates that the current trip mode was skipped.
			// Thus, do not compute any VTTS for this trip.
			return;
		}

		double activityDelayDisutility_h;
		final ScoringConfigGroup scoringConfigGroup = this.scenario.getConfig().scoring();
		if ( simData.currentActivityType==null || Double.isNaN( simData.currentActivityStartTime ) ) {
			// the second condition was already tested earlier. (??)

			log.warn("incomplete plan; personId={}; actType={}; actStartTime={}", personId, simData.currentActivityType, simData.currentActivityStartTime );

			// removing the trip record since otherwise it will have entries that destroy taking averages:
			simData.trips.removeLast();

			return;
			// yyyy returning here will probably crash later. However, otherwise it may create garbage, and do this silently.  kai, nov'25

			// there is no information about the current activity which indicates that the trip (with the delay) was not completed.
//			activityDelayDisutility_h = 3600. * handleIncompletePlan( personId, scenario.getConfig().scoring().getPerforming_utils_hr() );
		} else {
			Person person = this.scenario.getPopulation().getPersons().get( personId );
			String subpop = PopulationUtils.getSubpopulation( person );

			final MarginalSumScoringFunction marginalSumScoringFunction =
					new MarginalSumScoringFunction(
							new ScoringParameters.Builder( scoringConfigGroup, scoringConfigGroup.getScoringParameters( subpop ), scenario.getConfig().scenario() ).build() );
			// yyyy it would (presumably) be much better to pull the scoring function from injection.  Rather than self-constructing the
			// scoring function here, where we need to rely on having the same ("default") scoring function in the model implementation.
			// Which we almost surely do not have (e.g. bicycle scoring addition, bus penalty addition, ...).  kai, gr, jul'25

			if( activityEndTime.isUndefined() ){
				// The end time is undefined...

				// ... now handle the first and last OR overnight activity. This is figured out by the scoring function itself (depending on the activity types).

				Activity activityMorning = PopulationUtils.createActivityFromLinkId( simData.firstActivityType, null );
				activityMorning.setEndTime( simData.firstActivityEndTime );

				Activity activityEvening = PopulationUtils.createActivityFromLinkId( simData.currentActivityType, null );
				activityEvening.setStartTime( simData.currentActivityStartTime );

				activityDelayDisutility_h = 3600. * marginalSumScoringFunction.getOvernightActivityDelayDisutility( activityMorning, activityEvening, 1.0 );

				simData.trips.getLast().actDur_h = (simData.firstActivityEndTime + 3600.*24 - simData.currentActivityStartTime)/3600. ;

			} else{
				// The activity has an end time indicating a 'normal' activity.

				Activity activity = PopulationUtils.createActivityFromLinkId( simData.currentActivityType, null );
				activity.setStartTime( simData.currentActivityStartTime );
				activity.setEndTime( activityEndTime.seconds() );
				activityDelayDisutility_h = 3600. * marginalSumScoringFunction.getNormalActivityDelayDisutility( personId, activity, 1.0 );
				simData.trips.getLast().actDur_h = (activityEndTime.seconds() - simData.currentActivityStartTime)/3600. ;

			}
		}

		// Calculate the agent's trip delay disutility.
		// (Could be done similarly to the activity delay disutility. As long as it is computed linearly, the following should be okay.)
		String mode = simData.trips.getLast().mode;
		double directMarginalUtilityOfTraveling = 0.;
		if( scoringConfigGroup.getModes().get( mode ) != null ){
			directMarginalUtilityOfTraveling = scoringConfigGroup.getModes().get( mode ).getMarginalUtilityOfTraveling();
		} else{
			log.warn( "Could not identify the marginal utility of traveling for mode={}. Setting this value to zero. (Probably using subpopulations...)", mode );
		}
		double tripDelayDisutility_h = directMarginalUtilityOfTraveling * (-1);

		final double mUTTS_h = (activityDelayDisutility_h + tripDelayDisutility_h) ;

		simData.trips.getLast().mUTTSh = mUTTS_h;

		// Translate the disutility into monetary units.
		double marginalUtilityOfMoney = scoringParametersForPerson.getScoringParameters(scenario.getPopulation().getPersons().get(personId)).marginalUtilityOfMoney;
		simData.margUtlOfMoney = marginalUtilityOfMoney;
		if ( mUoMCnt < 10 ){
			mUoMCnt++;
			log.info( "personId={}, actDelayDisutil={}; tripDelayDisutl={}; mUM={}", personId, activityDelayDisutility_h, tripDelayDisutility_h, marginalUtilityOfMoney );
			if ( mUoMCnt == 10 ) {
				log.info( Gbl.FUTURE_SUPPRESSED );
			}
		}

		simData.trips.getLast().VTTSh = mUTTS_h / marginalUtilityOfMoney;

		simData.trips.getLast().actType = simData.currentActivityType;
		simData.trips.getLast().actTypDur_h = scoringConfigGroup.getActivityParams( simData.currentActivityType ).getTypicalDuration().seconds() / 3600. ;

	}
	private static double handleIncompletePlan( Id<Person> personId, double performing_utils_hr ){
		double activityDelayDisutilityOneSec;
		if( incompletedPlanWarning <= 10 ){
			log.warn( "Agent " + personId + " has not yet completed the plan/trip (the agent is probably stucking). Cannot compute the disutility of being late at this activity. "
							  + "Something like the disutility of not arriving at the activity is required. Try to avoid this by setting a smaller stuck time period." );
			log.warn( "Setting the disutilty of being delayed on the previous trip using the config parameters; assuming the marginal disutility of being delayed at the " +
						  "(hypothetical) activity to be equal to beta_performing: " + performing_utils_hr );

			if( incompletedPlanWarning == 10 ){
				log.warn( Gbl.FUTURE_SUPPRESSED );
			}
			incompletedPlanWarning++;
		}
		activityDelayDisutilityOneSec = (1.0 / 3600.) * performing_utils_hr;
		return activityDelayDisutilityOneSec;
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


	/*private double computeAvgIncome(Population population) {

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
*/


/*
	public void printVTTS(String fileName) {
		File file = new File(fileName);

		try ( BufferedWriter bw = new BufferedWriter(new FileWriter(file)) ) {
			bw.write("person Id\tTripNr\tMode\tVTTS (money/hour)");
			bw.newLine();

			for( Map.Entry<Id<Person>, SimData> entry : simDataMap.entrySet() ){
				Id<Person> personId = entry.getKey();
				SimData simData = entry.getValue();
				for ( int tripNr = 0; tripNr < simData.trips.size(); tripNr++ ) {
					TripData tripData = simData.trips.get( tripNr );
					bw.write( personId + "\t" + tripNr + "\t" + tripData.mode + "\t" + tripData.VTTSh );
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

	*/
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
/*

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

	private static int cnt1 = 0;
	public void printVTTSHistogram( String fileName ) {

		long maxBin = 0;
		Map<Integer, Long> histogram = new HashMap<>();
		for( Map.Entry<Id<Person>, SimData> entry : simDataMap.entrySet() ){
			Id<Person> personId = entry.getKey();
			SimData simData = entry.getValue();
			for( TripData data : simData.trips ) {
				if ( data.VTTSh < 0.001 ) {
					if ( cnt1<100 ){
						cnt1++;
						log.warn( "personId={}; vttsh={}; tripMode={}; tripDpTime={}", personId, data.VTTSh, data.mode, data.departureTime );
						if ( cnt1==100 ) {
							log.warn( Gbl.FUTURE_SUPPRESSED );
						}
					}
				}
				int bin = (int) data.VTTSh;
				if ( bin > maxBin ) {
					maxBin = bin ;
				}
				Long value = histogram.get( bin );
				if ( value == null ) {
					histogram.put( bin, 1L );
				} else {
					value++;
					histogram.put( bin, value );
				}
			}
		}

		double sum = histogram.values().stream().mapToDouble( Long::doubleValue ).sum();

		try( BufferedWriter writer = IOUtils.getBufferedWriter( fileName ) ){
			writer.write( "vtts\tfreq" + System.lineSeparator() );
			for( int ii = 0 ; ii <= maxBin ; ii++ ){
				Long value = histogram.get( ii );
				if( value == null ){
					writer.write( ii + "\t" + "0" + "\t" + "0" + System.lineSeparator() );
				} else{
					writer.write( ii + "\t" + value + "\t" + ( (double) ii*value/sum) + System.lineSeparator() );
				}
			}
		} catch( IOException e ){
			throw new RuntimeException( e );
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
*/

}
