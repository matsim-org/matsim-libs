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

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.experimental.ReflectiveModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterFactory;
import org.matsim.pt.router.TransitRouterNetwork;

import playground.ivt.matsim2030.router.TransitRouterNetworkReader;
import playground.ivt.matsim2030.router.TransitRouterWithThinnedNetworkFactory;

import eu.eunoiaproject.examples.schedulebasedteleportation.ScheduleBasedTripRouterFactory;

public class RunSimulationWithThinnedRouterNetwork {

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

		final Scenario scenario = ScenarioUtils.loadScenario( config );

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

	private static TransitRouterFactory createTransitRouterFactory(
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

	private static class ThinnedNetworkConfigGroup extends ReflectiveModule {
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

