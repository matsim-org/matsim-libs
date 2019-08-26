/* *********************************************************************** *
 * project: org.matsim.*
 * RoadPricingControlerTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.contrib.roadpricing;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.jdeqsim.Road;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tests the integration of the roadpricing-package into the Controler.
 *
 * @author mrieser
 */
public class RoadPricingControlerIT {
	
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
	public void testPaidTollsEndUpInScores() {
		// first run basecase
//		Config config = utils.loadConfig(IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("equil-extended"), "config.xml"));
		Config config = ConfigUtils.loadConfig(IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("equil-extended"), "config.xml"));
		config.controler().setLastIteration(0);
		config.plans().setInputFile("plans1.xml");
		config.controler().setOutputDirectory(utils.getOutputDirectory() + "/basecase/");
		config.controler().setWritePlansInterval(0);

		config.controler().setOutputDirectory(utils.getOutputDirectory() + "/tollcase/");

//		ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class).setTollLinksFile( "distanceToll.xml" ) ;
//		final String tollLinksFile = IOUtils.newUrl( config.getContext() , "distanceToll.xml" ).toString();

		String tollLinksFile = "/Users/kainagel/git/matsim/examples/target/classes/test/scenarios/equil-extended/distanceToll.xml";
		ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class ).setTollLinksFile(
			  tollLinksFile );

		Scenario scenario = ScenarioUtils.loadScenario( config );

		RoadPricingUtils.loadRoadPricingScheme( scenario ) ;
//
//		Controler controler1 = new Controler(scenario);
//		controler1.getConfig().controler().setCreateGraphs(false);
//		controler1.getConfig().controler().setDumpDataAtEnd(false);
//		controler1.getConfig().controler().setWriteEventsInterval(0);
//		controler1.run();
//		double scoreBasecase = controler1.getScenario().getPopulation().getPersons().get(Id.create("1", Person.class)).getPlans().get(0).getScore();


//		RoadPricingScheme scheme = new RoadPricingScheme(){
//
//			@Override public String getName(){
//				throw new RuntimeException( "not implemented" );
//			}
//
//			@Override public String getType(){
//				throw new RuntimeException( "not implemented" );
//			}
//
//			@Override public String getDescription(){
//				throw new RuntimeException( "not implemented" );
//			}
//
//			@Override public Set<Id<Link>> getTolledLinkIds(){
//				throw new RuntimeException( "not implemented" );
//			}
//
//			@Override
//			public RoadPricingSchemeImpl.Cost getLinkCostInfo( Id<Link> linkId , double time , Id<Person> personId , Id<Vehicle> vehicleId ){
//				throw new RuntimeException( "not implemented" );
//			}
//
//			@Override public RoadPricingSchemeImpl.Cost getTypicalLinkCostInfo( Id<Link> linkId , double time ){
//				throw new RuntimeException( "not implemented" );
//			}
//
//			@Override public Iterable<RoadPricingSchemeImpl.Cost> getTypicalCosts(){
//				throw new RuntimeException( "not implemented" );
//			}
//
//			@Override public Map<Id<Link>, List<RoadPricingSchemeImpl.Cost>> getTypicalCostsForLink(){
//				throw new RuntimeException( "not implemented" );
//			}
//		};;
//
//		scenario.addScenarioElement(RoadPricingScheme.ELEMENT_NAME, scheme );


		// now run toll case
		//        ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class).setUseRoadpricing(true);
//		ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class).setTollLinksFile(IOUtils.newUrl(utils.inputResourcePath(), "distanceToll.xml").toString());
		Controler controler2 = new Controler(scenario);
		/* FIXME Check if the following is correct, jwj '19. What's the difference? */
//		controler2.setModules(new RoadPricingModuleDefaults());
		controler2.addOverridingModule( new RoadPricingModule() );
		controler2.getConfig().controler().setCreateGraphs(false);
		controler2.getConfig().controler().setDumpDataAtEnd(false);
		controler2.getConfig().controler().setWriteEventsInterval(0);
		controler2.run();
//		double scoreTollcase = controler2.getScenario().getPopulation().getPersons().get(Id.create("1", Person.class)).getPlans().get(0).getScore();
		
		// there should be a score difference
//		Assert.assertEquals(3.0, scoreBasecase - scoreTollcase, MatsimTestUtils.EPSILON); // toll amount: 10000*0.00020 + 5000*0.00020
	}

	@Test
	public void testRoadPricingWithoutScheme() {

		Config config = ConfigUtils.createConfig() ;
		config.controler().setLastIteration( 1 );
		config.controler().setOutputDirectory( utils.getOutputDirectory() );

		RoadPricingConfigGroup rpConfig = ConfigUtils.addOrGetModule( config , RoadPricingConfigGroup.class );
		rpConfig.setTollLinksFile( "abc" );

		Scenario scenario = ScenarioUtils.createScenario( config ) ;

		RoadPricingUtils.loadRoadPricingScheme( scenario ) ;

		Controler controler = new Controler( scenario ) ;

		controler.addOverridingModule( new RoadPricingModule() ) ;

		controler.run() ;

	}


}
