/* *********************************************************************** *
 * project: org.matsim.*
 * ExtractTripModeSharesLocatedInArea.java
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
package playground.thibautd.scripts;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.router.CompositeStageActivityTypes;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.pt.PtConstants;

import playground.thibautd.socnetsim.population.JointActingTypes;

/**
 * @author thibautd
 */
public class ExtractTripModeSharesLocatedInArea {
	private static final String SEP = ";";

	private static final StageActivityTypes STAGES;
	static {
		STAGES = new CompositeStageActivityTypes();
		((CompositeStageActivityTypes) STAGES).addActivityTypes( new StageActivityTypesImpl( PtConstants.TRANSIT_ACTIVITY_TYPE ) );
		((CompositeStageActivityTypes) STAGES).addActivityTypes( JointActingTypes.JOINT_STAGE_ACTS );
	}

	private static final MainModeIdentifier MODE_IDENTIFIER =
			new MainModeIdentifier() {
				final MainModeIdentifier identifier = new MainModeIdentifierImpl();
				@Override
				public String identifyMainMode(
						final List<PlanElement> tripElements) {
					for ( PlanElement pe : tripElements ) {
						if ( pe instanceof Leg && JointActingTypes.JOINT_MODES.contains( ((Leg) pe).getMode() ) ) {
							return ((Leg) pe).getMode();
						}
					}
					return identifier.identifyMainMode( tripElements );
				}
			};

	public static void main(final String[] args) throws IOException {
		final String inPopFile = args[ 0 ];
		final String outRawData = args[ 1 ];

		final Collection<TripInfo> tripInfos = parseTripInfos( inPopFile );

		final Set<String> modes = new HashSet<String>();
		for ( TripInfo info : tripInfos ) modes.add( info.mode );

		final BufferedWriter writer = IOUtils.getBufferedWriter( outRawData );
		writeRawData( writer , tripInfos , outRawData );
		writer.close();
	}

	private static void writeRawData(
			final BufferedWriter writer,
			final Collection<TripInfo> tripInfos,
			final String outFile ) throws IOException {
		final Counter counter = new Counter( outFile+": line # " );
		writer.write( "mode"+SEP+"origType"+SEP+"destType"+SEP+"xOrig"+SEP+"yOrig"+SEP+"xDest"+SEP+"yDest" );
		for ( TripInfo info : tripInfos ) {
			counter.incCounter();
			writer.newLine();
			writer.write( info.mode + SEP +
					info.origType + SEP + info.destType +SEP+
					info.origin.getX() + SEP + info.origin.getY() +SEP+
					info.destination.getX() + SEP + info.destination.getY() );
		}
		counter.printCounter();
	}

	private static Collection<TripInfo> parseTripInfos(final String inPopFile) {
		final Collection<TripInfo> infos = new ArrayList<TripInfo>();

		final Scenario scenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		((PopulationImpl) scenario.getPopulation()).setIsStreaming( true );
		((PopulationImpl) scenario.getPopulation()).addAlgorithm(
				new PersonAlgorithm() {
					@Override
					public void run(final Person person) {
						for ( Trip trip : TripStructureUtils.getTrips( person.getSelectedPlan() , STAGES ) ) {
							infos.add(
								new TripInfo(
									MODE_IDENTIFIER.identifyMainMode( trip.getTripElements() ),
									trip.getOriginActivity().getCoord(),
									trip.getDestinationActivity().getCoord(),
									trip.getOriginActivity().getType(),
									trip.getDestinationActivity().getType()) );
						}
					}
				});

		new MatsimPopulationReader( scenario ).parse( inPopFile );
		return infos;
	}

	private static class TripInfo {
		public final String mode;
		public final Coord origin, destination;
		public final String origType, destType;

		public TripInfo(
				final String mode,
				final Coord origin,
				final Coord destination,
				final String origType,
				final String destType) {
			this.mode = mode;
			this.origin = origin;
			this.destination = destination;
			this.origType = origType;
			this.destType = destType;
		}
	}
}

