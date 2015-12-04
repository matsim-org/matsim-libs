/* *********************************************************************** *
 * project: org.matsim.*
 * RunSimpleBikeSharingSimulation.java
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
package eu.eunoiaproject.bikesharing.examples.example01runwithoutrelocation.run;

import com.google.inject.Provider;
import eu.eunoiaproject.bikesharing.framework.qsim.BikeSharingWithoutRelocationQsimFactory;
import eu.eunoiaproject.bikesharing.framework.router.BikeSharingTripRouterModule;
import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingScenarioUtils;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.Mobsim;

/**
 * The simplest bike-sharing-simulation-script possible:
 * load scenario, setup routing and mobility simulation,
 * and off we go!
 * @author thibautd
 */
public class RunSimpleBikeSharingSimulation {
	public static void main(final String[] args) {
		final String configFile = args[ 0 ];

		final Scenario sc = BikeSharingScenarioUtils.loadScenario( configFile );
		final Controler controler = new Controler( sc );

		controler.addOverridingModule( new BikeSharingTripRouterModule( sc , null ) );
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider(new Provider<Mobsim>() {
					@Override
					public Mobsim get() {
						return new BikeSharingWithoutRelocationQsimFactory().createMobsim(controler.getScenario(), controler.getEvents());
					}
				});
			}
		});

		controler.run();
	}
}

