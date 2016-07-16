/* *********************************************************************** *
 * project: org.matsim.*												   *
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
package org.matsim.core.mobsim.qsim;

import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author nagel
 *
 */
public class TeleportationEngineWDistanceCheckTest {
	private static final Logger log = Logger.getLogger( TeleportationEngineWDistanceCheckTest.class ) ;
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;
	
	@Test
	public final void test() {
		Config config = ConfigUtils.createConfig();
		config.controler().setOutputDirectory( utils.getOutputDirectory() );
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
		config.controler().setLastIteration(0);
		
		ActivityParams params = new ActivityParams("dummy" ) ;
		config.planCalcScore().addActivityParams(params);
		params.setScoringThisActivityAtAll(false);
		
		StrategySettings stratSets = new StrategySettings() ;
		stratSets.setStrategyName( DefaultSelector.ChangeExpBeta.name() );
		stratSets.setWeight(1.);
		config.strategy().addStrategySettings( stratSets );
		
		Scenario scenario = ScenarioUtils.createScenario( config ) ;
		
		Network network = scenario.getNetwork() ;
		NetworkFactory nf = network.getFactory() ;

		final int N_NODES = 2;
		Node prevNode = null ;
		for ( int ii=0 ; ii<=N_NODES ; ii++ ) {
			Node node = nf.createNode( Id.createNodeId( ii), new Coord(10000.*ii,0.) ) ;
			network.addNode(node);
			if ( ii >= 1 ) {
				Link link = nf.createLink( Id.createLinkId( Integer.toString(ii-1) + "-" + Integer.toString(ii) ), prevNode, node ) ;
				network.addLink(link);
			}
			prevNode = node ;
		}

		Population population = scenario.getPopulation() ;
		PopulationFactory pf = population.getFactory() ;
		
		Person person = pf.createPerson( Id.createPersonId(0) ) ;
		population.addPerson( person );
		
		Plan plan = pf.createPlan();
		person.addPlan( plan ) ;
		{		
			Activity act = pf.createActivityFromCoord("dummy",new Coord(0.,-10000.) ) ;
			plan.addActivity(act);
			act.setEndTime(0.);
		}
		{
			Leg leg = pf.createLeg( TransportMode.car ) ;
			plan.addLeg( leg );
		}
		{		
			Activity act = pf.createActivityFromCoord("dummy",new Coord(20000.,-1.) ) ;
			plan.addActivity(act);
		}
		
		Controler controler = new Controler( scenario ) ;
		controler.addOverridingModule( new AbstractModule(){
			@Override public void install() {
				this.addEventHandlerBinding().toInstance( new BasicEventHandler(){
					@Override public void reset(int iteration) {
					}
					@Override public void handleEvent(Event event) {
						log.warn( event.toString() ) ; 
					}
					
				});
			}
			
		});
		
		controler.run();
	}

}
