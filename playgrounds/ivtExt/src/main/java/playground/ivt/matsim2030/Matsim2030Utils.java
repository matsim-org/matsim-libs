/* *********************************************************************** *
 * project: org.matsim.*
 * Matsim2030Utils.java
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
package playground.ivt.matsim2030;

import java.io.File;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceInitializer;
import org.matsim.contrib.locationchoice.bestresponse.scoring.DCScoringFunctionFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.facilities.algorithms.WorldConnectLocations;
import org.matsim.pt.PtConstants;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetwork;

import herbie.running.controler.listeners.CalcLegTimesHerbieListener;
import herbie.running.controler.listeners.LegDistanceDistributionWriter;

import playground.ivt.kticompatibility.KtiLikeScoringConfigGroup;
import playground.ivt.matsim2030.router.Matsim2030RoutingConfigGroup;
import playground.ivt.matsim2030.router.TransitRouterNetworkReader;
import playground.ivt.matsim2030.router.TransitRouterWithThinnedNetworkFactory;
import playground.ivt.matsim2030.scoring.MATSim2010ScoringFunctionFactory;
import playground.ivt.utils.TripModeShares;

/**
 * @author thibautd
 */
public class Matsim2030Utils {
	
	private static final String CALC_LEG_TIMES_FILE_NAME = "calcLegTimes.txt";
	private static final String LEG_DISTANCE_DISTRIBUTION_FILE_NAME = "multimodalLegDistanceDistribution.txt";
	private static final String LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME = "legTravelTimeDistribution.txt";


	public static Config loadConfig( final String file ) {
		final Config config = ConfigUtils.createConfig();
		addDefaultGroups( config );
		ConfigUtils.loadConfig( config , file );
		return config;
	}

	public static void addDefaultGroups( final Config config ) {
		config.addModule( new Matsim2030RoutingConfigGroup() );
		config.addModule( new KtiLikeScoringConfigGroup() );
	}

	public static void connectFacilitiesWithLinks( final Scenario sc ) {
		new WorldConnectLocations( sc.getConfig() ).connectFacilitiesWithLinks(
				sc.getActivityFacilities(),
				(NetworkImpl) sc.getNetwork() );
	}

	public static void initializeLocationChoice( final Controler controler ) {
		final Scenario scenario = controler.getScenario();
		final DestinationChoiceBestResponseContext lcContext =
			new DestinationChoiceBestResponseContext( scenario );
		lcContext.init();

		// XXX this thing is awful. I think one can (and should) avoid using it...
		// There does not seem to be a reason not to do all the notify startup
		// method does before calling run().
		controler.addControlerListener(
				new DestinationChoiceInitializer(
					lcContext));
	}

	public static void initializeScoring( final Controler controler ) {
 		final MATSim2010ScoringFunctionFactory scoringFunctionFactory =
			new MATSim2010ScoringFunctionFactory(
					controler.getScenario(),
					new StageActivityTypesImpl( PtConstants.TRANSIT_ACTIVITY_TYPE ) ); 	
		controler.setScoringFunctionFactory( scoringFunctionFactory );
	}

	public static TripRouterFactory createTripRouterFactory( final Scenario scenario ) {
		final TransitRouterConfig conf = new TransitRouterConfig( scenario.getConfig() );

		final Matsim2030RoutingConfigGroup matsim2030conf =
			(Matsim2030RoutingConfigGroup)
			scenario.getConfig().getModule(
					Matsim2030RoutingConfigGroup.GROUP_NAME );
		final TransitRouterNetwork transitRouterNetwork = new TransitRouterNetwork();
		new TransitRouterNetworkReader(
				scenario,
				scenario.getTransitSchedule(),
				transitRouterNetwork ).parse(
					matsim2030conf.getThinnedTransitRouterNetworkFile() );

		final TransitRouterWithThinnedNetworkFactory transitRouterFactory =
			new TransitRouterWithThinnedNetworkFactory(
					scenario.getTransitSchedule(),
					conf,
					transitRouterNetwork );

		final TripRouterFactoryBuilderWithDefaults builder = new TripRouterFactoryBuilderWithDefaults();
		builder.setTransitRouterFactory( transitRouterFactory );
		return builder.build( scenario );
	}

	public static void loadControlerListeners( final Controler controler ) {
		controler.addControlerListener(
				new CalcLegTimesHerbieListener(
					CALC_LEG_TIMES_FILE_NAME,
					LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME));
		controler.addControlerListener(
				new LegDistanceDistributionWriter(
					LEG_DISTANCE_DISTRIBUTION_FILE_NAME,
					controler.getScenario().getNetwork()));
		final TripRouter router = controler.getTripRouterFactory().instantiateAndConfigureTripRouter();
		controler.addControlerListener(
				new TripModeShares(
					25, // write interval. TODO: pass by config
					controler.getControlerIO(),
					controler.getScenario(),
					router.getMainModeIdentifier(),
					router.getStageActivityTypes() ) );
	}

	public static void createEmptyDirectoryOrFailIfExists(final String directory) {
		final File file = new File( directory +"/" );
		if ( file.exists() && file.list().length > 0 ) {
			throw new UncheckedIOException( "Directory "+directory+" exists and is not empty!" );
		}
		file.mkdirs();
	}
}

