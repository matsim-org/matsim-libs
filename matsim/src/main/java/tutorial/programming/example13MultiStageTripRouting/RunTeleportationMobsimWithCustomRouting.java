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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author thibautd
 */
public class RunTeleportationMobsimWithCustomRouting {
	// this assumes the script is launched from matsim/trunk/
	private static final String configFile = "examples/pt-tutorial/config.xml";

	public static void main(final String[] args) {
		// make sure we get all the log messages in the logfile
		OutputDirectoryLogging.catchLogEntries();

		// load the config ...
		final Config config = ConfigUtils.loadConfig( configFile );
		// ... and add local changes:
		tuneConfig( config );

		// load the scenario:
		final Scenario scenario = ScenarioUtils.loadScenario( config );
		
		// load the controler:
		final Controler controler = new Controler( scenario );

		// create the teleportation station on a central link
		final ActivityFacility teleport =
			createFacility(
					Id.create( "teleport" , ActivityFacility.class),
					controler.getScenario().getNetwork().getLinks().get( Id.create( "2333", Link.class ) ));

		// now, plug our stuff in
		controler.setTripRouterFactory(
				new MyTripRouterFactory(
						scenario,
						teleport));
		
		// run the controler:
		controler.run();
	}

	private static void tuneConfig(final Config config) {
		config.getModule( "changeLegMode" ).addParam( "modes" , "car,pt,"+MyTripRouterFactory.TELEPORTATION_MAIN_MODE );

		final ActivityParams scoreTelepInteract = new ActivityParams( MyRoutingModule.STAGE );
		scoreTelepInteract.setTypicalDuration( 2 * 60 );
		scoreTelepInteract.setOpeningTime( 0 );
		scoreTelepInteract.setClosingTime( 0 );
		config.planCalcScore().addActivityParams( scoreTelepInteract );
	}

	private static ActivityFacility createFacility(
			final Id<ActivityFacility> id,
			final Link link) {
		if ( link == null ) throw new IllegalArgumentException( "link == "+link );
		
		return new ActivityFacility() {
			@Override
			public Coord getCoord() {
				return link.getFromNode().getCoord();
			}

			@Override
			public Id<ActivityFacility> getId() {
				return id;
			}

			@Override
			public Map<String, Object> getCustomAttributes() {
				throw new UnsupportedOperationException();
			}

			@Override
			public Id<Link> getLinkId() {
				return link.getId();
			}
			
			@Override
			public void addActivityOption(ActivityOption option) {
				throw new UnsupportedOperationException();
			}
			
			@Override
			public Map<String, ActivityOption> getActivityOptions() {
				throw new UnsupportedOperationException();
			}
		};
	}
}

