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
package tutorial.programming.example13MultiStageTripRouting;

import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author thibautd
 */
public class SimulateTeleportation {
	// this assumes the script is launched from matsim/trunk/
	private static final String configFile = "examples/pt-tutorial/config.xml";
	static final String MAIN_MODE = "myTeleportationMainMode"; 

	public static void main(final String[] args) {
		// make sure we get all the log messages in the logfile
		OutputDirectoryLogging.catchLogEntries();
		final Config config = ConfigUtils.loadConfig( configFile );

		// please do not do this at home.
		// This is done here to allow using the standard config file only.
		tuneConfig( config );

		final Scenario scenario = ScenarioUtils.loadScenario( config );
		final Controler controler = new Controler( scenario );

		// create the two teleportation station:
		// - south west
		final Facility teleport1 =
			createFacility(
					new IdImpl( "teleport1" ), 
					controler.getScenario().getNetwork().getLinks().get( new IdImpl( "1121" ) ));
		// - north east
		final Facility teleport2 =
			createFacility(
					new IdImpl( "teleport2" ), 
					controler.getScenario().getNetwork().getLinks().get( new IdImpl( "4434" ) ));

		// now, plug our stuff in
		controler.setTripRouterFactory(
				new MyTripRouterFactory(
						controler,
						teleport1,
						teleport2));
		controler.run();
	}

	private static void tuneConfig(final Config config) {
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

