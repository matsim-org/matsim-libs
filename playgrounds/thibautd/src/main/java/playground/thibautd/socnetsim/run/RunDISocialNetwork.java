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
package playground.thibautd.socnetsim.run;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;

import playground.thibautd.socnetsim.SocialNetworkConfigGroup;
import playground.thibautd.socnetsim.framework.controller.JointDecisionProcessModule;
import playground.thibautd.socnetsim.jointactivities.scoring.JointActivitiesScoringModule;
import playground.thibautd.socnetsim.jointtrips.JointTripsModule;
import playground.thibautd.socnetsim.framework.controller.SocialNetworkModule;
import playground.thibautd.socnetsim.framework.SocnetsimDefaultAnalysisModule;
import playground.thibautd.socnetsim.framework.population.SocialNetwork;
import playground.thibautd.socnetsim.framework.population.SocialNetworkReader;
import playground.thibautd.socnetsim.framework.replanning.GroupStrategyManagerModule;

/**
 * @author thibautd
 */
public class RunDISocialNetwork {
	public static void main(final String[] args) {
		final String configFile = args[ 0 ];

		final Scenario scenario = RunUtils.createScenario( configFile );
		final Config config = scenario.getConfig();

		// TODO: put somehow in a module
		final SocialNetworkConfigGroup snConf = (SocialNetworkConfigGroup)
				config.getModule( SocialNetworkConfigGroup.GROUP_NAME );

		new SocialNetworkReader( scenario ).parse( snConf.getInputFile() );

		final SocialNetwork sn = (SocialNetwork) scenario.getScenarioElement( SocialNetwork.ELEMENT_NAME );
		for ( Id p : scenario.getPopulation().getPersons().keySet() ) {
			if ( !sn.getEgos().contains( p ) ) sn.addEgo( p );
		}

		final Controler controler = new Controler( scenario );
		controler.addOverridingModule( new JointDecisionProcessModule() );
		controler.addOverridingModule( new SocnetsimDefaultAnalysisModule() );
		controler.addOverridingModule( new JointActivitiesScoringModule() );
		controler.addOverridingModule( new GroupStrategyManagerModule() );
		controler.addOverridingModule( new JointTripsModule() );
		controler.addOverridingModule( new SocialNetworkModule() );
		controler.run();
	}
}

