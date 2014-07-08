/* *********************************************************************** *
 * project: org.matsim.*
 * RunZurichBikeSharingSimulation.java
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
package playground.thibautd.eunoia.run;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.experimental.ReflectiveModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.OutputDirectoryLogging;

import playground.ivt.matsim2030.Matsim2030Utils;
import playground.thibautd.eunoia.scoring.Matsim2010BikeSharingScoringFunctionFactory;
import eu.eunoiaproject.bikesharing.framework.BikeSharingConstants;
import eu.eunoiaproject.bikesharing.framework.qsim.BikeSharingWithoutRelocationQsimFactory;
import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingScenarioUtils;
import eu.eunoiaproject.bikesharing.scoring.StepBasedFareConfigGroup;
import eu.eunoiaproject.elevation.scoring.SimpleElevationScorerParameters;

import playground.thibautd.router.CachingRoutingModuleWrapper;
import playground.thibautd.router.TripLruCache;

/**
 * @author thibautd
 */
public class RunZurichBikeSharingSimulation {
	public static void main(final String[] args) {
		final String configFile = args[ 0 ];

		OutputDirectoryLogging.catchLogEntries();
		Logger.getLogger( TripLruCache.class ).setLevel( Level.INFO );

		final Config config = BikeSharingScenarioUtils.loadConfig( configFile );
		Matsim2030Utils.addDefaultGroups( config );
		config.addModule( new StepBasedFareConfigGroup() );
		final DenivelationScoringConfigGroup denivelationScoringGroup = new DenivelationScoringConfigGroup();
		config.addModule( denivelationScoringGroup );
		config.addModule( new MultiModalConfigGroup() );

		final RelocationConfigGroup relocationGroup = new RelocationConfigGroup();
		config.addModule( relocationGroup );

		final Scenario sc = Matsim2030Utils.loadScenario( config );
		BikeSharingScenarioUtils.loadBikeSharingPart( sc );

		final Controler controler = new Controler( sc );

		controler.addControlerListener(
				new ShutdownListener() {
					@Override
					public void notifyShutdown(final ShutdownEvent event) {
						CachingRoutingModuleWrapper.logStats();
					}
				});

		// Not sure how well (or bad) this would interact with Bike Sharing...
		// I expect pretty nasty stuff to silently happen.
		// Matsim2030Utils.initializeLocationChoice( controler );

		controler.setTripRouterFactory(
				BikeSharingScenarioUtils.createTripRouterFactoryAndConfigureRouteFactories(
					controler.getTravelDisutilityFactory(),
					controler.getScenario() ) );

		switch ( relocationGroup.getStrategy() ) {
		case noRelocation:
			controler.setMobsimFactory( new BikeSharingWithoutRelocationQsimFactory( false ) );
			break;
		case systemWideCapacities:
			controler.setMobsimFactory( new BikeSharingWithoutRelocationQsimFactory( true ) );
			break;
		default:
			throw new RuntimeException();
		}

		controler.setScoringFunctionFactory(
				new Matsim2010BikeSharingScoringFunctionFactory(
					sc,
					denivelationScoringGroup.getParameters() ) );

		controler.run();
	}

	private static class RelocationConfigGroup extends ReflectiveModule {
		public static final String GROUP_NAME = "bikeSharingRedistribution";

		public static enum Strategy {
			noRelocation, systemWideCapacities;
		}
		private Strategy strategy = Strategy.systemWideCapacities;

		public RelocationConfigGroup() {
			super( GROUP_NAME );
		}

		@StringGetter( "strategy" )
		public Strategy getStrategy() {
			return this.strategy;
		}

		@StringSetter( "strategy" )
		public void setStrategy(Strategy strategy) {
			this.strategy = strategy;
		}
	}

	/**
	 * Better not to use non-flat config for EUNOIA stuff: we kind of need to do
	 * a very specific config group or use the underscore approach...
	 */
	private static class DenivelationScoringConfigGroup extends ReflectiveModule {
		public static final String GROUP_NAME = "denivelationScoring";

		/**
		 * not sure it actually makes sense / is possible to get this from other models...
		 */
		private double bikeMarginalUtilityOfDenivelation_m = 0;
		private double walkMarginalUtilityOfDenivelation_m = 0;

		public DenivelationScoringConfigGroup() {
			super( GROUP_NAME );
		}

		@StringGetter( "bikeMarginalUtilityOfDenivelation_m" )
		public double getBikeMarginalUtilityOfDenivelation_m() {
			return this.bikeMarginalUtilityOfDenivelation_m;
		}

		@StringSetter( "bikeMarginalUtilityOfDenivelation_m" )
		public void setBikeMarginalUtilityOfDenivelation_m(
				final double bikeMarginalUtilityOfDenivelation_m) {
			this.bikeMarginalUtilityOfDenivelation_m = bikeMarginalUtilityOfDenivelation_m;
		}

		@StringGetter( "walkMarginalUtilityOfDenivelation_m" )
		public double getWalkMarginalUtilityOfDenivelation_m() {
			return this.walkMarginalUtilityOfDenivelation_m;
		}

		@StringSetter( "walkMarginalUtilityOfDenivelation_m" )
		public void setWalkMarginalUtilityOfDenivelation_m(
				final double walkMarginalUtilityOfDenivelation_m) {
			this.walkMarginalUtilityOfDenivelation_m = walkMarginalUtilityOfDenivelation_m;
		}

		public SimpleElevationScorerParameters getParameters() {
			final SimpleElevationScorerParameters params = new SimpleElevationScorerParameters();

			params.addParams(
					TransportMode.bike,
					getBikeMarginalUtilityOfDenivelation_m() );
			params.addParams(
					BikeSharingConstants.MODE,
					getBikeMarginalUtilityOfDenivelation_m() );

			params.addParams(
					TransportMode.walk,
					getWalkMarginalUtilityOfDenivelation_m() );

			return params;
		}
	}
}

