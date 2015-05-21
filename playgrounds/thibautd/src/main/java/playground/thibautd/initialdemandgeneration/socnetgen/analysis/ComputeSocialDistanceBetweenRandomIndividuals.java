/* *********************************************************************** *
 * project: org.matsim.*
 * ComputeSocialDistanceBetweenRandomIndividuals.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.thibautd.initialdemandgeneration.socnetgen.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.util.FastDijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Counter;
import org.matsim.vehicles.Vehicle;

import playground.thibautd.initialdemandgeneration.socnetgen.framework.SnaUtils;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.contrib.socnetsim.framework.population.SocialNetworkReader;

/**
 * @author thibautd
 */
public class ComputeSocialDistanceBetweenRandomIndividuals {
	private static final Logger log =
		Logger.getLogger(ComputeSocialDistanceBetweenRandomIndividuals.class);

	public static void main(final String[] args) {
		final String inputSocialNetwork = args[ 0 ];
		final String outputDataFile = args[ 1 ];

		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new SocialNetworkReader( sc ).parse( inputSocialNetwork );
	
		final SocialNetwork socialNetwork = (SocialNetwork) sc.getScenarioElement( SocialNetwork.ELEMENT_NAME );
		writeRandomDistances( outputDataFile, socialNetwork, 10000 );
	}

	public static void writeRandomDistances(
			final String outputDataFile,
			final SocialNetwork socialNetwork,
			final int nPairs ) {
		final Network matsimNetwork = SocialNetworkAsMatsimNetworkUtils.convertToNetwork( socialNetwork );

		// Fast dijkstra is approx. twice as fast here.
		final LeastCostPathCalculator dijkstra = 
			new FastDijkstraFactory().createPathCalculator(
				matsimNetwork,
				new TravelDisutility() {
					@Override
					public double getLinkTravelDisutility(Link link,
							double time, Person person, Vehicle vehicle) {
						return 1;
					}

					@Override
					public double getLinkMinimumTravelDisutility(Link link) {
						return 1;
					}
				},
				new TravelTime() {
					@Override
					public double getLinkTravelTime(Link link, double time,
							Person person, Vehicle vehicle) {
						return 1;
					}
				});

		log.info( "searching for the biggest connected component..." );
		Set<Id> biggestComponent = null;
		for ( Set<Id> component : SnaUtils.identifyConnectedComponents( socialNetwork ) ) {
			if ( biggestComponent == null || biggestComponent.size() < component.size() ) {
				biggestComponent = component;
			}
		}

		log.info( "considering only biggest component with size "+ biggestComponent.size() );
		log.info( "ignoring "+( socialNetwork.getEgos().size() - biggestComponent.size() )+" agents ("+
				( ( socialNetwork.getEgos().size() - biggestComponent.size() ) * 100d / socialNetwork.getEgos().size() )+"%)" );

		try ( final BufferedWriter writer = IOUtils.getBufferedWriter( outputDataFile ) ) {
			writer.write( "ego\talter\tdistance" );
			final Random random = new Random( 20140121 );
			final List<Node> egos = new ArrayList<Node>( biggestComponent.size() );

			for ( Id id : biggestComponent ) {
				egos.add( matsimNetwork.getNodes().get( id ) );
			}

			final Counter counter = new Counter( "sampling pair # " );
			for ( int i = 0; i < nPairs; i++ ) {
				counter.incCounter();
				final Node ego = egos.get( random.nextInt( egos.size() ) );
				final Node alter = egos.get( random.nextInt( egos.size() ) );

				final Path path = dijkstra.calcLeastCostPath( ego, alter, 0, null, null );

				writer.newLine();
				writer.write( ego.getId() + "\t" + alter.getId() + "\t" + path.travelCost );
			}
			counter.printCounter();
		}
		catch ( IOException e ) {
			throw new UncheckedIOException( e );
		}

	}
}

