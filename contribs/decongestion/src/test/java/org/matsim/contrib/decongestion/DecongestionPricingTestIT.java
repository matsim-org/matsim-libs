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
package org.matsim.contrib.decongestion;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.analysis.ScoreStatsControlerListener.ScoreItem;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.decongestion.DecongestionConfigGroup;
import org.matsim.contrib.decongestion.DecongestionConfigGroup.DecongestionApproach;
import org.matsim.contrib.decongestion.DecongestionControlerListener;
import org.matsim.contrib.decongestion.DecongestionModule;
import org.matsim.contrib.decongestion.data.DecongestionInfo;
import org.matsim.contrib.decongestion.handler.DelayAnalysis;
import org.matsim.contrib.decongestion.handler.IntervalBasedTolling;
import org.matsim.contrib.decongestion.handler.IntervalBasedTollingAll;
import org.matsim.contrib.decongestion.handler.PersonVehicleTracker;
import org.matsim.contrib.decongestion.routing.TollTimeDistanceTravelDisutilityFactory;
import org.matsim.contrib.decongestion.tollSetting.DecongestionTollSetting;
import org.matsim.contrib.decongestion.tollSetting.DecongestionTollingBangBang;
import org.matsim.contrib.decongestion.tollSetting.DecongestionTollingPID;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * 
 * 
 * @author ikaddoura
 *
 */
