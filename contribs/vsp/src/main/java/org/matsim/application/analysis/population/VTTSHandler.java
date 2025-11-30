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
public final class VTTSHandler implements ActivityStartEventHandler, ActivityEndEventHandler, PersonDepartureEventHandler, TransitDriverStartsEventHandler {
	// the constructor is package-private so having the class public final is ok. kai, nov'25

	public static class TripData {
		public double mUTTSh;
		public String actType;
		public double actTypDur_h;
		public double actDur_h;
		double VTTSh;
		double musl_h;
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

	private static final Logger log = LogManager.getLogger( VTTSHandler.class );
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


	@Inject VTTSHandler( Scenario scenario, ScoringParametersForPerson scoringParametersForPerson ) {
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
		if ( this.personIdsToBeIgnored.contains( personId )){
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
		DoubleColumn muslValues = DoubleColumn.create( HeadersKN.muslh );
		DoubleColumn muttsValues = DoubleColumn.create( HeadersKN.muttsh );
		DoubleColumn vttsValues = DoubleColumn.create( HeadersKN.vttsh );
		DoubleColumn mUoMs = DoubleColumn.create( HeadersKN.mUoM );
		Table table = Table.create( personIds, mUoMs, tripIndices, modes, acts, actDurs, typDurs, muslValues, muttsValues, vttsValues );

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
				muslValues.append( trip.musl_h );
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

			final MarginalSumScoringFunction marginalSumScoringFunction = new MarginalSumScoringFunction(
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

		simData.trips.getLast().musl_h = activityDelayDisutility_h;

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


}
