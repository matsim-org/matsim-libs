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
package playground.ivt.analysis.tripstats;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scoring.EventsToActivities;
import org.matsim.core.scoring.EventsToActivities.ActivityHandler;
import org.matsim.core.scoring.EventsToLegs.LegHandler;
import org.matsim.core.scoring.PersonExperiencedActivity;
import org.matsim.core.scoring.PersonExperiencedLeg;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.facilities.ActivityFacility;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author thibautd
 */
@Singleton
public class TripStatisticsCollectingEventHandler implements LegHandler, ActivityHandler, BeforeMobsimListener, AfterMobsimListener {
	private static final Logger log = Logger.getLogger(TripStatisticsCollectingEventHandler.class);
	private boolean collectStatistics = true;

	private final OutputDirectoryHierarchy io;
	private BufferedWriter writer = null;

	private final StageActivityTypes stages;
	private final MainModeIdentifier modeIdentifier;

	private final Map<Id<Person>, Record> currentTrips = new HashMap<>();

	private final Set<Id<Person>> personsToTrack;
	private final Scenario scenario;
	private EventsToActivities e2a;

	@Inject
	public TripStatisticsCollectingEventHandler(
			OutputDirectoryHierarchy io,
			TripRouter router,
			Scenario scenario) {
		this(
				io,
				router.getStageActivityTypes(),
				router.getMainModeIdentifier(),
				scenario.getPopulation().getPersons().keySet(),
				scenario);
	}

	public TripStatisticsCollectingEventHandler(
			OutputDirectoryHierarchy io,
			StageActivityTypes stages,
			MainModeIdentifier modeIdentifier,
			Set<Id<Person>> personsToTrack,
			Scenario scenario) {
		this.io = io;
		this.stages = stages;
		this.modeIdentifier = modeIdentifier;
		this.personsToTrack = personsToTrack;
		this.scenario = scenario;
	}

	@Override
	public void handleActivity(PersonExperiencedActivity personExperiencedActivity) {
		if ( !collectStatistics || !personsToTrack.contains(personExperiencedActivity.getAgentId()) ) return;

		if ( log.isTraceEnabled() ) {
			log.trace( "handle activity "+ personExperiencedActivity.getActivity() +" for agent "+ personExperiencedActivity.getAgentId());
		}

		if ( stages.isStageActivity(personExperiencedActivity.getActivity().getType() ) ) {
			currentTrips.get(personExperiencedActivity.getAgentId()).trip.add(personExperiencedActivity.getActivity());
			return;
		}

		final Record record = currentTrips.put(personExperiencedActivity.getAgentId(), new Record(personExperiencedActivity.getActivity()) );
		if ( record == null )  return;
		record.destination = personExperiencedActivity.getActivity();

		try {
			writer.newLine();
			writer.write(personExperiencedActivity.getAgentId().toString());
			writer.write( "\t" );
			writer.write( ""+record.origin.getEndTime() );
			writer.write( "\t" );
			writer.write( modeIdentifier.identifyMainMode( record.trip ) );
			writer.write( "\t" );
			writer.write( record.origin.getType() );
			writer.write( "\t" );
			writer.write("" + getCoord( record.origin  ).getX());
			writer.write("\t");
			writer.write("" + getCoord( record.origin  ).getY());
			writer.write("\t");
			writer.write( ""+record.destination.getType() );
			writer.write("\t");
			writer.write("" + getCoord(record.destination).getX());
			writer.write("\t");
			writer.write("" + getCoord(record.destination).getY());
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
		catch (RuntimeException e) {
			log.error( "got exception handling "+ personExperiencedActivity.getActivity() +" for agent "+ personExperiencedActivity.getAgentId(), e );
			throw e;
		}
	}

	/**
	 * gets the coordinate of an activity, infering it from facilities or link if it is not defined.
	 *
	 */
	private Coord getCoord( final Activity act ) {
		if ( act.getCoord() != null ) return act.getCoord();

		final Id<ActivityFacility> facilityId = act.getFacilityId();
		if ( facilityId != null ) return scenario.getActivityFacilities().getFacilities().get( facilityId ).getCoord();

		// Where should the activity be? start, end, middle?
		return scenario.getNetwork().getLinks().get( act.getLinkId() ).getCoord();
	}

	@Override
	public void handleLeg(PersonExperiencedLeg personExperiencedLeg) {
		if ( !collectStatistics || !personsToTrack.contains(personExperiencedLeg.getAgentId()) ) return;
		if ( log.isTraceEnabled() ) {
			log.trace( "handle leg "+ personExperiencedLeg.getLeg() +" for agent "+ personExperiencedLeg.getAgentId());
		}
		currentTrips.get(personExperiencedLeg.getAgentId()).trip.add(personExperiencedLeg.getLeg());
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		try {
			// This is dirty, but has to be done somehow
			e2a.finish();
			writer.close();
			currentTrips.clear();
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
		writer = null;
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		writer = IOUtils.getBufferedWriter( io.getIterationFilename( event.getIteration() , "trip-stats.dat" ) );
		try {
			writer.write( "personId" );
			writer.write( "\t" );
			writer.write( "departureTime" );
			writer.write( "\t" );
			writer.write( "mode" );
			writer.write( "\t" );
			writer.write( "origType" );
			writer.write( "\t" );
			writer.write( "xOrig" );
			writer.write( "\t" );
			writer.write( "yOrig" );
			writer.write( "\t" );
			writer.write( "destType" );
			writer.write( "\t" );
			writer.write( "xDest" );
			writer.write( "\t" );
			writer.write( "yDest" );
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}

	/**
	 * used to call "finish"
	 */
	public void setEventsToActivities(EventsToActivities e2a) {
		this.e2a = e2a;
	}

	private static class Record {
		Activity origin, destination;
		final List<PlanElement> trip = new ArrayList<>();

		Record( final Activity origin )  {
			this.origin = origin;
		}
	}
}
