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
package org.matsim.contrib.socnetsim.run;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.contrib.socnetsim.framework.SocialNetworkConfigGroup;
import org.matsim.contrib.socnetsim.framework.controller.JointDecisionProcessModule;
import org.matsim.contrib.socnetsim.framework.controller.SocialNetworkModule;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.contrib.socnetsim.framework.population.SocialNetworkReader;
import org.matsim.contrib.socnetsim.jointactivities.scoring.JointActivitiesScoringModule;
import org.matsim.contrib.socnetsim.jointtrips.JointTripsModule;
import org.matsim.contrib.socnetsim.usage.ConfigConfiguredPlanLinkIdentifierModule;
import org.matsim.contrib.socnetsim.usage.analysis.SocnetsimDefaultAnalysisModule;
import org.matsim.contrib.socnetsim.usage.replanning.DefaultGroupStrategyRegistryModule;

/**
 * Starts a simulation with a social network, joint trips and joint activities.
 * @author thibautd
 */
public class RunSocialNetwork {
	public static void main(final String[] args) {
		final String configFile = args[ 0 ];

		final Scenario scenario = RunUtils.createScenario( configFile );
		final Config config = scenario.getConfig();

		// TODO: put somehow in a module
		final SocialNetworkConfigGroup snConf = (SocialNetworkConfigGroup)
				config.getModule( SocialNetworkConfigGroup.GROUP_NAME );

		new SocialNetworkReader( scenario ).readFile( snConf.getInputFile() );

		final SocialNetwork sn = (SocialNetwork) scenario.getScenarioElement( SocialNetwork.ELEMENT_NAME );
		for ( Id p : scenario.getPopulation().getPersons().keySet() ) {
			if ( !sn.getEgos().contains( p ) ) sn.addEgo( p );
		}

		final Controler controler = new Controler( scenario );
		controler.addOverridingModule( new JointDecisionProcessModule() );
		// One needs to add the various features one wants to use in one module to be safe:
		// this way, if two features conflict, a crash will occur at injection.
		controler.addOverridingModule(
				new AbstractModule() {
					@Override
					public void install() {
						install( new ConfigConfiguredPlanLinkIdentifierModule());
						install( new SocnetsimDefaultAnalysisModule() );
						install( new JointActivitiesScoringModule() );
						install( new DefaultGroupStrategyRegistryModule() );
						install( new JointTripsModule() );
						install( new SocialNetworkModule() );
					}
				} );
		controler.run();
	}
}

