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
package playground.thibautd.maxess.prepareforbiogeme.tripbased;

import com.google.inject.Provider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.facilities.algorithms.WorldConnectLocations;
import org.matsim.population.algorithms.XY2Links;
import org.matsim.pt.PtConstants;
import playground.thibautd.maxess.prepareforbiogeme.framework.ChoiceSetSampler;
import playground.thibautd.maxess.prepareforbiogeme.framework.ChoicesIdentifier;
import playground.thibautd.maxess.prepareforbiogeme.framework.Converter;
import playground.thibautd.router.CachingRoutingModuleWrapper;
import playground.thibautd.router.TripSoftCache;
import playground.thibautd.utils.MoreIOUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;

/**
 * @author thibautd
 */
public class RunMzTripChoiceSetConversion {
	public static void main( final String... args ) {
		final PrismicConversionConfigGroup group = new PrismicConversionConfigGroup();
		final Config config = ConfigUtils.loadConfig( args[ 0 ], group );

		if ( new File( group.getOutputPath() ).exists() ) throw new RuntimeException( group.getOutputPath()+" exists" );
		MoreIOUtils.initOut( group.getOutputPath() );

		final Scenario sc = ScenarioUtils.loadScenario( config );

		final TransportModeNetworkFilter filter = new TransportModeNetworkFilter(sc.getNetwork());
		final Network carNetwork = NetworkUtils.createNetwork();
		filter.filter(carNetwork, Collections.singleton( "car" ));
		new WorldConnectLocations( config ).connectFacilitiesWithLinks(sc.getActivityFacilities(), (NetworkImpl) carNetwork);

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
									final FreespeedTravelTimeAndDisutility tt = new FreespeedTravelTimeAndDisutility(config.planCalcScore());

									final TripRouterFactoryBuilderWithDefaults b = new TripRouterFactoryBuilderWithDefaults();
									b.setTravelTime( tt );
									b.setTravelDisutility( tt );
									final TripRouter tripRouter = b.build(sc).get();

									tripRouter.setRoutingModule(
											TransportMode.car,
											new CachingRoutingModuleWrapper(
													cache,
													tripRouter.getRoutingModule(
															TransportMode.car)));

									return new RoutingChoiceSetSampler(
											tripRouter,
											group.getModes(),
											new PrismicDestinationSampler(
													group.getActivityType(),
													sc.getActivityFacilities(),
													group.getChoiceSetSize(),
													group.getBudget_m()));
								}
							})
					.withChoicesIdentifier(
							new Provider<ChoicesIdentifier<TripChoiceSituation>>() {
								@Override
								public ChoicesIdentifier<TripChoiceSituation> get() {
									return new TripChoicesIdentifier(
											group.getActivityType(),
											sc.getActivityFacilities(),
											new StageActivityTypesImpl(
													PtConstants.TRANSIT_ACTIVITY_TYPE));
								}
							})
					.withNumberOfThreads(
							group.getNumberOfThreads())
					.create()
					.convert(
							sc.getPopulation(),
							group.getOutputPath() + "/data.dat");

			try ( final BufferedWriter writer = IOUtils.getBufferedWriter( group.getOutputPath() +"/codebook.md"  ) ) {
				MoreIOUtils.writeLines(
						writer,
						"Information",
						"===========",
						"This is metadata to dataset generated with:",
						"",
						"`"+RunMzTripChoiceSetConversion.class.getName()+"`",
						"",
						"Date: " + DateFormat.getDateInstance(
								DateFormat.FULL ).format(
										new Date() ),
						"",
						"",
						"Conversion to pdf: use [pandoc](http://pandoc.org/README.html)",
						"",
						"Command: `pandoc Codebook.md -o Codebook.pdf`",
						"",
						"Codebook",
						"========"
						);

				for ( MZ2010ExportChoiceSetRecordFiller.Codepage page : filler.getCodebook().getPages().values() ) {
					writer.write( page.getVariableName() );
					writer.newLine();

					for ( int i=0; i < page.getVariableName().length(); i++ ) writer.write( "-" );
					writer.newLine();
					writer.newLine();

					// Probably not supernice, one should adapt column width to some extent.
					// on the other side, one can simply use pandoc to get a PDF version
					writer.write( "| Code | Meaning | Count |" );
					writer.newLine();
					writer.write( "|------|---------|-------|" );
					writer.newLine();
					for ( Number code : page.getCodingToCount().keySet() ) {
						writer.write( "| "+code );
						writer.write( " | "+page.getCodingToMeaning().get( code ) );
						writer.write( " | "+page.getCodingToCount().get( code )+" |" );
						writer.newLine();
					}
					writer.newLine();
				}
			}
			catch ( IOException e ) {
				throw new UncheckedIOException( e );
			}
		}
		finally {
			MoreIOUtils.closeOutputDirLogging();
		}
	}
}
