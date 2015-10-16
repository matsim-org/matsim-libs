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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.scoring.EventsToActivities.ActivityHandler;
import org.matsim.core.scoring.EventsToLegs.LegHandler;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author thibautd
 */
public class TripStatisticsCollectingEventHandler implements LegHandler, ActivityHandler, BeforeMobsimListener, AfterMobsimListener {
	private boolean collectStatistics = true;

	private final OutputDirectoryHierarchy io;
	private BufferedWriter writer = null;

	private final StageActivityTypes stages;
	private final MainModeIdentifier modeIdentifier;

	private final Map<Id<Person>, List<PlanElement>> currentTrips = new HashMap<>();

	public TripStatisticsCollectingEventHandler(OutputDirectoryHierarchy io, StageActivityTypes stages, MainModeIdentifier modeIdentifier) {
		this.io = io;
		this.stages = stages;
		this.modeIdentifier = modeIdentifier;
	}

	@Override
	public void handleActivity(Id<Person> agentId, Activity activity) {
		if ( !collectStatistics ) return;
		if ( stages.isStageActivity( activity.getType() ) ) {
			MapUtils.getList( agentId , currentTrips ).add( activity );
			return;
		}

		final List<PlanElement> trip = currentTrips.remove( agentId );
		if ( trip == null ) return;

		final String mode = modeIdentifier.identifyMainMode( trip );

	}

	@Override
	public void handleLeg(Id<Person> agentId, Leg leg) {
		if ( !collectStatistics ) return;
		MapUtils.getList( agentId , currentTrips ).add( leg );
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		try {
			writer.close();
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
}
