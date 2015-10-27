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

import com.google.inject.Provider;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerDefaults;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import playground.ivt.matsim2030.router.TransitRouterNetworkReader;
import playground.thibautd.utils.SoftCache;

import eu.eunoiaproject.bikesharing.framework.BikeSharingConstants;
import eu.eunoiaproject.bikesharing.framework.qsim.BikeSharingWithoutRelocationQsimFactory;
import eu.eunoiaproject.bikesharing.framework.router.TransitMultiModalAccessRoutingModule.RoutingData;
import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingScenarioUtils;
import eu.eunoiaproject.bikesharing.scoring.StepBasedFare;
import eu.eunoiaproject.bikesharing.scoring.StepBasedFareConfigGroup;

/**
 * @author thibautd
 */
public class RunConfigurableBikeSharingSimulation {
	private static final Logger log =
		Logger.getLogger(RunConfigurableBikeSharingSimulation.class);

	public static void main(final String... args) {
		final String configFile = args[ 0 ];

		OutputDirectoryLogging.catchLogEntries();
		Logger.getLogger( SoftCache.class ).setLevel( Level.TRACE );

		final Config config = BikeSharingScenarioUtils.loadConfig( configFile );
		final ThinnedNetworkConfigGroup thinnedNetworkGroup = new ThinnedNetworkConfigGroup();
		config.addModule( thinnedNetworkGroup );
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
			bikesharing.setMonetaryDistanceRate( bike.getMonetaryDistanceRate() );
		}

		final RelocationConfigGroup relocationGroup = new RelocationConfigGroup();
		config.addModule( relocationGroup );

		final Scenario sc = BikeSharingScenarioUtils.loadScenario( config );
		loadTransitInScenario( sc );

		final Controler controler = new Controler( sc );

		final RoutingData ptRouting = createRoutingData( sc, thinnedNetworkGroup );
		controler.addOverridingModule(
				BikeSharingScenarioUtils.createTripRouterFactoryAndConfigureRouteFactories(
					controler.getScenario(),
					null,
					ptRouting,
					thinnedNetworkGroup.isTeleportRatherThanSimulate() ) );

		switch ( relocationGroup.getStrategy() ) {
		case noRelocation:
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bindMobsim().toProvider(new Provider<Mobsim>() {
						@Override
						public Mobsim get() {
							return new BikeSharingWithoutRelocationQsimFactory(false).createMobsim(controler.getScenario(), controler.getEvents());
						}
					});
				}
			});
			break;
		case systemWideCapacities:
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bindMobsim().toProvider(new Provider<Mobsim>() {
						@Override
						public Mobsim get() {
							return new BikeSharingWithoutRelocationQsimFactory(true).createMobsim(controler.getScenario(), controler.getEvents());
						}
					});
				}
			});
			break;
		default:
			throw new RuntimeException();
		}

		controler.setScoringFunctionFactory(
				new ScoringFunctionWithFareFactory(
					sc ) );

		controler.run();
	}

	private static RoutingData createRoutingData(
			final Scenario sc,
			final ThinnedNetworkConfigGroup conf ) {
		if ( conf.getThinnedNetworkFile() == null ) return new RoutingData( sc );

		final TransitRouterNetwork transitRouterNetwork = new TransitRouterNetwork();
		new TransitRouterNetworkReader(
				sc.getTransitSchedule(),
				transitRouterNetwork ).parse(
					conf.getThinnedNetworkFile() );

		return new RoutingData( sc ,  transitRouterNetwork );
	}

	private static void failIfExists(final String outdir) {
		final File file = new File( outdir +"/" );
		if ( file.exists() && file.list().length > 0 ) {
			throw new UncheckedIOException( "Directory "+outdir+" exists and is not empty!" );
		}
	}

	private static final void loadTransitInScenario( final Scenario scenario ) {
		final Config config = scenario.getConfig();
		// if actual simulation of transit is disabled, the transit schedule
		// is not loaded automatically: we need to do it by hand
		if ( !config.transit().isUseTransit() ) {
			if ( config.transit().getTransitScheduleFile() == null ) {
				log.info( "no schedule file defined in config: not loading any schedule information" );
				return;
			}

			log.info( "read schedule from "+config.transit().getTransitScheduleFile() );
			new TransitScheduleReader( scenario ).readFile( config.transit().getTransitScheduleFile() );

			// this is not necessary in the vast majority of applications.
			if ( config.transit().getTransitLinesAttributesFile() != null ) {
				log.info("loading transit lines attributes from " + config.transit().getTransitLinesAttributesFile());
				new ObjectAttributesXmlReader( scenario.getTransitSchedule().getTransitLinesAttributes() ).parse(
						config.transit().getTransitLinesAttributesFile() );
			}
			if ( config.transit().getTransitStopsAttributesFile() != null ) {
				log.info("loading transit stop facilities attributes from " + config.transit().getTransitStopsAttributesFile() );
				new ObjectAttributesXmlReader( scenario.getTransitSchedule().getTransitStopsAttributes() ).parse(
						config.transit().getTransitStopsAttributesFile() );
			}
		}
		else {
			log.info( "Transit will be simulated." );
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

	private static class ThinnedNetworkConfigGroup extends ReflectiveConfigGroup {
		private static final String GROUP_NAME = "customPtRouting";

		private boolean teleportRatherThanSimulate = false;
		private String thinnedNetworkFile = null;

		public ThinnedNetworkConfigGroup( ) {
			super( GROUP_NAME );
		}

		@StringGetter( "teleportRatherThanSimulate" )
		public boolean isTeleportRatherThanSimulate() {
			return teleportRatherThanSimulate;
		}

		@StringSetter( "teleportRatherThanSimulate" )
		public void setTeleportRatherThanSimulate( boolean teleportRatherThanSimulate ) {
			this.teleportRatherThanSimulate = teleportRatherThanSimulate;
		}

		@StringGetter( "thinnedNetworkFile" )
		public String getThinnedNetworkFile() {
			return thinnedNetworkFile;
		}

		@StringSetter( "thinnedNetworkFile" )
		public void setThinnedNetworkFile( String thinnedNetworkFile ) {
			this.thinnedNetworkFile = thinnedNetworkFile;
		}
	}
}

