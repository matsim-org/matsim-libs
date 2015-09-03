/* *********************************************************************** *
 * project: org.matsim.*
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

package eu.eunoiaproject.examples.thinnedtransitrouternetwork;

import eu.eunoiaproject.examples.schedulebasedteleportation.ScheduleBasedTripRouterFactory;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.PtConstants;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import playground.ivt.matsim2030.router.TransitRouterNetworkReader;
import playground.ivt.matsim2030.router.TransitRouterWithThinnedNetworkFactory;

import javax.inject.Provider;

public class RunSimulationWithThinnedRouterNetwork {
	private static final Logger log =
		Logger.getLogger(RunSimulationWithThinnedRouterNetwork.class);


	public static void main( final String[] args ) {
		final String configFile = args[ 0 ];

		// This created a config group ("module") in the config file,
		// named "customPtRouting", with one single parameter,
		// "thinnedNetworkFile", that you need to set to the path of your
		// thinned network, if it is different of "input/thinnedTransitRouterNetwork.xml.gz"
		final ThinnedNetworkConfigGroup thinnedNetworkConfigGroup =
					new ThinnedNetworkConfigGroup();
		final Config config =
			ConfigUtils.loadConfig(
				configFile,
				thinnedNetworkConfigGroup );
		setupTransitActivityParams( config );

		final Scenario scenario = ScenarioUtils.loadScenario( config );
		loadTransitInScenario( scenario );

		final Controler controler = new Controler( scenario );

		// This configures the routing in a way that uses the "thinned" network.
		controler.setTripRouterFactory(
				thinnedNetworkConfigGroup.isTeleportRatherThanSimulate() ?
					createTeleportingTripRouterFactory(
						thinnedNetworkConfigGroup.getThinnedNetworkFile(),
						scenario ) :
					createTripRouterFactory(
						thinnedNetworkConfigGroup.getThinnedNetworkFile(),
						scenario ) );
		
		controler.run();
	}

	private static Provider<TransitRouter> createTransitRouterFactory(
			final String routingNetworkFile,
			final Scenario scenario ) {
		final TransitRouterConfig conf = new TransitRouterConfig( scenario.getConfig() );

		final TransitRouterNetwork transitRouterNetwork = new TransitRouterNetwork();
		new TransitRouterNetworkReader(
				scenario.getTransitSchedule(),
				transitRouterNetwork ).parse(
					routingNetworkFile );

		final TransitRouterWithThinnedNetworkFactory transitRouterFactory =
			new TransitRouterWithThinnedNetworkFactory(
					scenario.getTransitSchedule(),
					conf,
					transitRouterNetwork );

		return transitRouterFactory;
	}

	private static TripRouterFactory createTripRouterFactory(
			final String routingNetworkFile,
			final Scenario scenario ) {
		final TripRouterFactoryBuilderWithDefaults builder = new TripRouterFactoryBuilderWithDefaults();
		
		builder.setTransitRouterFactory(
				createTransitRouterFactory(
					routingNetworkFile,
					scenario ) );

		return builder.build( scenario );
	}

	private static TripRouterFactory createTeleportingTripRouterFactory(
			final String routingNetworkFile,
			final Scenario scenario ) {
		return new ScheduleBasedTripRouterFactory(
				createTransitRouterFactory(
					routingNetworkFile,
					scenario ),
				scenario );
	}

	private static final void loadTransitInScenario( final Scenario scenario ) {
		final Config config = scenario.getConfig();
		// if actual simulation of transit is disabled, the transit schedule
		// is not loaded automatically: we need to do it by hand
		if ( !config.transit().isUseTransit() ) {
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

	private static void setupTransitActivityParams( final Config config ) {
		if ( config.planCalcScore().getActivityTypes().contains( PtConstants.TRANSIT_ACTIVITY_TYPE ) ) return;

		// this is normally done in the Controler if transit is enabled
		final ActivityParams transitActivityParams = new ActivityParams(PtConstants.TRANSIT_ACTIVITY_TYPE);
		transitActivityParams.setTypicalDuration(120.0);

		transitActivityParams.setOpeningTime(0.) ;
		transitActivityParams.setClosingTime(0.) ;

		config.planCalcScore().addActivityParams(transitActivityParams);
	}
	

	private static class ThinnedNetworkConfigGroup extends ReflectiveConfigGroup {
		private static final String GROUP_NAME = "customPtRouting";

		private boolean teleportRatherThanSimulate = false;
		private String thinnedNetworkFile = "input/thinnedTransitRouterNetwork.xml.gz";

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

