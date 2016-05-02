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
package playground.thibautd.scripts;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author thibautd
 */
public class ExtractTravelTimesForModes {
	public static void main( final String... args ) {
		final String inputPopulation = args[ 0 ];
		final String outputDatFile = args[ 1 ];

		final Scenario scenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new MatsimPopulationReader( scenario ).readFile( inputPopulation );

		final Map<String,Collection<TripInfo>> tripInfos = new LinkedHashMap<>();

		for ( Person p : scenario.getPopulation().getPersons().values() ) {
			final Plan plan = p.getSelectedPlan();
			for ( TripStructureUtils.Trip t : TripStructureUtils.getTrips( plan , EmptyStageActivityTypes.INSTANCE ) ) {
				assert t.getTripElements().size() == 1;
				final Leg l = t.getLegsOnly().get( 0 );

				final TripInfo info = new TripInfo();
				info.tt = l.getTravelTime();
				info.dist =
						CoordUtils.calcEuclideanDistance(
								t.getOriginActivity().getCoord(),
								t.getDestinationActivity().getCoord() );

				MapUtils.getCollection( l.getMode() , tripInfos ).add( info );
			}
		}

		try ( final BufferedWriter writer = IOUtils.getBufferedWriter( outputDatFile ) ) {
			boolean first = true;
			for ( String mode : tripInfos.keySet() ) {
				if (!first) writer.write( "\t" );
				writer.write( mode+"_tt\t"+mode+"_crowdist\t"+mode+"_crowspeed" );
				first = false;
			}

			final Collection<Iterator<TripInfo>> iterators = new ArrayList<>();
			for ( Collection<TripInfo> coll : tripInfos.values() ) {
				iterators.add( coll.iterator() );
			}

			boolean writeNextRow = true;
			while ( writeNextRow ) {
				writer.newLine();
				first = true;
				writeNextRow = false;
				for ( Iterator<TripInfo> iterator : iterators ) {
					if (!first) writer.write( "\t" );
					if ( iterator.hasNext() ) {
						final TripInfo info = iterator.next();
						writer.write( info.tt+"\t"+info.dist+"\t"+( info.tt < 1E-9 ? 0 : info.dist / info.tt ) );
						if ( iterator.hasNext() ) writeNextRow = true;
					}
					else {
						writer.write( "NA\tNA\tNA" );
					}
					first = false;
				}
			}
		}
		catch ( IOException e ) {
			throw new RuntimeException( e );
		}
	}

	private static class TripInfo {
		double tt, dist;
	}
}

