/* *********************************************************************** *
 * project: org.matsim.*
 * SimulateTeleportation.java
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
package tutorial.unsupported.implementANewComplexMode;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.Module;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.TripRouterFactoryImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.PtConstants;

/**
 * @author thibautd
 */
public class SimulateTeleportation {
	private static final Logger log =
		Logger.getLogger(SimulateTeleportation.class);

	// this assumes the script is launched from matsim/trunk/
	private static final String configFile = "examples/pt-tutorial/config.xml";
	private static final String MAIN_MODE = "myTeleportationMainMode"; 

	public static void main(final String[] args) {
		// make sure we get all the log messages in the logfile
		OutputDirectoryLogging.catchLogEntries();
		final Config config = ConfigUtils.loadConfig( configFile );

		// please do not do this at home.
		// This is done here to allow using the standard config file only.
		tuneConfig( config );

		final Scenario scenario = ScenarioUtils.loadScenario( config );
		final Controler controler = new Controler( scenario );

		final Facility teleport1 =
			createFacility(
					new IdImpl( "teleport1" ), 
					controler.getScenario().getNetwork().getLinks().get( new IdImpl( "1121" ) ));

		final Facility teleport2 =
			createFacility(
					new IdImpl( "teleport2" ), 
					controler.getScenario().getNetwork().getLinks().get( new IdImpl( "4434" ) ));

		controler.setTripRouterFactory(
				new TripRouterFactory() {
					@Override
					public TripRouter createTripRouter() {
						// this factory initializes a TripRouter with default modules.
						// This allows us to just add our module and go.
						final TripRouterFactory delegate =
							new TripRouterFactoryImpl(
								controler.getScenario(),
								controler.getTravelDisutilityFactory(),
								controler.getLinkTravelTimes(),
								controler.getLeastCostPathCalculatorFactory(),
								controler.getTransitRouterFactory() );

						final TripRouter router = delegate.createTripRouter();

						router.setRoutingModule(
							MAIN_MODE,
							new MyRoutingModule(
								// use the default routing module for the
								// public transport sub-part.
								// It will adapt to the configuration (teleported,
								// simulated, user implementation...)
								router.getRoutingModule( TransportMode.pt ),
								controler.getScenario().getPopulation().getFactory(),
								teleport1,
								teleport2));

						final MainModeIdentifier defaultModeIdentifier =
							router.getMainModeIdentifier();
						router.setMainModeIdentifier(
								new MainModeIdentifier() {
									@Override
									public String identifyMainMode(
											final List<PlanElement> tripElements) {
										for ( PlanElement pe : tripElements ) {
											if ( pe instanceof Leg && ((Leg) pe).getMode().equals( MyRoutingModule.TELEPORTATION_LEG_MODE ) ) {
												return MAIN_MODE;
											}
										}
										// if the trip doesn't contain a teleportation leg,
										// fall back to the default identification method.
										return defaultModeIdentifier.identifyMainMode( tripElements );
									}
								});

						return router;
					}
				});

		controler.run();
	}

	private static void tuneConfig(final Config config) {
		config.addModule( "changeLegMode" , new Module( "changeLegMode" ) );
		config.getModule( "changeLegMode" ).addParam( "modes" , "car,pt,"+MAIN_MODE );

		final ActivityParams scoreTelepInteract = new ActivityParams( MyRoutingModule.STAGE );
		scoreTelepInteract.setTypicalDuration( 2 * 60 );
		scoreTelepInteract.setOpeningTime( 0 );
		scoreTelepInteract.setClosingTime( 0 );
		config.planCalcScore().addActivityParams( scoreTelepInteract );
	}

	private static Facility createFacility(
			final Id id,
			final Link link) {
		if ( link == null ) throw new IllegalArgumentException( "link == "+link );
		return new Facility() {
			@Override
			public Coord getCoord() {
				return link.getFromNode().getCoord();
			}

			@Override
			public Id getId() {
				return id;
			}

			@Override
			public Map<String, Object> getCustomAttributes() {
				throw new UnsupportedOperationException();
			}

			@Override
			public Id getLinkId() {
				return link.getId();
			}
		};
	}
}

