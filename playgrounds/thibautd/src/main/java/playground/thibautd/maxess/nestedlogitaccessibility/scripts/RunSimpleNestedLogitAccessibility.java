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
package playground.thibautd.maxess.nestedlogitaccessibility.scripts;

import gnu.trove.map.TObjectDoubleMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.algorithms.WorldConnectLocations;
import org.matsim.population.algorithms.XY2Links;
import playground.thibautd.maxess.nestedlogitaccessibility.framework.NestedLogitAccessibilityCalculator;
import playground.thibautd.maxess.nestedlogitaccessibility.framework.NestedLogitModel;
import playground.thibautd.maxess.nestedlogitaccessibility.writers.BasicPersonAccessibilityWriter;
import playground.thibautd.maxess.prepareforbiogeme.tripbased.RunMzTripChoiceSetConversion;
import playground.thibautd.router.TripSoftCache;
import playground.thibautd.utils.MoreIOUtils;

import java.util.Collections;

/**
 * @author thibautd
 */
public class RunSimpleNestedLogitAccessibility {
	public static void main( final String... args ) {
		final String configFile = args[ 0 ];
		final String outputDir = args[ 1 ];

		MoreIOUtils.initOut( outputDir );

		try {
			final Config config = ConfigUtils.loadConfig( configFile );
			final Scenario scenario = ScenarioUtils.loadScenario( config );

			final TransportModeNetworkFilter filter = new TransportModeNetworkFilter(scenario.getNetwork());
			final Network carNetwork = NetworkUtils.createNetwork();
			filter.filter(carNetwork, Collections.singleton( "car" ));
			new WorldConnectLocations( config ).connectFacilitiesWithLinks(scenario.getActivityFacilities(), (NetworkImpl) carNetwork);

			new XY2Links( carNetwork , scenario.getActivityFacilities() ).run( scenario.getPopulation() );

			final NestedLogitAccessibilityCalculator<ModeNests> calculator =
					new NestedLogitAccessibilityCalculator<>(
							scenario.getPopulation(),
							scenario.getActivityFacilities(),
							new NestedLogitModel<>(
									new SimpleNestedLogitModelUtility(
											scenario.getPopulation().getPersonAttributes() ),
									new SimpleNestedLogitModelChoiceSetIdentifier(
											"leisure",
											200,
											RunMzTripChoiceSetConversion.createTripRouter(
												scenario,
												new TripSoftCache(
														false,
														TripSoftCache.LocationType.link) ),
											scenario.getActivityFacilities(),
											20 * 1000 ) ) );

			// TODO store and write results
			final TObjectDoubleMap<Id<Person>> accessibilities = calculator.computeAccessibilities ();
			new BasicPersonAccessibilityWriter( scenario , accessibilities ).write( outputDir + "/accessibility_per_person.dat" );
		}
		finally {
			MoreIOUtils.closeOutputDirLogging();
		}
	}
}
