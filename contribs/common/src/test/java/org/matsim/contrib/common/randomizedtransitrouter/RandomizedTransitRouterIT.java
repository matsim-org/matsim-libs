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
package org.matsim.contrib.common.randomizedtransitrouter;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspDefaultsCheckingLevel;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;


/**
 * @author nagel
 *
 */
public class RandomizedTransitRouterIT {
	private static Logger log = Logger.getLogger( RandomizedTransitRouterIT.class ) ;
	
	private static final class MyObserver implements PersonEntersVehicleEventHandler {
//		private enum ObservedVehicle{ pt_1009_1 /*direct, fast, with wait*/, pt_2009_1 /*direct, slow*/, pt_3009_1 /*with interchange*/} ;
		
		Map<Id<Vehicle>,Double> cnts = new HashMap<>() ;		

		@Override public void reset(int iteration) {
			cnts.clear();
		}

		@Override public void handleEvent(PersonEntersVehicleEvent event) {
			final Double oldVal = cnts.get( event.getVehicleId() );
			if ( oldVal!= null ) {
				cnts.put( event.getVehicleId(), oldVal + 1. ) ;
			} else {
				cnts.put( event.getVehicleId(), 1. ) ;
			}
		}
		
		void printCounts() {
			for ( Entry<Id<Vehicle>, Double> entry : cnts.entrySet() ) {
				log.info( "Vehicle id: " + entry.getKey() + "; number of boards: " + entry.getValue() ) ;
			}
		}
		
		Map< Id<Vehicle>, Double> getCounts() {
			return this.cnts ;
		}
	}

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
//	@Ignore
	public final void test() {
		String scenarioDir = utils.getPackageInputDirectory() ;
		String outputDir = utils.getOutputDirectory() ;

		Config config = ConfigUtils.createConfig();
		
		config.network().setInputFile(scenarioDir + "/network.xml");
		config.plans().setInputFile(scenarioDir + "/population.xml");

		config.transit().setTransitScheduleFile(scenarioDir + "/transitschedule.xml");
		config.transit().setVehiclesFile( scenarioDir + "/transitVehicles.xml" );
		config.transit().setUseTransit(true);
		
		config.controler().setOutputDirectory( outputDir );
		config.controler().setLastIteration(20);
		
		config.global().setNumberOfThreads(1);
		
		{
			ActivityParams params = new ActivityParams("home") ;
			params.setTypicalDuration( 6*3600. );
			config.planCalcScore().addActivityParams(params);
		}
		{
			ActivityParams params = new ActivityParams("education_100") ;
			params.setTypicalDuration( 6*3600. );
			config.planCalcScore().addActivityParams(params);
		}
		{
			StrategySettings stratSets = new StrategySettings(ConfigUtils.createAvailableStrategyId(config)) ;
			stratSets.setStrategyName( DefaultStrategy.ReRoute.name() );
			stratSets.setWeight(0.1);
			config.strategy().addStrategySettings(stratSets);
		}
		{
			StrategySettings stratSets = new StrategySettings(ConfigUtils.createAvailableStrategyId(config)) ;
			stratSets.setStrategyName( DefaultSelector.ChangeExpBeta.name() );
			stratSets.setWeight(0.9);
			config.strategy().addStrategySettings(stratSets);
		}
		
		config.qsim().setEndTime(18.*3600.);
		
		config.timeAllocationMutator().setMutationRange(7200);
		config.timeAllocationMutator().setAffectingDuration(false);
		config.plans().setRemovingUnneccessaryPlanAttributes(true);
		config.qsim().setTrafficDynamics( TrafficDynamics.withHoles );
		config.qsim().setUsingFastCapacityUpdate(true);
		
		config.vspExperimental().setWritingOutputEvents(true);
		config.vspExperimental().setVspDefaultsCheckingLevel( VspDefaultsCheckingLevel.warn );
		
		// ---
		
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		
		// ---
		
		Controler controler = new Controler( scenario ) ;
//		controler.setDirtyShutdown(true);
		
		controler.addOverridingModule( new RandomizedTransitRouterModule() );

		final MyObserver observer = new MyObserver();
		controler.getEvents().addHandler(observer);
		
		controler.run();
		
		// ---
		
		observer.printCounts(); 
		
		// the following is just a regression test, making sure that results remain stable.  In general, the randomized transit router 
		// could be improved, for example along the lines of the randomized regular router, which uses a (hopefully unbiased) lognormal
		// distribution rather than a biased uniform distribution as is used here.  kai, jul'15
		
		Assert.assertEquals(36., observer.getCounts().get( Id.create("1009", Vehicle.class) ), 0.1 );
		Assert.assertEquals( 6., observer.getCounts().get( Id.create("1012", Vehicle.class) ) , 0.1 );
		Assert.assertEquals(21., observer.getCounts().get( Id.create("2009", Vehicle.class) ) , 0.1 );
		Assert.assertEquals(36., observer.getCounts().get( Id.create("3009", Vehicle.class) ) , 0.1 );
		
		
	}

}