public class DecongestionPricingTestIT {
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	/**
	 * Kp = 0.0123
	 * 
	 */
	@Test
	public final void test0a() {
		
		System.out.println(testUtils.getPackageInputDirectory());
		
		final String configFile = testUtils.getPackageInputDirectory() + "/config0.xml";
		
		Config config = ConfigUtils.loadConfig(configFile);

		String outputDirectory = testUtils.getOutputDirectory() + "/";
		config.controler().setOutputDirectory(outputDirectory);
		
		final DecongestionConfigGroup decongestionSettings = new DecongestionConfigGroup();
		decongestionSettings.setWRITE_OUTPUT_ITERATION(1);
		decongestionSettings.setKp(0.0123);
		decongestionSettings.setKd(0.0);
		decongestionSettings.setKi(0.0);
		decongestionSettings.setMsa(false);
		decongestionSettings.setTOLL_BLEND_FACTOR(1.0);
		decongestionSettings.setFRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT(1.0);
		decongestionSettings.setFRACTION_OF_ITERATIONS_TO_START_PRICE_ADJUSTMENT(0.0);
		config.addModule(decongestionSettings);
		
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
	
		DecongestionInfo info = new DecongestionInfo();
		
		// congestion toll computation
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				
				this.bind(DecongestionInfo.class).toInstance(info);
				
				this.bind(DecongestionTollSetting.class).to(DecongestionTollingPID.class);
				this.bind(IntervalBasedTolling.class).to(IntervalBasedTollingAll.class);
				
				this.bind(IntervalBasedTollingAll.class).asEagerSingleton();
				this.bind(DelayAnalysis.class).asEagerSingleton();
				this.bind(PersonVehicleTracker.class).asEagerSingleton();
								
				this.addEventHandlerBinding().to(IntervalBasedTollingAll.class);
				this.addEventHandlerBinding().to(DelayAnalysis.class);
				this.addEventHandlerBinding().to(PersonVehicleTracker.class);
				
				this.addControlerListenerBinding().to(DecongestionControlerListener.class);

			}
		});
		
		// toll-adjusted routing
		
		final TollTimeDistanceTravelDisutilityFactory travelDisutilityFactory = new TollTimeDistanceTravelDisutilityFactory();
		travelDisutilityFactory.setSigma(0.);
		
		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
				this.bindCarTravelDisutilityFactory().toInstance( travelDisutilityFactory );
			}
		});	
		
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        controler.run();   
		
        double tt0 = controler.getLinkTravelTimes().getLinkTravelTime(scenario.getNetwork().getLinks().get(Id.createLinkId("link12")), 6 * 3600 + 50. * 60, null, null);
        double tt1 = controler.getLinkTravelTimes().getLinkTravelTime(scenario.getNetwork().getLinks().get(Id.createLinkId("link12")), 7 * 3600 + 63, null, null);
        double tt2 = controler.getLinkTravelTimes().getLinkTravelTime(scenario.getNetwork().getLinks().get(Id.createLinkId("link12")), 7 * 3600 + 15. * 60, null, null);
        
		Assert.assertEquals("Wrong travel time. The run output seems to have changed.", 100.0, tt0, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong travel time. The run output seems to have changed.", 150.5, tt1, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong travel time. The run output seems to have changed.", 100.0, tt2, MatsimTestUtils.EPSILON);
				
		final int index = config.controler().getLastIteration() - config.controler().getFirstIteration();
		double avgScore = controler.getScoreStats().getScoreHistory().get( ScoreItem.executed ).get(index);
		Assert.assertEquals("Wrong average executed score. The tolls seem to have changed.", -33.940316666666666, avgScore, MatsimTestUtils.EPSILON);
		
        System.out.println(info.getlinkInfos().get(Id.createLinkId("link12")).getTime2toll().toString());
        System.out.println(info.getlinkInfos().get(Id.createLinkId("link12")).getTime2avgDelay().toString());

		Assert.assertEquals("Wrong average delay (capacity is set in a way that one of the two agents has to wait 101 sec. Thus the average is 50.5", 50.5, info.getlinkInfos().get(Id.createLinkId("link12")).getTime2avgDelay().get(84), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong toll.", 50.5 * 0.0123, info.getlinkInfos().get(Id.createLinkId("link12")).getTime2toll().get(84), MatsimTestUtils.EPSILON);

	}
	
	/**
	 * Kp = 0.0123, other syntax
	 * 
	 */
	@Test
	public final void test0amodified() {
		
		System.out.println(testUtils.getPackageInputDirectory());
		
		final String configFile = testUtils.getPackageInputDirectory() + "/config0.xml";
		
		Config config = ConfigUtils.loadConfig(configFile);

		String outputDirectory = testUtils.getOutputDirectory() + "/";
		config.controler().setOutputDirectory(outputDirectory);
		
		final DecongestionConfigGroup decongestionSettings = new DecongestionConfigGroup();
		decongestionSettings.setWRITE_OUTPUT_ITERATION(1);
		decongestionSettings.setKp(0.0123);
		decongestionSettings.setKd(0.0);
		decongestionSettings.setKi(0.0);
		decongestionSettings.setMsa(false);
		decongestionSettings.setTOLL_BLEND_FACTOR(1.0);
		decongestionSettings.setFRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT(1.0);
		decongestionSettings.setFRACTION_OF_ITERATIONS_TO_START_PRICE_ADJUSTMENT(0.0);
		config.addModule(decongestionSettings);
		
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
			
		// congestion toll computation
		controler.addOverridingModule(new DecongestionModule(scenario));
		
		// toll-adjusted routing
		
		final TollTimeDistanceTravelDisutilityFactory travelDisutilityFactory = new TollTimeDistanceTravelDisutilityFactory();
		travelDisutilityFactory.setSigma(0.);
		
		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
				this.bindCarTravelDisutilityFactory().toInstance( travelDisutilityFactory );
			}
		});	
		
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        controler.run();   
		
        double tt0 = controler.getLinkTravelTimes().getLinkTravelTime(scenario.getNetwork().getLinks().get(Id.createLinkId("link12")), 6 * 3600 + 50. * 60, null, null);
        double tt1 = controler.getLinkTravelTimes().getLinkTravelTime(scenario.getNetwork().getLinks().get(Id.createLinkId("link12")), 7 * 3600 + 63, null, null);
        double tt2 = controler.getLinkTravelTimes().getLinkTravelTime(scenario.getNetwork().getLinks().get(Id.createLinkId("link12")), 7 * 3600 + 15. * 60, null, null);
        
		Assert.assertEquals("Wrong travel time. The run output seems to have changed.", 100.0, tt0, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong travel time. The run output seems to have changed.", 150.5, tt1, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong travel time. The run output seems to have changed.", 100.0, tt2, MatsimTestUtils.EPSILON);
				
		final int index = config.controler().getLastIteration() - config.controler().getFirstIteration();
		double avgScore = controler.getScoreStats().getScoreHistory().get( ScoreItem.executed ).get(index);
		Assert.assertEquals("Wrong average executed score. The tolls seem to have changed.", -33.940316666666666, avgScore, MatsimTestUtils.EPSILON);
	}
	
	/**
	 * Kp = 2
	 * 
	 */
	@Test
	public final void test0b() {
		
		System.out.println(testUtils.getPackageInputDirectory());
		
		final String configFile = testUtils.getPackageInputDirectory() + "/config0.xml";
		
		Config config = ConfigUtils.loadConfig(configFile);

		String outputDirectory = testUtils.getOutputDirectory() + "/";
		config.controler().setOutputDirectory(outputDirectory);
		
		final DecongestionConfigGroup decongestionSettings = new DecongestionConfigGroup();
		decongestionSettings.setWRITE_OUTPUT_ITERATION(1);
		decongestionSettings.setKp(2.0);
		decongestionSettings.setKd(0.0);
		decongestionSettings.setKi(0.0);
		decongestionSettings.setMsa(false);
		decongestionSettings.setTOLL_BLEND_FACTOR(1.0);
		decongestionSettings.setFRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT(1.0);
		decongestionSettings.setFRACTION_OF_ITERATIONS_TO_START_PRICE_ADJUSTMENT(0.0);
		config.addModule(decongestionSettings);
		
		final Scenario scenario = ScenarioUtils.loadScenario(config);		
		Controler controler = new Controler(scenario);

		DecongestionInfo info = new DecongestionInfo();
		
		// congestion toll computation
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				
				this.bind(DecongestionInfo.class).toInstance(info);
				
				this.bind(DecongestionTollSetting.class).to(DecongestionTollingPID.class);
				this.bind(IntervalBasedTolling.class).to(IntervalBasedTollingAll.class);
				
				this.bind(IntervalBasedTollingAll.class).asEagerSingleton();
				this.bind(DelayAnalysis.class).asEagerSingleton();
				this.bind(PersonVehicleTracker.class).asEagerSingleton();
								
				this.addEventHandlerBinding().to(IntervalBasedTollingAll.class);
				this.addEventHandlerBinding().to(DelayAnalysis.class);
				this.addEventHandlerBinding().to(PersonVehicleTracker.class);
				
				this.addControlerListenerBinding().to(DecongestionControlerListener.class);

			}
		});
		
		// toll-adjusted routing
		
		final TollTimeDistanceTravelDisutilityFactory travelDisutilityFactory = new TollTimeDistanceTravelDisutilityFactory();
		travelDisutilityFactory.setSigma(0.);
		
		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
				this.bindCarTravelDisutilityFactory().toInstance( travelDisutilityFactory );
			}
		});	
		
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        controler.run();   
		
        double tt0 = controler.getLinkTravelTimes().getLinkTravelTime(scenario.getNetwork().getLinks().get(Id.createLinkId("link12")), 6 * 3600 + 50. * 60, null, null);
        double tt1 = controler.getLinkTravelTimes().getLinkTravelTime(scenario.getNetwork().getLinks().get(Id.createLinkId("link12")), 7 * 3600 + 63, null, null);
        double tt2 = controler.getLinkTravelTimes().getLinkTravelTime(scenario.getNetwork().getLinks().get(Id.createLinkId("link12")), 7 * 3600 + 15. * 60, null, null);
        
		Assert.assertEquals("Wrong travel time. The run output seems to have changed.", 100.0, tt0, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong travel time. The run output seems to have changed.", 150.5, tt1, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong travel time. The run output seems to have changed.", 100.0, tt2, MatsimTestUtils.EPSILON);
				
		final int index = config.controler().getLastIteration() - config.controler().getFirstIteration();
		double avgScore = controler.getScoreStats().getScoreHistory().get( ScoreItem.executed ).get(index);
		Assert.assertEquals("Wrong average executed score. The tolls seem to have changed.", -134.31916666666666, avgScore, MatsimTestUtils.EPSILON);
		
        System.out.println(info.getlinkInfos().get(Id.createLinkId("link12")).getTime2toll().toString());
        System.out.println(info.getlinkInfos().get(Id.createLinkId("link12")).getTime2avgDelay().toString());

		Assert.assertEquals("Wrong average delay (capacity is set in a way that one of the two agents has to wait 101 sec. Thus the average is 50.5", 50.5, info.getlinkInfos().get(Id.createLinkId("link12")).getTime2avgDelay().get(84), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong toll.", 50.5 * 2, info.getlinkInfos().get(Id.createLinkId("link12")).getTime2toll().get(84), MatsimTestUtils.EPSILON);
	}
	
	/**
	 * Kp = 2
	 * 
	 */
	@Test
	public final void test0bmodified() {
		
		System.out.println(testUtils.getPackageInputDirectory());
		
		final String configFile = testUtils.getPackageInputDirectory() + "/config0.xml";
		
		Config config = ConfigUtils.loadConfig(configFile);

		String outputDirectory = testUtils.getOutputDirectory() + "/";
		config.controler().setOutputDirectory(outputDirectory);
		
		final DecongestionConfigGroup decongestionSettings = new DecongestionConfigGroup();
		decongestionSettings.setWRITE_OUTPUT_ITERATION(1);
		decongestionSettings.setKp(2.0);
		decongestionSettings.setKd(0.0);
		decongestionSettings.setKi(0.0);
		decongestionSettings.setMsa(false);
		decongestionSettings.setTOLL_BLEND_FACTOR(1.0);
		decongestionSettings.setFRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT(1.0);
		decongestionSettings.setFRACTION_OF_ITERATIONS_TO_START_PRICE_ADJUSTMENT(0.0);
		config.addModule(decongestionSettings);
		
		final Scenario scenario = ScenarioUtils.loadScenario(config);		
		Controler controler = new Controler(scenario);
		
		// congestion toll computation
		controler.addOverridingModule(new DecongestionModule(scenario));
		
		// toll-adjusted routing
		
		final TollTimeDistanceTravelDisutilityFactory travelDisutilityFactory = new TollTimeDistanceTravelDisutilityFactory();
		travelDisutilityFactory.setSigma(0.);
		
		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
				this.bindCarTravelDisutilityFactory().toInstance( travelDisutilityFactory );
			}
		});	
		
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        controler.run();   
		
        double tt0 = controler.getLinkTravelTimes().getLinkTravelTime(scenario.getNetwork().getLinks().get(Id.createLinkId("link12")), 6 * 3600 + 50. * 60, null, null);
        double tt1 = controler.getLinkTravelTimes().getLinkTravelTime(scenario.getNetwork().getLinks().get(Id.createLinkId("link12")), 7 * 3600 + 63, null, null);
        double tt2 = controler.getLinkTravelTimes().getLinkTravelTime(scenario.getNetwork().getLinks().get(Id.createLinkId("link12")), 7 * 3600 + 15. * 60, null, null);
        
		Assert.assertEquals("Wrong travel time. The run output seems to have changed.", 100.0, tt0, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong travel time. The run output seems to have changed.", 150.5, tt1, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong travel time. The run output seems to have changed.", 100.0, tt2, MatsimTestUtils.EPSILON);
				
		final int index = config.controler().getLastIteration() - config.controler().getFirstIteration();
		double avgScore = controler.getScoreStats().getScoreHistory().get( ScoreItem.executed ).get(index);
		Assert.assertEquals("Wrong average executed score. The tolls seem to have changed.", -134.31916666666666, avgScore, MatsimTestUtils.EPSILON);
	}
	
	/**
	 * Kp = 0 / no tolling
	 * 
	 */
	@Test
	public final void test0c() {
		
		System.out.println(testUtils.getPackageInputDirectory());
		
		final String configFile = testUtils.getPackageInputDirectory() + "/config0.xml";
		
		Config config = ConfigUtils.loadConfig(configFile);

		String outputDirectory = testUtils.getOutputDirectory() + "/";
		config.controler().setOutputDirectory(outputDirectory);

		final DecongestionConfigGroup decongestionSettings = new DecongestionConfigGroup();
		decongestionSettings.setWRITE_OUTPUT_ITERATION(1);
		decongestionSettings.setKp(0.0);
		decongestionSettings.setKd(0.0);
		decongestionSettings.setKi(0.0);
		decongestionSettings.setMsa(false);
		decongestionSettings.setTOLL_BLEND_FACTOR(1.0);
		decongestionSettings.setFRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT(1.0);
		decongestionSettings.setFRACTION_OF_ITERATIONS_TO_START_PRICE_ADJUSTMENT(0.0);
		config.addModule(decongestionSettings);

		final Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		
		DecongestionInfo info = new DecongestionInfo();
		
		// congestion toll computation
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				
				this.bind(DecongestionInfo.class).toInstance(info);
				
				this.bind(DelayAnalysis.class).asEagerSingleton();
				this.addEventHandlerBinding().to(DelayAnalysis.class);
				
				this.addControlerListenerBinding().to(DecongestionControlerListener.class);

			}
		});
		
		// toll-adjusted routing
		
		final TollTimeDistanceTravelDisutilityFactory travelDisutilityFactory = new TollTimeDistanceTravelDisutilityFactory();
		travelDisutilityFactory.setSigma(0.);
		
		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
				this.bindCarTravelDisutilityFactory().toInstance( travelDisutilityFactory );
			}
		});	
		
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        controler.run();   
		
        double tt0 = controler.getLinkTravelTimes().getLinkTravelTime(scenario.getNetwork().getLinks().get(Id.createLinkId("link12")), 6 * 3600 + 50. * 60, null, null);
        double tt1 = controler.getLinkTravelTimes().getLinkTravelTime(scenario.getNetwork().getLinks().get(Id.createLinkId("link12")), 7 * 3600 + 63, null, null);
        double tt2 = controler.getLinkTravelTimes().getLinkTravelTime(scenario.getNetwork().getLinks().get(Id.createLinkId("link12")), 7 * 3600 + 15. * 60, null, null);
        
		Assert.assertEquals("Wrong travel time. The run output seems to have changed.", 100.0, tt0, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong travel time. The run output seems to have changed.", 150.5, tt1, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong travel time. The run output seems to have changed.", 100.0, tt2, MatsimTestUtils.EPSILON);
				
		final int index = config.controler().getLastIteration() - config.controler().getFirstIteration();
		double avgScore = controler.getScoreStats().getScoreHistory().get( ScoreItem.executed ).get(index);
		Assert.assertEquals("Wrong average executed score. The tolls seem to have changed.", -33.31916666666666, avgScore, MatsimTestUtils.EPSILON);
		
        System.out.println(info.getlinkInfos().get(Id.createLinkId("link12")).getTime2toll().toString());
        System.out.println(info.getlinkInfos().get(Id.createLinkId("link12")).getTime2avgDelay().toString());

		Assert.assertEquals("Wrong average delay (capacity is set in a way that one of the two agents has to wait 101 sec. Thus the average is 50.5", 50.5, info.getlinkInfos().get(Id.createLinkId("link12")).getTime2avgDelay().get(84), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong toll.", null, info.getlinkInfos().get(Id.createLinkId("link12")).getTime2toll().get(84));

	}
	
	/**
	 * Tests the PID controller
	 * 
	 */
	@Test
	public final void test1() {
		
		System.out.println(testUtils.getPackageInputDirectory());
		
		final String configFile = testUtils.getPackageInputDirectory() + "/config.xml";
		Config config = ConfigUtils.loadConfig(configFile);

		String outputDirectory = testUtils.getOutputDirectory() + "/";
		config.controler().setOutputDirectory(outputDirectory);
		
		final DecongestionConfigGroup decongestionSettings = new DecongestionConfigGroup();
		decongestionSettings.setWRITE_OUTPUT_ITERATION(1);
		decongestionSettings.setFRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT(1.0);
		decongestionSettings.setFRACTION_OF_ITERATIONS_TO_START_PRICE_ADJUSTMENT(0.0);
		decongestionSettings.setDecongestionApproach(DecongestionApproach.PID);
		config.addModule(decongestionSettings);

		final Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		
		DecongestionInfo info = new DecongestionInfo();
		
		// decongestion pricing
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				
				this.bind(DecongestionInfo.class).toInstance(info);
				
				this.bind(DecongestionTollSetting.class).to(DecongestionTollingPID.class);
				this.bind(IntervalBasedTolling.class).to(IntervalBasedTollingAll.class);
				
				this.bind(IntervalBasedTollingAll.class).asEagerSingleton();
				this.bind(DelayAnalysis.class).asEagerSingleton();
				this.bind(PersonVehicleTracker.class).asEagerSingleton();
								
				this.addEventHandlerBinding().to(IntervalBasedTollingAll.class);
				this.addEventHandlerBinding().to(DelayAnalysis.class);
				this.addEventHandlerBinding().to(PersonVehicleTracker.class);
				
				this.addControlerListenerBinding().to(DecongestionControlerListener.class);
			}
		});
		
        controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        controler.run();   
        		
		final int index = config.controler().getLastIteration() - config.controler().getFirstIteration();
		double avgScore = controler.getScoreStats().getScoreHistory().get( ScoreItem.executed ).get(index) ;
		Assert.assertEquals("Wrong average executed score. The run output seems to have changed.", -11757.488437376147, avgScore, MatsimTestUtils.EPSILON);
		
        System.out.println(info.getlinkInfos().get(Id.createLinkId("link12")).getTime2toll().toString());
		Assert.assertEquals("Wrong toll in time bin 61.", 12.695054408822529, info.getlinkInfos().get(Id.createLinkId("link12")).getTime2toll().get(61), MatsimTestUtils.EPSILON);	
		Assert.assertEquals("Wrong toll in time bin 73.", 16.77452194697956, info.getlinkInfos().get(Id.createLinkId("link12")).getTime2toll().get(73), MatsimTestUtils.EPSILON);	
	}
	
	/**
	 * Tests the BangBang controller
	 * 
	 */
	@Test
	public final void test2() {
		
		System.out.println(testUtils.getPackageInputDirectory());
		
		final String configFile = testUtils.getPackageInputDirectory() + "/config.xml";
		Config config = ConfigUtils.loadConfig(configFile);

		String outputDirectory = testUtils.getOutputDirectory() + "/";
		config.controler().setOutputDirectory(outputDirectory);

		final DecongestionConfigGroup decongestionSettings = new DecongestionConfigGroup();
		decongestionSettings.setWRITE_OUTPUT_ITERATION(1);
		decongestionSettings.setFRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT(1.0);
		decongestionSettings.setFRACTION_OF_ITERATIONS_TO_START_PRICE_ADJUSTMENT(0.0);
		config.addModule(decongestionSettings);

		DecongestionInfo info = new DecongestionInfo();
		
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);

		// decongestion pricing
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				
				this.bind(DecongestionInfo.class).toInstance(info);
				
				this.bind(DecongestionTollSetting.class).to(DecongestionTollingBangBang.class);
				this.bind(IntervalBasedTolling.class).to(IntervalBasedTollingAll.class);

				this.bind(IntervalBasedTollingAll.class).asEagerSingleton();				
				this.bind(DelayAnalysis.class).asEagerSingleton();
				this.bind(PersonVehicleTracker.class).asEagerSingleton();
								
				this.addEventHandlerBinding().to(IntervalBasedTollingAll.class);
				this.addEventHandlerBinding().to(DelayAnalysis.class);
				this.addEventHandlerBinding().to(PersonVehicleTracker.class);
				
				this.addControlerListenerBinding().to(DecongestionControlerListener.class);

			}
		});
		
        controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        controler.run();
		
		final int index = config.controler().getLastIteration() - config.controler().getFirstIteration();
		double avgScore = controler.getScoreStats().getScoreHistory().get( ScoreItem.executed ).get( index ) ;
		Assert.assertEquals("Wrong average executed score. The run output seems to have changed.", -54.97929444444, avgScore, MatsimTestUtils.EPSILON);
		
		System.out.println(info.getlinkInfos().get(Id.createLinkId("link12")).getTime2toll().toString());
		Assert.assertEquals("Wrong toll in time bin 61.", 13., info.getlinkInfos().get(Id.createLinkId("link12")).getTime2toll().get(61), MatsimTestUtils.EPSILON);	
		Assert.assertEquals("Wrong toll in time bin 73.", 13., info.getlinkInfos().get(Id.createLinkId("link12")).getTime2toll().get(73), MatsimTestUtils.EPSILON);	
	}
	
}
