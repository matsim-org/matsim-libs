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
package eu.eunoiaproject.bikesharing.examples.example03configurablesimulation;

import java.io.File;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.experimental.ReflectiveConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerDefaults;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.utils.io.UncheckedIOException;

import playground.thibautd.utils.SoftCache;

import eu.eunoiaproject.bikesharing.framework.BikeSharingConstants;
import eu.eunoiaproject.bikesharing.framework.qsim.BikeSharingWithoutRelocationQsimFactory;
import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingScenarioUtils;
import eu.eunoiaproject.bikesharing.scoring.StepBasedFare;
import eu.eunoiaproject.bikesharing.scoring.StepBasedFareConfigGroup;

/**
 * @author thibautd
 */
public class RunConfigurableBikeSharingSimulation {
	private static final Logger log =
		Logger.getLogger(RunConfigurableBikeSharingSimulation.class);

	public static void main(final String[] args) {
		final String configFile = args[ 0 ];

		OutputDirectoryLogging.catchLogEntries();
		Logger.getLogger( SoftCache.class ).setLevel( Level.TRACE );

		final Config config = BikeSharingScenarioUtils.loadConfig( configFile );
		config.addModule( new StepBasedFareConfigGroup() );
		config.addModule( new MultiModalConfigGroup() );

		failIfExists( config.controler().getOutputDirectory() );

		if ( !config.planCalcScore().getModes().containsKey( BikeSharingConstants.MODE ) ) {
			log.warn( "adding the disutility of bike sharing programmatically!" );
			final ModeParams bikesharing = config.planCalcScore().getOrCreateModeParams( BikeSharingConstants.MODE );
			final ModeParams bike = config.planCalcScore().getOrCreateModeParams( TransportMode.bike );

			bikesharing.setConstant( bike.getConstant() );
			bikesharing.setMarginalUtilityOfDistance( bike.getMarginalUtilityOfDistance() );
			bikesharing.setMarginalUtilityOfTraveling( bike.getMarginalUtilityOfTraveling() );
			bikesharing.setMonetaryDistanceCostRate( bike.getMonetaryDistanceCostRate() );
		}

		final RelocationConfigGroup relocationGroup = new RelocationConfigGroup();
		config.addModule( relocationGroup );

		final Scenario sc = BikeSharingScenarioUtils.loadScenario( config );

		final Controler controler = new Controler( sc );

		controler.setTripRouterFactory(
				BikeSharingScenarioUtils.createTripRouterFactoryAndConfigureRouteFactories(
					controler.getTravelDisutilityFactory(),
					controler.getScenario(),
					null ) );

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
				new ScoringFunctionWithFareFactory(
					sc ) );

		controler.run();
	}

	private static void failIfExists(final String outdir) {
		final File file = new File( outdir +"/" );
		if ( file.exists() && file.list().length > 0 ) {
			throw new UncheckedIOException( "Directory "+outdir+" exists and is not empty!" );
		}
	}

	private static class RelocationConfigGroup extends ReflectiveConfigGroup {
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

	private static class ScoringFunctionWithFareFactory implements ScoringFunctionFactory {
		private final ScoringFunctionFactory delegate;
		private final Scenario sc;

		public ScoringFunctionWithFareFactory( final Scenario sc ) {
			this.delegate = ControlerDefaults.createDefaultScoringFunctionFactory( sc );
			this.sc = sc;
		}

		@Override
		public ScoringFunction createNewScoringFunction( final Person person ) {
			final SumScoringFunction sum = (SumScoringFunction) delegate.createNewScoringFunction( person );

			sum.addScoringFunction(
					new StepBasedFare(
						sc.getConfig().planCalcScore(),
						(StepBasedFareConfigGroup) sc.getConfig().getModule(
							StepBasedFareConfigGroup.GROUP_NAME ) ) );

			return sum;
		}

	}

}

