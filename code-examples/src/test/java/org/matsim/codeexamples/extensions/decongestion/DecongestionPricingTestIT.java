/* *********************************************************************** *
 * project: org.matsim.*
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

/**
 *
 */
package org.matsim.codeexamples.extensions.decongestion;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.decongestion.DecongestionConfigGroup;
import org.matsim.contrib.decongestion.DecongestionModule;
import org.matsim.contrib.decongestion.data.DecongestionInfo;
import org.matsim.contrib.decongestion.data.LinkInfo;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.net.URL;

/**
 *
 *
 * @author ikaddoura
 *
 */
public class DecongestionPricingTestIT{

	@RegisterExtension
	private MatsimTestUtils testUtils = new MatsimTestUtils();


	@Test
	final void testDecongestion() {

		URL configUrl = IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL( "equil" ), "config.xml" );

		Config config = ConfigUtils.loadConfig( configUrl );
		config.controller().setOutputDirectory( testUtils.getOutputDirectory()  );

		config.plans().setInputFile( "plans2000.xml.gz" );
		// (in my first attempts, the default plans file had too few agents.  after my later changes, it may no longer be necessary to use this file here.  kai, jan'23)

		config.controller().setLastIteration( 20 );
		// (need some iterations for the decongestion to unfold.  20 may be more than really needed.  kai, jan'23)

		final DecongestionConfigGroup decongestionSettings = ConfigUtils.addOrGetModule( config, DecongestionConfigGroup.class );

		decongestionSettings.setWriteOutputIteration(1);
//		decongestionSettings.setKp(0.0123);
		decongestionSettings.setKp(0.123);
		decongestionSettings.setKd(0.0);
		decongestionSettings.setKi(0.0);
		decongestionSettings.setMsa(false);
		decongestionSettings.setTollBlendFactor(1.0);
		decongestionSettings.setFractionOfIterationsToEndPriceAdjustment(1.0);
		decongestionSettings.setFractionOfIterationsToStartPriceAdjustment(0.0);

		// ===

		final Scenario scenario = ScenarioUtils.loadScenario(config);
		
		Network network = scenario.getNetwork();

		// make middle link faster
		network.getLinks().get( Id.createLinkId( "6" )).setFreespeed( 100. );

		// make alternative wider
		network.getLinks().get( Id.createLinkId( "14" ) ).setCapacity( 100000. );

		// increase some other capacities:
		network.getLinks().get( Id.createLinkId( "5" ) ).setCapacity( 100000. );
		network.getLinks().get( Id.createLinkId( "6" ) ).setCapacity( 100000. );

		// remove all other alternatives:
		network.removeLink( Id.createLinkId( "11" ) );
		network.removeLink( Id.createLinkId( "12" ) );
		network.removeLink( Id.createLinkId( "13" ) );
		network.removeLink( Id.createLinkId( "16" ) );
		network.removeLink( Id.createLinkId( "17" ) );
		network.removeLink( Id.createLinkId( "18" ) );
		network.removeLink( Id.createLinkId( "19" ) );

		network.removeLink( Id.createLinkId( "2" ) );
		network.removeLink( Id.createLinkId( "3" ) );
		network.removeLink( Id.createLinkId( "4" ) );
		network.removeLink( Id.createLinkId( "7" ) );
		network.removeLink( Id.createLinkId( "8" ) );
		network.removeLink( Id.createLinkId( "9" ) );
		network.removeLink( Id.createLinkId( "10" ) );

		// ---

		Population population = scenario.getPopulation();

		// remove 3/4 of the population to reduce computation time:
		for ( int ii=500; ii<2000; ii++ ){
			population.removePerson( Id.createPersonId( ii ) );
		}


		// ---

		Controler controler = new Controler(scenario);

		controler.addOverridingModule(new DecongestionModule() );

//		controler.addOverridingModule( new OTFVisLiveModule() );

		controler.run();

		// ===

		DecongestionInfo info = controler.getInjector().getInstance( DecongestionInfo.class );

		final LinkInfo linkInfo = info.getlinkInfos().get( Id.createLinkId( "15" ) );
		if ( linkInfo!= null ){
			System.out.println( linkInfo.getTime2toll().toString() );
		}

		final TravelTime linkTravelTimes = controler.getLinkTravelTimes();
		double tt0a = linkTravelTimes.getLinkTravelTime(scenario.getNetwork().getLinks().get(Id.createLinkId("15" ) ), 6 * 3600-1 , null, null );
		double tt0b = linkTravelTimes.getLinkTravelTime(scenario.getNetwork().getLinks().get(Id.createLinkId("15" ) ), 6 * 3600 , null, null );
		double tt0c = linkTravelTimes.getLinkTravelTime(scenario.getNetwork().getLinks().get(Id.createLinkId("15" ) ), 6 * 3600+15*60 , null, null );
		double tt1 = linkTravelTimes.getLinkTravelTime(scenario.getNetwork().getLinks().get(Id.createLinkId("14" ) ), 6 * 3600, null, null );

		System.err.println( tt0a + " " + tt0b + " " + tt0c );
		System.err.println( tt1 );

		Assertions.assertEquals(179.985, tt0a, MatsimTestUtils.EPSILON, "Wrong travel time. The run output seems to have changed.");
		Assertions.assertEquals(344.04, tt0b, MatsimTestUtils.EPSILON, "Wrong travel time. The run output seems to have changed.");
		Assertions.assertEquals(179.985, tt0c, MatsimTestUtils.EPSILON, "Wrong travel time. The run output seems to have changed.");
		Assertions.assertEquals(180.0, tt1, MatsimTestUtils.EPSILON, "Wrong travel time. The run output seems to have changed.");

	}


}
