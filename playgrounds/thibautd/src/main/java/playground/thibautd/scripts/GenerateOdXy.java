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
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.StreamingUtils;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import playground.ivt.utils.MoreIOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;

/**
 * @author thibautd
 */
public class GenerateOdXy {
	public static void main( String[] args ) {
		final String inputPopulation = args[ 0 ];
		final String outputXy = args[ 1 ];
		final StageActivityTypes stages =
				args.length == 2 ? EmptyStageActivityTypes.INSTANCE :
						new StageActivityTypesImpl(
								Arrays.copyOfRange(
										args,
										2,
										args.length ) );

		MoreIOUtils.checkFile( outputXy );

		try ( final BufferedWriter writer = IOUtils.getBufferedWriter( outputXy ) ) {
			// TODO mode (with mode identifier)
			writer.write( "P_ID\tO_TYPE\tO_X\tO_Y\tD_TYPE\tD_X\tD_Y" );

			final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );

			StreamingUtils.setIsStreaming((( Population) sc.getPopulation()), true);
			StreamingUtils.addAlgorithm((( Population) sc.getPopulation()), p -> {
				try {
					final Plan plan = p.getSelectedPlan();
			
					for ( TripStructureUtils.Trip trip : TripStructureUtils.getTrips( plan, stages ) ) {
						writer.newLine();
						writer.write( p.getId() + "\t" );
			
						writer.write( trip.getOriginActivity().getType() + "\t" );
						writer.write( trip.getOriginActivity().getCoord().getX() + "\t" );
						writer.write( trip.getOriginActivity().getCoord().getY() + "\t" );
			
						writer.write( trip.getDestinationActivity().getType() + "\t" );
						writer.write( trip.getDestinationActivity().getCoord().getX() + "\t" );
						writer.write( trip.getDestinationActivity().getCoord().getY()+"" );
					}
				}
				catch ( IOException e ) {
					throw new UncheckedIOException( e );
				}
			});

			new PopulationReader( sc ).readFile( inputPopulation );
		}
		catch ( IOException e ) {
			throw new UncheckedIOException( e );
		}
	}
}
