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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.analysis.ScoreStatsControlerListener.ScoreItem;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.decongestion.DecongestionConfigGroup.DecongestionApproach;
import org.matsim.contrib.decongestion.data.DecongestionInfo;
import org.matsim.contrib.decongestion.data.LinkInfo;
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
public class DecongestionPricingTestIT {

	@RegisterExtension
	private MatsimTestUtils testUtils = new MatsimTestUtils();

	/**
	 * Kp = 0.0123
	 *
	 */
	@Test
	final void test0a() {

		System.out.println(testUtils.getPackageInputDirectory());

		final String configFile = testUtils.getPackageInputDirectory() + "/config0.xml";

		Config config = ConfigUtils.loadConfig(configFile);

		String outputDirectory = testUtils.getOutputDirectory() + "/";
		config.controller().setOutputDirectory(outputDirectory);

		final DecongestionConfigGroup decongestionSettings = new DecongestionConfigGroup();
		decongestionSettings.setWriteOutputIteration(1);
		decongestionSettings.setKp(0.0123);
		decongestionSettings.setKd(0.0);
		decongestionSettings.setKi(0.0);
		decongestionSettings.setMsa(false);
		decongestionSettings.setTollBlendFactor(1.0);
		decongestionSettings.setFractionOfIterationsToEndPriceAdjustment(1.0);
		decongestionSettings.setFractionOfIterationsToStartPriceAdjustment(0.0);
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

		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
				addTravelDisutilityFactoryBinding( TransportMode.car ).toInstance( new TollTimeDistanceTravelDisutilityFactory() );
			}
		});

		controler.run();

		double tt0 = controler.getLinkTravelTimes().getLinkTravelTime(scenario.getNetwork().getLinks().get(Id.createLinkId("link12")), 6 * 3600 + 50. * 60, null, null);
		double tt1 = controler.getLinkTravelTimes().getLinkTravelTime(scenario.getNetwork().getLinks().get(Id.createLinkId("link12")), 7 * 3600 + 63, null, null);
		double tt2 = controler.getLinkTravelTimes().getLinkTravelTime(scenario.getNetwork().getLinks().get(Id.createLinkId("link12")), 7 * 3600 + 15. * 60, null, null);

		Assertions.assertEquals(100.0, tt0, MatsimTestUtils.EPSILON, "Wrong travel time. The run output seems to have changed.");
		Assertions.assertEquals(150.5, tt1, MatsimTestUtils.EPSILON, "Wrong travel time. The run output seems to have changed.");
		Assertions.assertEquals(100.0, tt2, MatsimTestUtils.EPSILON, "Wrong travel time. The run output seems to have changed.");

		final int index = config.controller().getLastIteration() - config.controller().getFirstIteration();
		double avgScore = controler.getScoreStats().getScoreHistory().get( ScoreItem.executed ).get(index);
		Assertions.assertEquals(-33.940316666666666, avgScore, MatsimTestUtils.EPSILON, "Wrong average executed score. The tolls seem to have changed.");

		System.out.println(info.getlinkInfos().get(Id.createLinkId("link12")).getTime2toll().toString());
		System.out.println(info.getlinkInfos().get(Id.createLinkId("link12")).getTime2avgDelay().toString());

		Assertions.assertEquals(50.5, info.getlinkInfos().get(Id.createLinkId("link12")).getTime2avgDelay().get(84), MatsimTestUtils.EPSILON, "Wrong average delay (capacity is set in a way that one of the two agents has to wait 101 sec. Thus the average is 50.5");
		Assertions.assertEquals(50.5 * 0.0123, info.getlinkInfos().get(Id.createLinkId("link12")).getTime2toll().get(84), MatsimTestUtils.EPSILON, "Wrong toll.");

	}

	/**
	 * Kp = 0.0123, other syntax
	 *
	 */
	@Test
	final void test0amodified() {

		System.out.println(testUtils.getPackageInputDirectory());

		// ---

		Config config = ConfigUtils.loadConfig( testUtils.getPackageInputDirectory() + "/config0.xml" );

		config.controller().setOutputDirectory( testUtils.getOutputDirectory()  );

		final DecongestionConfigGroup decongestionSettings = ConfigUtils.addOrGetModule( config, DecongestionConfigGroup.class );

		decongestionSettings.setWriteOutputIteration(1);
		decongestionSettings.setKp(0.0123);
		decongestionSettings.setKd(0.0);
		decongestionSettings.setKi(0.0);
		decongestionSettings.setMsa(false);
		decongestionSettings.setTollBlendFactor(1.0);
		decongestionSettings.setFractionOfIterationsToEndPriceAdjustment(1.0);
		decongestionSettings.setFractionOfIterationsToStartPriceAdjustment(0.0);

		// ---

		final Scenario scenario = ScenarioUtils.loadScenario(config);

		// ---

		Controler controler = new Controler(scenario);

		// congestion toll computation
		controler.addOverridingModule(new DecongestionModule() );

		// toll-adjusted routing

		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
				addTravelDisutilityFactoryBinding( TransportMode.car ).toInstance( new TollTimeDistanceTravelDisutilityFactory() );
			}
		});

		controler.run();

		// ---

		double tt0 = controler.getLinkTravelTimes().getLinkTravelTime(scenario.getNetwork().getLinks().get(Id.createLinkId("link12")), 6 * 3600 + 50. * 60, null, null);
		double tt1 = controler.getLinkTravelTimes().getLinkTravelTime(scenario.getNetwork().getLinks().get(Id.createLinkId("link12")), 7 * 3600 + 63, null, null);
		double tt2 = controler.getLinkTravelTimes().getLinkTravelTime(scenario.getNetwork().getLinks().get(Id.createLinkId("link12")), 7 * 3600 + 15. * 60, null, null);

		Assertions.assertEquals(100.0, tt0, MatsimTestUtils.EPSILON, "Wrong travel time. The run output seems to have changed.");
		Assertions.assertEquals(150.5, tt1, MatsimTestUtils.EPSILON, "Wrong travel time. The run output seems to have changed.");
		Assertions.assertEquals(100.0, tt2, MatsimTestUtils.EPSILON, "Wrong travel time. The run output seems to have changed.");

		final int index = config.controller().getLastIteration() - config.controller().getFirstIteration();
		double avgScore = controler.getScoreStats().getScoreHistory().get( ScoreItem.executed ).get(index);
		Assertions.assertEquals(-33.940316666666666, avgScore, MatsimTestUtils.EPSILON, "Wrong average executed score. The tolls seem to have changed.");
	}

	/**
	 * Kp = 0.0123, other syntax, kn
	 *
	 */
	@Test
	final void test0amodifiedKn() {

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

	/**
	 * Kp = 2
	 *
	 */
	@Test
	final void test0b() {

		System.out.println(testUtils.getPackageInputDirectory());

		final String configFile = testUtils.getPackageInputDirectory() + "/config0.xml";

		Config config = ConfigUtils.loadConfig(configFile);

		String outputDirectory = testUtils.getOutputDirectory() + "/";
		config.controller().setOutputDirectory(outputDirectory);

		final DecongestionConfigGroup decongestionSettings = new DecongestionConfigGroup();
		decongestionSettings.setWriteOutputIteration(1);
		decongestionSettings.setKp(2.0);
		decongestionSettings.setKd(0.0);
		decongestionSettings.setKi(0.0);
		decongestionSettings.setMsa(false);
		decongestionSettings.setTollBlendFactor(1.0);
		decongestionSettings.setFractionOfIterationsToEndPriceAdjustment(1.0);
		decongestionSettings.setFractionOfIterationsToStartPriceAdjustment(0.0);
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

		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
				addTravelDisutilityFactoryBinding( TransportMode.car ).toInstance( travelDisutilityFactory );
			}
		});

		controler.getConfig().controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler.run();

		// ===

		double tt0 = controler.getLinkTravelTimes().getLinkTravelTime(scenario.getNetwork().getLinks().get(Id.createLinkId("link12")), 6 * 3600 + 50. * 60, null, null);
		double tt1 = controler.getLinkTravelTimes().getLinkTravelTime(scenario.getNetwork().getLinks().get(Id.createLinkId("link12")), 7 * 3600 + 63, null, null);
		double tt2 = controler.getLinkTravelTimes().getLinkTravelTime(scenario.getNetwork().getLinks().get(Id.createLinkId("link12")), 7 * 3600 + 15. * 60, null, null);

		Assertions.assertEquals(100.0, tt0, MatsimTestUtils.EPSILON, "Wrong travel time. The run output seems to have changed.");
		Assertions.assertEquals(150.5, tt1, MatsimTestUtils.EPSILON, "Wrong travel time. The run output seems to have changed.");
		Assertions.assertEquals(100.0, tt2, MatsimTestUtils.EPSILON, "Wrong travel time. The run output seems to have changed.");

		final int index = config.controller().getLastIteration() - config.controller().getFirstIteration();
		double avgScore = controler.getScoreStats().getScoreHistory().get( ScoreItem.executed ).get(index);
		Assertions.assertEquals(-134.31916666666666, avgScore, MatsimTestUtils.EPSILON, "Wrong average executed score. The tolls seem to have changed.");

		System.out.println(info.getlinkInfos().get(Id.createLinkId("link12")).getTime2toll().toString());
		System.out.println(info.getlinkInfos().get(Id.createLinkId("link12")).getTime2avgDelay().toString());

		Assertions.assertEquals(50.5, info.getlinkInfos().get(Id.createLinkId("link12")).getTime2avgDelay().get(84), MatsimTestUtils.EPSILON, "Wrong average delay (capacity is set in a way that one of the two agents has to wait 101 sec. Thus the average is 50.5");
		Assertions.assertEquals(50.5 * 2, info.getlinkInfos().get(Id.createLinkId("link12")).getTime2toll().get(84), MatsimTestUtils.EPSILON, "Wrong toll.");
	}

	/**
	 * Kp = 2
	 *
	 */
	@Test
	final void test0bmodified() {

		System.out.println(testUtils.getPackageInputDirectory());

		final String configFile = testUtils.getPackageInputDirectory() + "/config0.xml";

		Config config = ConfigUtils.loadConfig(configFile);

		String outputDirectory = testUtils.getOutputDirectory() + "/";
		config.controller().setOutputDirectory(outputDirectory);

		final DecongestionConfigGroup decongestionSettings = new DecongestionConfigGroup();
		decongestionSettings.setWriteOutputIteration(1);
		decongestionSettings.setKp(2.0);
		decongestionSettings.setKd(0.0);
		decongestionSettings.setKi(0.0);
		decongestionSettings.setMsa(false);
		decongestionSettings.setTollBlendFactor(1.0);
		decongestionSettings.setFractionOfIterationsToEndPriceAdjustment(1.0);
		decongestionSettings.setFractionOfIterationsToStartPriceAdjustment(0.0);
		config.addModule(decongestionSettings);

		final Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);

		// congestion toll computation
		controler.addOverridingModule(new DecongestionModule() );

		// toll-adjusted routing

		final TollTimeDistanceTravelDisutilityFactory travelDisutilityFactory = new TollTimeDistanceTravelDisutilityFactory();

		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
				addTravelDisutilityFactoryBinding( TransportMode.car ).toInstance( travelDisutilityFactory );
			}
		});

		controler.getConfig().controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler.run();

		double tt0 = controler.getLinkTravelTimes().getLinkTravelTime(scenario.getNetwork().getLinks().get(Id.createLinkId("link12")), 6 * 3600 + 50. * 60, null, null);
		double tt1 = controler.getLinkTravelTimes().getLinkTravelTime(scenario.getNetwork().getLinks().get(Id.createLinkId("link12")), 7 * 3600 + 63, null, null);
		double tt2 = controler.getLinkTravelTimes().getLinkTravelTime(scenario.getNetwork().getLinks().get(Id.createLinkId("link12")), 7 * 3600 + 15. * 60, null, null);

		Assertions.assertEquals(100.0, tt0, MatsimTestUtils.EPSILON, "Wrong travel time. The run output seems to have changed.");
		Assertions.assertEquals(150.5, tt1, MatsimTestUtils.EPSILON, "Wrong travel time. The run output seems to have changed.");
		Assertions.assertEquals(100.0, tt2, MatsimTestUtils.EPSILON, "Wrong travel time. The run output seems to have changed.");

		final int index = config.controller().getLastIteration() - config.controller().getFirstIteration();
		double avgScore = controler.getScoreStats().getScoreHistory().get( ScoreItem.executed ).get(index);
		Assertions.assertEquals(-134.31916666666666, avgScore, MatsimTestUtils.EPSILON, "Wrong average executed score. The tolls seem to have changed.");
	}

	/**
	 * Kp = 0 / no tolling
	 *
	 */
	@Test
	final void test0c() {

		System.out.println(testUtils.getPackageInputDirectory());

		final String configFile = testUtils.getPackageInputDirectory() + "/config0.xml";

		Config config = ConfigUtils.loadConfig(configFile);

		String outputDirectory = testUtils.getOutputDirectory() + "/";
		config.controller().setOutputDirectory(outputDirectory);

		final DecongestionConfigGroup decongestionSettings = new DecongestionConfigGroup();
		decongestionSettings.setWriteOutputIteration(1);
		decongestionSettings.setKp(0.0);
		decongestionSettings.setKd(0.0);
		decongestionSettings.setKi(0.0);
		decongestionSettings.setMsa(false);
		decongestionSettings.setTollBlendFactor(1.0);
		decongestionSettings.setFractionOfIterationsToEndPriceAdjustment(1.0);
		decongestionSettings.setFractionOfIterationsToStartPriceAdjustment(0.0);
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

		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
				this.bindCarTravelDisutilityFactory().toInstance( travelDisutilityFactory );
			}
		});

		controler.getConfig().controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler.run();

		double tt0 = controler.getLinkTravelTimes().getLinkTravelTime(scenario.getNetwork().getLinks().get(Id.createLinkId("link12")), 6 * 3600 + 50. * 60, null, null);
		double tt1 = controler.getLinkTravelTimes().getLinkTravelTime(scenario.getNetwork().getLinks().get(Id.createLinkId("link12")), 7 * 3600 + 63, null, null);
		double tt2 = controler.getLinkTravelTimes().getLinkTravelTime(scenario.getNetwork().getLinks().get(Id.createLinkId("link12")), 7 * 3600 + 15. * 60, null, null);

		Assertions.assertEquals(100.0, tt0, MatsimTestUtils.EPSILON, "Wrong travel time. The run output seems to have changed.");
		Assertions.assertEquals(150.5, tt1, MatsimTestUtils.EPSILON, "Wrong travel time. The run output seems to have changed.");
		Assertions.assertEquals(100.0, tt2, MatsimTestUtils.EPSILON, "Wrong travel time. The run output seems to have changed.");

		final int index = config.controller().getLastIteration() - config.controller().getFirstIteration();
		double avgScore = controler.getScoreStats().getScoreHistory().get( ScoreItem.executed ).get(index);
		Assertions.assertEquals(-33.31916666666666, avgScore, MatsimTestUtils.EPSILON, "Wrong average executed score. The tolls seem to have changed.");

		System.out.println(info.getlinkInfos().get(Id.createLinkId("link12")).getTime2toll().toString());
		System.out.println(info.getlinkInfos().get(Id.createLinkId("link12")).getTime2avgDelay().toString());

		Assertions.assertEquals(50.5, info.getlinkInfos().get(Id.createLinkId("link12")).getTime2avgDelay().get(84), MatsimTestUtils.EPSILON, "Wrong average delay (capacity is set in a way that one of the two agents has to wait 101 sec. Thus the average is 50.5");
		Assertions.assertNull(info.getlinkInfos().get(Id.createLinkId("link12")).getTime2toll().get(84), "Wrong toll.");

	}

	/**
	 * Tests the PID controller
	 *
	 */
	@Test
	final void test1() {

		System.out.println(testUtils.getPackageInputDirectory());

		final String configFile = testUtils.getPackageInputDirectory() + "/config.xml";
		Config config = ConfigUtils.loadConfig(configFile);

		String outputDirectory = testUtils.getOutputDirectory() + "/";
		config.controller().setOutputDirectory(outputDirectory);

		final DecongestionConfigGroup decongestionSettings = new DecongestionConfigGroup();
		decongestionSettings.setWriteOutputIteration(1);
		decongestionSettings.setFractionOfIterationsToEndPriceAdjustment(1.0);
		decongestionSettings.setFractionOfIterationsToStartPriceAdjustment(0.0);
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

		controler.getConfig().controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler.run();

		final int index = config.controller().getLastIteration() - config.controller().getFirstIteration();
		double avgScore = controler.getScoreStats().getScoreHistory().get( ScoreItem.executed ).get(index) ;
		Assertions.assertEquals(-12036.177448472225, avgScore, MatsimTestUtils.EPSILON, "Wrong average executed score. The run output seems to have changed.");

		System.out.println(info.getlinkInfos().get(Id.createLinkId("link12")).getTime2toll().toString());
		Assertions.assertEquals(9.197000000000003, info.getlinkInfos().get(Id.createLinkId("link12")).getTime2toll().get(61), MatsimTestUtils.EPSILON, "Wrong toll in time bin 61.");
		Assertions.assertEquals(12.963999999999984, info.getlinkInfos().get(Id.createLinkId("link12")).getTime2toll().get(73), MatsimTestUtils.EPSILON, "Wrong toll in time bin 73.");
	}

	/**
	 * Tests the BangBang controller
	 *
	 */
	@Test
	final void test2() {

		System.out.println(testUtils.getPackageInputDirectory());

		Config config = ConfigUtils.loadConfig( testUtils.getPackageInputDirectory() + "/config.xml" );

		config.controller().setOutputDirectory( testUtils.getOutputDirectory() );

		final DecongestionConfigGroup decongestionSettings = new DecongestionConfigGroup();
		decongestionSettings.setWriteOutputIteration(1);
		decongestionSettings.setFractionOfIterationsToEndPriceAdjustment(1.0);
		decongestionSettings.setFractionOfIterationsToStartPriceAdjustment(0.0);
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

		controler.run();

		// ---

		final int index = config.controller().getLastIteration() - config.controller().getFirstIteration();
		double avgScore = controler.getScoreStats().getScoreHistory().get( ScoreItem.executed ).get( index ) ;
		Assertions.assertEquals(-55.215645833333184, avgScore, MatsimTestUtils.EPSILON, "Wrong average executed score. The run output seems to have changed.");

		System.out.println(info.getlinkInfos().get(Id.createLinkId("link12")).getTime2toll().toString());
		Assertions.assertEquals(13., info.getlinkInfos().get(Id.createLinkId("link12")).getTime2toll().get(61), MatsimTestUtils.EPSILON, "Wrong toll in time bin 61.");
		Assertions.assertEquals(13., info.getlinkInfos().get(Id.createLinkId("link12")).getTime2toll().get(73), MatsimTestUtils.EPSILON, "Wrong toll in time bin 73.");
	}

}
