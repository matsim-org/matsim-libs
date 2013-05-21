/* *********************************************************************** *
 * project: org.matsim.*
 * CadytsIntegrationTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.contrib.cadyts.car;

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.Module;
import org.matsim.core.config.groups.ControlerConfigGroup.MobsimType;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import cadyts.measurements.SingleLinkMeasurement;

public class CadytsCarIntegrationTest {
	
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public final void testInitialization() {
		String inputDir = this.utils.getClassInputDirectory();

		Config config = createTestConfig(inputDir, this.utils.getOutputDirectory());
		config.controler().setLastIteration(0);
		StrategySettings stratSets = new StrategySettings(new IdImpl(1));
		stratSets.setModuleName("ccc") ;
		stratSets.setProbability(1.) ;
		config.strategy().addStrategySettings(stratSets) ;
		
		Scenario scenario = ScenarioUtils.loadScenario(config) ;
		final Controler controler = new Controler(scenario);
		final CadytsContext context = new CadytsContext( config ) ;
		controler.addControlerListener(context) ;
		controler.setOverwriteFiles(true);
		controler.addPlanStrategyFactory("ccc", new PlanStrategyFactory() {
			@Override
			public PlanStrategy createPlanStrategy(Scenario scenario2, EventsManager events2) {
				// return new PlanStrategyImpl(new CadytsPtPlanChanger(scenario2, context));
				return new PlanStrategyImpl(new CadytsPlanChanger(context));
			}} ) ;
		
		controler.setCreateGraphs(false);
		controler.getConfig().controler().setWriteEventsInterval(0);
		controler.setDumpDataAtEnd(true);
		controler.setMobsimFactory(new DummyMobsimFactory());
		controler.run();
		
		//test calibration settings
		// Assert.assertEquals(true, context.getCalibrator().getBruteForce());
		Assert.assertEquals(false, context.getCalibrator().getBruteForce());
		Assert.assertEquals(false, context.getCalibrator().getCenterRegression());
		Assert.assertEquals(Integer.MAX_VALUE, context.getCalibrator().getFreezeIteration());
		// Assert.assertEquals(8.0, context.getCalibrator().getMinStddev(SingleLinkMeasurement.TYPE.FLOW_VEH_H), MatsimTestUtils.EPSILON);
		Assert.assertEquals(25.0, context.getCalibrator().getMinStddev(SingleLinkMeasurement.TYPE.FLOW_VEH_H), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, context.getCalibrator().getPreparatoryIterations());
		Assert.assertEquals(0.95, context.getCalibrator().getRegressionInertia(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1.0, context.getCalibrator().getVarianceScale(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(3600.0, context.getCalibrator().getTimeBinSize_s(), MatsimTestUtils.EPSILON);

	}



	private static Config createTestConfig(String inputDir, String outputDir) {
		Config config = ConfigUtils.createConfig() ;
		// ---
		config.global().setRandomSeed(4711) ;
		// ---
		config.network().setInputFile(inputDir + "network.xml") ;
		// ---
		// config.plans().setInputFile(inputDir + "4plans.xml") ;
		config.plans().setInputFile(inputDir + "300.plans.xml.gz") ;
		// ---
		// config.scenario().setUseTransit(true) ;
		// config.scenario().setUseVehicles(true);
		// ---
		config.controler().setFirstIteration(1) ;
		config.controler().setLastIteration(10) ;
		config.controler().setOutputDirectory(outputDir) ;
		config.controler().setWriteEventsInterval(1) ;
		config.controler().setMobsim(MobsimType.qsim.toString()) ;
		// ---
		QSimConfigGroup qsimConfigGroup = new QSimConfigGroup() ;
		config.addQSimConfigGroup(qsimConfigGroup) ;
		
		config.getQSimConfigGroup().setFlowCapFactor(0.02) ;
		config.getQSimConfigGroup().setStorageCapFactor(0.06) ;
		config.getQSimConfigGroup().setStuckTime(10.) ;
		config.getQSimConfigGroup().setRemoveStuckVehicles(false) ; // ??
		// ---
//		config.transit().setTransitScheduleFile(inputDir + "transitSchedule1bus.xml") ;
//		config.transit().setVehiclesFile(inputDir + "vehicles.xml") ;
		Set<String> modes = new HashSet<String>() ;
		// modes.add("pt") ;
		modes.add("car");
		config.transit().setTransitModes(modes) ;
		// ---
		{
			ActivityParams params = new ActivityParams("h") ;
			config.planCalcScore().addActivityParams(params ) ;
			params.setTypicalDuration(12*60*60.) ;
		}{
			ActivityParams params = new ActivityParams("w") ;
			config.planCalcScore().addActivityParams(params ) ;
			params.setTypicalDuration(8*60*60.) ;
		}
		// ---		
		
//		Module cadytsCarConfig = config.createModule(CadytsCarConfigGroup.GROUP_NAME ) ;
//		
//		cadytsCarConfig.addParam(CadytsCarConfigGroup.START_TIME, "04:00:00") ;
//		cadytsCarConfig.addParam(CadytsCarConfigGroup.END_TIME, "20:00:00" ) ;
//		cadytsCarConfig.addParam(CadytsCarConfigGroup.REGRESSION_INERTIA, "0.95") ;
//		cadytsCarConfig.addParam(CadytsCarConfigGroup.USE_BRUTE_FORCE, "true") ;
//		cadytsCarConfig.addParam(CadytsCarConfigGroup.MIN_FLOW_STDDEV, "8") ;
//		cadytsCarConfig.addParam(CadytsCarConfigGroup.PREPARATORY_ITERATIONS, "1") ;
//		// cadytsCarConfig.addParam(CadytsCarConfigGroup.TIME_BIN_SIZE, "3600") ;
//		// cadytsCarConfig.addParam(CadytsCarConfigGroup.CALIBRATED_LINES, "M44,M43") ;
//		
//		CadytsCarConfigGroup ccc = new CadytsCarConfigGroup() ;
//		config.addModule(CadytsCarConfigGroup.GROUP_NAME, ccc) ;
		
		
		// ---
//		config.ptCounts().setOccupancyCountsFileName(inputDir + "counts/counts_occupancy.xml") ;
//		config.ptCounts().setBoardCountsFileName(inputDir + "counts/counts_boarding.xml") ;
//		config.ptCounts().setAlightCountsFileName(inputDir + "counts/counts_alighting.xml") ;
//		config.ptCounts().setDistanceFilter(30000.) ; // why?
//		config.ptCounts().setDistanceFilterCenterNode("7") ; // why?
//		config.ptCounts().setOutputFormat("txt");
//		config.ptCounts().setCountsScaleFactor(1.) ;
		
		config.counts().setCountsFileName(inputDir + "counts/counts-5_-0.5.xml");
		// ---
		return config;
	}

	
	private static class DummyMobsim implements Mobsim {
		public DummyMobsim() {
		}
		@Override
		public void run() {
		}
	}

	private static class DummyMobsimFactory implements MobsimFactory {
		@Override
		public Mobsim createMobsim(final Scenario sc, final EventsManager eventsManager) {
			return new DummyMobsim();
		}
	}

}
