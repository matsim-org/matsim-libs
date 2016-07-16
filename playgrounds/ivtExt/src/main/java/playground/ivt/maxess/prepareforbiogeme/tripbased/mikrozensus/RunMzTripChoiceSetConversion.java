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
package playground.ivt.maxess.prepareforbiogeme.tripbased.mikrozensus;

import com.google.inject.Provider;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.algorithms.XY2Links;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.algorithms.WorldConnectLocations;
import org.matsim.pt.PtConstants;
import org.matsim.pt.router.MultiNodeDijkstra;
import playground.ivt.maxess.prepareforbiogeme.framework.ChoiceSetSampler;
import playground.ivt.maxess.prepareforbiogeme.framework.Converter;
import playground.ivt.maxess.prepareforbiogeme.tripbased.PrismicConversionConfigGroup;
import playground.ivt.maxess.prepareforbiogeme.tripbased.PrismicDestinationSampler;
import playground.ivt.maxess.prepareforbiogeme.tripbased.RoutingChoiceSetSampler;
import playground.ivt.maxess.prepareforbiogeme.tripbased.Trip;
import playground.ivt.maxess.prepareforbiogeme.tripbased.TripChoiceSituation;
import playground.ivt.maxess.prepareforbiogeme.tripbased.TripChoicesIdentifier;
import playground.ivt.router.CachingRoutingModuleWrapper;
import playground.ivt.router.TripSoftCache;
import playground.ivt.utils.MoreIOUtils;

import java.io.File;
import java.util.Collections;

/**
 * @author thibautd
 */
public class RunMzTripChoiceSetConversion {
	public static void main( final String... args ) {
		final PrismicConversionConfigGroup group = new PrismicConversionConfigGroup();
		final Config config = ConfigUtils.loadConfig( args[ 0 ], group );
		// random generators are obtained from MatsimRandom.getLocalInstance().
		// Allow varying the seed from the config file
		MatsimRandom.reset( config.global().getRandomSeed() );

		Logger.getLogger( MultiNodeDijkstra.class ).setLevel( Level.ERROR );

		if ( new File( group.getOutputPath() ).exists() ) throw new RuntimeException( group.getOutputPath()+" exists" );
		MoreIOUtils.initOut( group.getOutputPath() , config );

		final Scenario sc = ScenarioUtils.loadScenario( config );

		final TransportModeNetworkFilter filter = new TransportModeNetworkFilter(sc.getNetwork());
		final Network carNetwork = NetworkUtils.createNetwork();
		filter.filter(carNetwork, Collections.singleton( "car" ));
		new WorldConnectLocations( config ).connectFacilitiesWithLinks(sc.getActivityFacilities(), (Network) carNetwork);

		new XY2Links( carNetwork , sc.getActivityFacilities() ).run(sc.getPopulation());

		//Logger.getLogger(SoftCache.class).setLevel(Level.TRACE );
		try {
			final MZ2010ExportChoiceSetRecordFiller filler = new MZ2010ExportChoiceSetRecordFiller( sc.getPopulation().getPersonAttributes() );

			Converter.<Trip, TripChoiceSituation>builder()
					.withRecordFiller(
							filler )
					.withChoiceSetSampler(
							new Provider<ChoiceSetSampler<Trip, TripChoiceSituation>>() {
								// only one global route cache: less memory consumpion, more chances of a hit
								final TripSoftCache cache = new TripSoftCache(false, TripSoftCache.LocationType.link);
								@Override
								public ChoiceSetSampler<Trip, TripChoiceSituation> get() {
									return new RoutingChoiceSetSampler(
											createTripRouter( sc , cache ),
											group.getModes(),
											new PrismicDestinationSampler(
													group.getActivityType(),
													sc.getActivityFacilities(),
													group.getChoiceSetSize(),
													group.getBudget_m()));
								}
							})
					.withChoicesIdentifier(
							() -> new TripChoicesIdentifier(
									group.getActivityType(),
									sc.getActivityFacilities(),
									new StageActivityTypesImpl(
											PtConstants.TRANSIT_ACTIVITY_TYPE),
									new MainModeIdentifierImpl(),
									group.getModes() ) )
					.withNumberOfThreads(
							group.getNumberOfThreads())
					.create()
					.convert(
							sc.getPopulation(),
							group.getOutputPath() + "/data.dat");

			CodebookUtils.writeCodebook( group.getOutputPath() + "/codebook.md", filler.getCodebook() );
		}
		finally {
			MoreIOUtils.closeOutputDirLogging();
		}
	}

	public static TripRouter createTripRouter(
			final Scenario sc,
			final TripSoftCache cache ) {
		final FreespeedTravelTimeAndDisutility tt = new FreespeedTravelTimeAndDisutility(sc.getConfig().planCalcScore());

		final TripRouterFactoryBuilderWithDefaults b = new TripRouterFactoryBuilderWithDefaults();
		b.setTransitRouterFactory( TripRouterFactoryBuilderWithDefaults.createDefaultTransitRouter( sc ) );
		b.setTravelTime( tt );
		b.setTravelDisutility( tt );
		final TripRouter tripRouter = b.build(sc).get();

		tripRouter.setRoutingModule(
				TransportMode.car,
				new CachingRoutingModuleWrapper(
						cache,
						tripRouter.getRoutingModule(
								TransportMode.car)));

		return tripRouter;
	}
}
