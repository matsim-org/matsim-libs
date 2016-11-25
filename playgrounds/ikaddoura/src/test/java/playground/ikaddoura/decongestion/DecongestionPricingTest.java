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
package playground.ikaddoura.decongestion;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.analysis.ScoreStatsControlerListener.ScoreItem;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import playground.ikaddoura.analysis.vtts.VTTSHandler;
import playground.ikaddoura.decongestion.DecongestionConfigGroup.TollingApproach;
import playground.ikaddoura.decongestion.data.DecongestionInfo;

/**
 * 
 * Tests a router which accounts for the agent's real VTTS.
 * The VTTS is computed by {@link VTTSHandler}. 
 * 
 * @author ikaddoura
 *
 */

public class DecongestionPricingTest {
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	/**
	 * Tests the PID controller
	 * 
	 */
	@Test
	public final void test1() {
		
		System.out.println(testUtils.getPackageInputDirectory());
		
		final String configFile = testUtils.getPackageInputDirectory() + "/config.xml";
		
		final DecongestionConfigGroup decongestionSettings = new DecongestionConfigGroup();
		decongestionSettings.setTOLLING_APPROACH(TollingApproach.PID);
		decongestionSettings.setWRITE_OUTPUT_ITERATION(1);
		decongestionSettings.setFRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT(1.0);
		decongestionSettings.setFRACTION_OF_ITERATIONS_TO_START_PRICE_ADJUSTMENT(0.0);
		
		Config config = ConfigUtils.loadConfig(configFile);

		String outputDirectory = testUtils.getOutputDirectory() + "/";
		config.controler().setOutputDirectory(outputDirectory);
		final Scenario scenario = ScenarioUtils.loadScenario(config);
				
		final DecongestionInfo info = new DecongestionInfo(scenario, decongestionSettings);
		final Decongestion decongestion = new Decongestion(info);
		
		final Controler controler = decongestion.getControler();
        controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        controler.run();   
		
        double tt1 = controler.getLinkTravelTimes().getLinkTravelTime(scenario.getNetwork().getLinks().get(Id.createLinkId("link12")), 7 * 3600 + 63, null, null);
        double tt2 = controler.getLinkTravelTimes().getLinkTravelTime(scenario.getNetwork().getLinks().get(Id.createLinkId("link12")), 7 * 3600 + 35 * 60 + 5., null, null);
        double tt3 = controler.getLinkTravelTimes().getLinkTravelTime(scenario.getNetwork().getLinks().get(Id.createLinkId("link12")), 7 * 3600 + 35 * 60 + 10., null, null);

		Assert.assertEquals("Wrong travel time. The run output seems to have changed.", 2646.72285378283, tt1, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong travel time. The run output seems to have changed.", 3835.328125, tt2, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong travel time. The run output seems to have changed.", 3835.328125, tt3, MatsimTestUtils.EPSILON);
		
		final int index = config.controler().getLastIteration() - config.controler().getFirstIteration();
//		double avgScore = controler.getScoreStats().getScoreHistory()[3][index];
		double avgScore = controler.getScoreStats().getScoreHistory().get( ScoreItem.executed ).get(index) ;
		Assert.assertEquals("Wrong average executed score. The run output seems to have changed.", -11757.488437376147, avgScore, MatsimTestUtils.EPSILON);		
	}
	
	/**
	 * Tests the BangBang controller
	 * 
	 */
	@Test
	public final void test2() {
		
		System.out.println(testUtils.getPackageInputDirectory());
		
		final String configFile = testUtils.getPackageInputDirectory() + "/config.xml";
		
		final DecongestionConfigGroup decongestionSettings = new DecongestionConfigGroup();
		decongestionSettings.setTOLLING_APPROACH(TollingApproach.BangBang);
		decongestionSettings.setWRITE_OUTPUT_ITERATION(1);
		decongestionSettings.setFRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT(1.0);
		decongestionSettings.setFRACTION_OF_ITERATIONS_TO_START_PRICE_ADJUSTMENT(0.0);
		
		Config config = ConfigUtils.loadConfig(configFile);

		String outputDirectory = testUtils.getOutputDirectory() + "/";
		config.controler().setOutputDirectory(outputDirectory);
		final Scenario scenario = ScenarioUtils.loadScenario(config);
				
		final DecongestionInfo info = new DecongestionInfo(scenario, decongestionSettings);
		final Decongestion decongestion = new Decongestion(info);
		
		final Controler controler = decongestion.getControler();
        controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        controler.run();   
		
        double tt1 = controler.getLinkTravelTimes().getLinkTravelTime(scenario.getNetwork().getLinks().get(Id.createLinkId("link12")), 7 * 3600 + 63, null, null);
        double tt2 = controler.getLinkTravelTimes().getLinkTravelTime(scenario.getNetwork().getLinks().get(Id.createLinkId("link12")), 7 * 3600 + 35 * 60 + 5., null, null);
        double tt3 = controler.getLinkTravelTimes().getLinkTravelTime(scenario.getNetwork().getLinks().get(Id.createLinkId("link12")), 7 * 3600 + 35 * 60 + 10., null, null);

		Assert.assertEquals("Wrong travel time. The run output seems to have changed.", 3429.57425742574, tt1, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong travel time. The run output seems to have changed.", 5343.2, tt2, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong travel time. The run output seems to have changed.", 5343.2, tt3, MatsimTestUtils.EPSILON);
		
		final int index = config.controler().getLastIteration() - config.controler().getFirstIteration();
//		double avgScore = controler.getScoreStats().getScoreHistory()[3][index];
		double avgScore = controler.getScoreStats().getScoreHistory().get( ScoreItem.executed ).get( index ) ;
		Assert.assertEquals("Wrong average executed score. The run output seems to have changed.", -55.08439467592601, avgScore, MatsimTestUtils.EPSILON);		
	}
	
}
