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

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.algorithms.WorldConnectLocations;
import org.matsim.population.algorithms.XY2Links;
import playground.thibautd.maxess.prepareforbiogeme.framework.Converter;

import java.io.File;

/**
 * @author thibautd
 */
public class PrismicTripChoiceSetConversion {
	public static void main(final String[] args) {
		final PrismicConversionConfigGroup group = new PrismicConversionConfigGroup();
		final Config config = ConfigUtils.loadConfig(args[0], group);

		if ( new File( group.getOutputPath() ).exists() ) throw new RuntimeException( group.getOutputPath()+" exists" );
		IOUtils.createDirectory( group.getOutputPath() );

		final Scenario sc = ScenarioUtils.loadScenario( config );
		new WorldConnectLocations( config ).connectFacilitiesWithLinks( sc.getActivityFacilities() , (NetworkImpl) sc.getNetwork() );
		new XY2Links( sc ).run( sc.getPopulation() );

		final TripRouter tripRouter =
				new TripRouterFactoryBuilderWithDefaults().build( sc ).instantiateAndConfigureTripRouter(
						new RoutingContext() {
							final FreespeedTravelTimeAndDisutility tt = new FreespeedTravelTimeAndDisutility( config.planCalcScore() );
							@Override
							public TravelDisutility getTravelDisutility() {
								return tt;
							}

							@Override
							public TravelTime getTravelTime() {
								return tt;
							}
						}
				);

		Converter.<Trip,TripChoiceSituation>builder()
				.withRecordFiller(
						new BasicTripChoiceSetRecordFiller())
				.withChoiceSetSampler(
						new RoutingChoiceSetSampler(
								tripRouter,
								group.getModes(),
								new PrismicDestinationSampler(
										group.getActivityType(),
										sc.getActivityFacilities(),
										group.getChoiceSetSize(),
										group.getBudget_m() ) ) )
				.withChoicesIdentifier(
						new TripChoicesIdentifier(
								sc.getActivityFacilities(),
								tripRouter.getStageActivityTypes() ) )
				.create()
				.convert(
						sc.getPopulation(),
						group.getOutputPath()+"data.dat" );
	}
}
