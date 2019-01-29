/* *********************************************************************** *
 * project: org.matsim.*
 * RunDISocialNetwork.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetsim.examples;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.socnetsim.framework.SocialNetworkConfigGroup;
import org.matsim.contrib.socnetsim.framework.controller.JointDecisionProcessModule;
import org.matsim.contrib.socnetsim.framework.controller.SocialNetworkModule;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.contrib.socnetsim.framework.population.SocialNetworkReader;
import org.matsim.contrib.socnetsim.jointactivities.scoring.JointActivitiesScoringModule;
import org.matsim.contrib.socnetsim.jointtrips.JointTripsModule;
import org.matsim.contrib.socnetsim.run.RunUtils;
import org.matsim.contrib.socnetsim.usage.ConfigConfiguredPlanLinkIdentifierModule;
import org.matsim.contrib.socnetsim.usage.analysis.SocnetsimDefaultAnalysisModule;
import org.matsim.contrib.socnetsim.usage.replanning.DefaultGroupStrategyRegistryModule;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;

/**
 * Starts a simulation with a social network, joint trips and joint activities.
 * @author thibautd
 */
public class RunExampleSocialSimulation {
	public static void main(final String[] args) {
		final String configFile = "examples/siouxfalls-socialnetwork/config.xml";

		final Scenario scenario = RunUtils.createScenario( configFile );
		final Config config = scenario.getConfig();

		final SocialNetworkConfigGroup snConf = (SocialNetworkConfigGroup)
				config.getModule( SocialNetworkConfigGroup.GROUP_NAME );

		new SocialNetworkReader( scenario ).readFile( snConf.getInputFile() );

		final SocialNetwork sn = (SocialNetwork) scenario.getScenarioElement( SocialNetwork.ELEMENT_NAME );
		for ( Id p : scenario.getPopulation().getPersons().keySet() ) {
			if ( !sn.getEgos().contains( p ) ) sn.addEgo( p );
		}

		final Controler controler = new Controler( scenario );
		// The first step is to set the modifications to the co-evolutionary algorithm
		controler.addOverridingModule( new JointDecisionProcessModule() );

		// Then, one can add the "features", as overriding modules, in case they erase
		// defaults from the JointDecisionProcessModule
		// One needs however to add the various features one wants to use in one module to be safe:
		// this way, if two features conflict, a crash will occur at injection.
		controler.addOverridingModule(
				new AbstractModule() {
					@Override
					public void install() {
						// this module enable configuration of joint plan creation from the config file
						install( new ConfigConfiguredPlanLinkIdentifierModule());
						// Some default analysis: not necessary, but cannot hurt
						install( new SocnetsimDefaultAnalysisModule() );
						// Makes the scoring function aware of co-presence at secondary activity
						// locations
						install( new JointActivitiesScoringModule() );
						// Makes the default strategies provided by the framework available
						install( new DefaultGroupStrategyRegistryModule() );
						// Activates joint trip simulation (drivers and passengers waiting each other in the QSim
						install( new JointTripsModule() );
						// Enables storing the Social Network.
						// In the future, the loading routines above should also be enabled automatically
						// by this module.
						install( new SocialNetworkModule() );
					}
				} );

		// We are set!
		controler.run();
	}
}

