/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package org.matsim.contrib.signals.controller.laemmerFix;

import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.analysis.SignalAnalysisTool;
import org.matsim.contrib.signals.controller.laemmerFix.LaemmerConfig.Regime;
import org.matsim.contrib.signals.controller.laemmerFix.LaemmerConfig.StabilizationStrategy;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestUtils;

import analysis.TtGeneralAnalysis;
import scenarios.illustrative.singleCrossing.SingleCrossingScenario;
import utils.ModifyPopulation;

/**
 * @author tthunig
 *
 */
public class LaemmerIT {

	private static final Logger log = Logger.getLogger(LaemmerIT.class);

	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	private final int maxCycleTime = 90;
	private final int cycleIntergreenTime = 10;
	
	/**
	 * single intersection with demand (equals flow capacity) only in NS-direction. signals should show green only for the NS-direction.
	 */
	@Test
	public void testSingleCrossingScenarioDemandNS() {
		SignalAnalysisTool signalAnalyzer = new SignalAnalysisTool();
		TtGeneralAnalysis generalAnalyzer = runSingleCrossingScenario(1800, 0, true, signalAnalyzer, Regime.COMBINED);

		Map<Id<SignalGroup>, Double> totalSignalGreenTimes = signalAnalyzer.getTotalSignalGreenTime(); // NS should show a lot more green than WE
		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycle = signalAnalyzer.calculateAvgSignalGreenTimePerFlexibleCycle(); // WE should be almost 0
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystem = signalAnalyzer.calculateAvgFlexibleCycleTimePerSignalSystem(); // should be around 60, at most 90
		Map<Id<Link>, Double> avgDelayPerLink = generalAnalyzer.getAvgDelayPerLink();
		Double avgDelayWE = avgDelayPerLink.get(Id.createLinkId("2_3"));
		Double avgDelayNS = avgDelayPerLink.get(Id.createLinkId("7_3"));
		
		log.info("total signal green times: " + totalSignalGreenTimes.get(SingleCrossingScenario.signalGroupId1) + ", "
				+ totalSignalGreenTimes.get(SingleCrossingScenario.signalGroupId2));
		log.info("avg signal green times per cycle: " + avgSignalGreenTimePerCycle.get(SingleCrossingScenario.signalGroupId1) + ", "
				+ avgSignalGreenTimePerCycle.get(SingleCrossingScenario.signalGroupId2));
		log.info("avg cycle time per system: " + avgCycleTimePerSystem.get(SingleCrossingScenario.signalSystemId));
		log.info("avg delay per link: " + avgDelayWE + ", " + avgDelayNS);
		
		Assert.assertNull("signal group 1 should show no green", avgSignalGreenTimePerCycle.get(SingleCrossingScenario.signalGroupId1));
		Assert.assertNotNull("signal group 2 should show green", avgSignalGreenTimePerCycle.get(SingleCrossingScenario.signalGroupId2));
		Assert.assertEquals("avg cycle time of the system and total green time of NS-group should be equal", totalSignalGreenTimes.get(SingleCrossingScenario.signalGroupId2), avgCycleTimePerSystem.get(SingleCrossingScenario.signalSystemId), MatsimTestUtils.EPSILON);
		Assert.assertEquals("avg delay at NS-direction should be zero", 0.0, avgDelayNS, MatsimTestUtils.EPSILON);
	}
	
	/**
	 * Test minimum green time:
	 * single intersection with high demand in WE-direction, very low demand in NS-direction but minimum green time. I.e. the NS-signal should show green for exactly this 5 seconds per cycle.
	 */
	@Test
	public void testSingleCrossingScenarioLowVsHighDemandWithMinG(){
		SignalAnalysisTool signalAnalyzer = new SignalAnalysisTool();
		TtGeneralAnalysis generalAnalyzer = runSingleCrossingScenario(90, 1800, true, signalAnalyzer, Regime.COMBINED);
		
		Map<Id<SignalGroup>, Double> totalSignalGreenTimes = signalAnalyzer.getTotalSignalGreenTime(); 
		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycle = signalAnalyzer.calculateAvgSignalGreenTimePerFlexibleCycle(); 
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystem = signalAnalyzer.calculateAvgFlexibleCycleTimePerSignalSystem(); 
		Map<Id<Link>, Double> avgDelayPerLink = generalAnalyzer.getAvgDelayPerLink();
		Double avgDelayWE = avgDelayPerLink.get(Id.createLinkId("2_3"));
		Double avgDelayNS = avgDelayPerLink.get(Id.createLinkId("7_3"));
		
		log.info("total signal green times: " + totalSignalGreenTimes.get(SingleCrossingScenario.signalGroupId1) + ", " + totalSignalGreenTimes.get(SingleCrossingScenario.signalGroupId2));
		log.info("avg signal green times per cycle: " + avgSignalGreenTimePerCycle.get(SingleCrossingScenario.signalGroupId1) + ", "
				+ avgSignalGreenTimePerCycle.get(SingleCrossingScenario.signalGroupId2));
		log.info("avg cycle time per system: " + avgCycleTimePerSystem.get(SingleCrossingScenario.signalSystemId));
		log.info("avg delay per link: " + avgDelayWE + ", " + avgDelayNS);
		
		Assert.assertTrue("total signal green time of WE-direction should be higher than NS-direction", totalSignalGreenTimes.get(SingleCrossingScenario.signalGroupId1)-totalSignalGreenTimes.get(SingleCrossingScenario.signalGroupId2) > 0);
		Assert.assertTrue("avg signal green time per cycle of WE-direction should be higher than NS-direction", avgSignalGreenTimePerCycle.get(SingleCrossingScenario.signalGroupId1)-avgSignalGreenTimePerCycle.get(SingleCrossingScenario.signalGroupId2) > 0);
		Assert.assertEquals("avg signal green time per cycle of NS-direction should be the minimum green time of 5 seconds", 5.0, avgSignalGreenTimePerCycle.get(SingleCrossingScenario.signalGroupId2), MatsimTestUtils.EPSILON);
		Assert.assertTrue("cycle time should stay below 90 seconds", avgCycleTimePerSystem.get(SingleCrossingScenario.signalSystemId) <= 90);
		Assert.assertTrue("avg delay per vehicle on WS-direction should be less than on NS-direction", avgDelayWE<avgDelayNS);
	}
	
	/**
	 * counterpart to minimum green time test above.
	 * single intersection with high demand in WE-direction, very low demand in NS-direction. No minimum green time! I.e. the NS-signal should show green for less than 5 seconds per cycle.
	 */
	@Test
	public void testSingleCrossingScenarioLowVsHighDemandWoMinG(){
		SignalAnalysisTool signalAnalyzer = new SignalAnalysisTool();
		TtGeneralAnalysis generalAnalyzer = runSingleCrossingScenario(90, 1800, false, signalAnalyzer, Regime.COMBINED);
		
		Map<Id<SignalGroup>, Double> totalSignalGreenTimes = signalAnalyzer.getTotalSignalGreenTime(); 
		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycle = signalAnalyzer.calculateAvgSignalGreenTimePerFlexibleCycle(); 
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystem = signalAnalyzer.calculateAvgFlexibleCycleTimePerSignalSystem(); 
		Map<Id<Link>, Double> avgDelayPerLink = generalAnalyzer.getAvgDelayPerLink();
		Double avgDelayWE = avgDelayPerLink.get(Id.createLinkId("2_3"));
		Double avgDelayNS = avgDelayPerLink.get(Id.createLinkId("7_3"));
		
		log.info("total signal green times: " + totalSignalGreenTimes.get(SingleCrossingScenario.signalGroupId1) + ", " + totalSignalGreenTimes.get(SingleCrossingScenario.signalGroupId2));
		log.info("avg signal green times per cycle: " + avgSignalGreenTimePerCycle.get(SingleCrossingScenario.signalGroupId1) + ", "
				+ avgSignalGreenTimePerCycle.get(SingleCrossingScenario.signalGroupId2));
		log.info("avg cycle time per system: " + avgCycleTimePerSystem.get(SingleCrossingScenario.signalSystemId));
		log.info("avg delay per link: " + avgDelayWE + ", " + avgDelayNS);
		
		Assert.assertTrue("total signal green time of WE-direction should be higher than NS-direction", totalSignalGreenTimes.get(SingleCrossingScenario.signalGroupId1)-totalSignalGreenTimes.get(SingleCrossingScenario.signalGroupId2) > 0);
		Assert.assertTrue("avg signal green time per cycle of WE-direction should be higher than NS-direction", avgSignalGreenTimePerCycle.get(SingleCrossingScenario.signalGroupId1)-avgSignalGreenTimePerCycle.get(SingleCrossingScenario.signalGroupId2) > 0);
		Assert.assertTrue("avg signal green time per cycle of NS-direction should be less than 5 seconds", avgSignalGreenTimePerCycle.get(SingleCrossingScenario.signalGroupId2) < 5.0);
		Assert.assertTrue("cycle time should stay below 90 seconds", avgCycleTimePerSystem.get(SingleCrossingScenario.signalSystemId) <= 90);
		Assert.assertTrue("avg delay per vehicle on WS-direction should be less than on NS-direction", avgDelayWE<avgDelayNS);
	}
	
	/**
	 * directions with the same demand-capacity-ration should get green for more or less the same time
	 */
	@Test
	public void testSingleCrossingScenarioEqualDemandCapacityRatio(){
		SignalAnalysisTool signalAnalyzer = new SignalAnalysisTool();
		TtGeneralAnalysis generalAnalyzer = runSingleCrossingScenario(900, 1800, signalAnalyzer, Regime.COMBINED);
		
		Map<Id<SignalGroup>, Double> totalSignalGreenTimes = signalAnalyzer.getTotalSignalGreenTime(); 
		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycle = signalAnalyzer.calculateAvgSignalGreenTimePerFlexibleCycle(); 
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystem = signalAnalyzer.calculateAvgFlexibleCycleTimePerSignalSystem(); 
		Map<Id<Link>, Double> avgDelayPerLink = generalAnalyzer.getAvgDelayPerLink();
		Double avgDelayWE = avgDelayPerLink.get(Id.createLinkId("2_3"));
		Double avgDelayNS = avgDelayPerLink.get(Id.createLinkId("7_3"));
		
		log.info("total signal green times: " + totalSignalGreenTimes.get(SingleCrossingScenario.signalGroupId1) + ", " + totalSignalGreenTimes.get(SingleCrossingScenario.signalGroupId2));
		log.info("avg signal green times per cycle: " + avgSignalGreenTimePerCycle.get(SingleCrossingScenario.signalGroupId1) + ", "
				+ avgSignalGreenTimePerCycle.get(SingleCrossingScenario.signalGroupId2));
		log.info("avg cycle time per system: " + avgCycleTimePerSystem.get(SingleCrossingScenario.signalSystemId));
		log.info("avg delay per link: " + avgDelayWE + ", " + avgDelayNS);
		
		Assert.assertEquals("total signal green times should not differ more than 1%", 1, totalSignalGreenTimes.get(SingleCrossingScenario.signalGroupId1)/totalSignalGreenTimes.get(SingleCrossingScenario.signalGroupId2), 0.01);
		Assert.assertEquals("avg signal green times per cycle should not differ more than 1%", 1, avgSignalGreenTimePerCycle.get(SingleCrossingScenario.signalGroupId1)/avgSignalGreenTimePerCycle.get(SingleCrossingScenario.signalGroupId2), 0.01);
		Assert.assertEquals("avg delay per vehicle per link should not differ more than 1%", 1, avgDelayWE/avgDelayNS, 0.01);
	}

	/**
	 * compare different regimes for laemmer: stabilizing, optimizing and combination of both (combined)
	 * for low demand, i.e. an occupancy rate of 0.5 in the example of nico kuehnel's master thesis, the optimizing regime should be better than the stabilizing regime.
	 */
	@Test
	public void testSingleCrossingScenarioStabilizingVsOptimizingRegimeLowDemand(){
		SignalAnalysisTool signalAnalyzerStab = new SignalAnalysisTool();
		TtGeneralAnalysis generalAnalyzerStab = runSingleCrossingScenario(360, 1440, signalAnalyzerStab, Regime.STABILIZING);
		
		SignalAnalysisTool signalAnalyzerOpt = new SignalAnalysisTool();
		TtGeneralAnalysis generalAnalyzerOpt = runSingleCrossingScenario(360, 1440, signalAnalyzerOpt, Regime.OPTIMIZING);
		
		SignalAnalysisTool signalAnalyzerComb = new SignalAnalysisTool();
		TtGeneralAnalysis generalAnalyzerComb = runSingleCrossingScenario(360, 1440, signalAnalyzerComb, Regime.COMBINED);
		
		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycleStab = signalAnalyzerStab.calculateAvgSignalGreenTimePerFlexibleCycle(); 
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystemStab = signalAnalyzerStab.calculateAvgFlexibleCycleTimePerSignalSystem(); 
		Map<Id<Link>, Double> avgDelayPerLinkStab = generalAnalyzerStab.getAvgDelayPerLink();
		
		log.info("demand 360,1440 (i.e. 0.5) -- results for the stabilizing regime:");
		log.info("avg signal green times per cycle: " + avgSignalGreenTimePerCycleStab.get(SingleCrossingScenario.signalGroupId1) + ", "
				+ avgSignalGreenTimePerCycleStab.get(SingleCrossingScenario.signalGroupId2));
		log.info("avg cycle time per system: " + avgCycleTimePerSystemStab.get(SingleCrossingScenario.signalSystemId));
		log.info("avg delay per link: " + avgDelayPerLinkStab.get(Id.createLinkId("2_3")) + ", " + avgDelayPerLinkStab.get(Id.createLinkId("7_3")));
		log.info("Total travel time: " + generalAnalyzerStab.getTotalTt() + ", total delay: " + generalAnalyzerStab.getTotalDelay());
		
		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycleOpt = signalAnalyzerOpt.calculateAvgSignalGreenTimePerFlexibleCycle(); 
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystemOpt = signalAnalyzerOpt.calculateAvgFlexibleCycleTimePerSignalSystem(); 
		Map<Id<Link>, Double> avgDelayPerLinkOpt = generalAnalyzerOpt.getAvgDelayPerLink();
		
		log.info("demand 360,1440 (i.e. 0.5) -- results for the optimizing regime:");
		log.info("avg signal green times per cycle: " + avgSignalGreenTimePerCycleOpt.get(SingleCrossingScenario.signalGroupId1) + ", "
				+ avgSignalGreenTimePerCycleOpt.get(SingleCrossingScenario.signalGroupId2));
		log.info("avg cycle time per system: " + avgCycleTimePerSystemOpt.get(SingleCrossingScenario.signalSystemId));
		log.info("avg delay per link: " + avgDelayPerLinkOpt.get(Id.createLinkId("2_3")) + ", " + avgDelayPerLinkOpt.get(Id.createLinkId("7_3")));
		log.info("Total travel time: " + generalAnalyzerOpt.getTotalTt() + ", total delay: " + generalAnalyzerOpt.getTotalDelay());
		
		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycleComb = signalAnalyzerComb.calculateAvgSignalGreenTimePerFlexibleCycle(); 
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystemComb = signalAnalyzerComb.calculateAvgFlexibleCycleTimePerSignalSystem(); 
		Map<Id<Link>, Double> avgDelayPerLinkComb = generalAnalyzerComb.getAvgDelayPerLink();
		
		log.info("demand 360,1440 (i.e. 0.5) -- results for the combined regime:");
		log.info("avg signal green times per cycle: " + avgSignalGreenTimePerCycleComb.get(SingleCrossingScenario.signalGroupId1) + ", "
				+ avgSignalGreenTimePerCycleComb.get(SingleCrossingScenario.signalGroupId2));
		log.info("avg cycle time per system: " + avgCycleTimePerSystemComb.get(SingleCrossingScenario.signalSystemId));
		log.info("avg delay per link: " + avgDelayPerLinkComb.get(Id.createLinkId("2_3")) + ", " + avgDelayPerLinkComb.get(Id.createLinkId("7_3")));
		log.info("Total travel time: " + generalAnalyzerComb.getTotalTt() + ", total delay: " + generalAnalyzerComb.getTotalDelay());
		
		// Test Stabilizing Regime:
		for (Id<Link> linkId : avgDelayPerLinkStab.keySet()) {
			Assert.assertTrue("stab: avg delay per link should be below a threshold (i.e. still stable)", avgDelayPerLinkStab.get(linkId) < maxCycleTime);
		}
		Assert.assertTrue("stab: total delay should be higher than for the other regimes", generalAnalyzerStab.getTotalDelay() > generalAnalyzerOpt.getTotalDelay());
		Assert.assertTrue("stab: total delay should be higher than for the other regimes", generalAnalyzerStab.getTotalDelay() > generalAnalyzerComb.getTotalDelay());
		Assert.assertTrue("stab: total travel time should be higher than for the other regimes", generalAnalyzerStab.getTotalTt() > generalAnalyzerOpt.getTotalTt());
		Assert.assertTrue("stab: total travel time should be higher than for the other regimes", generalAnalyzerStab.getTotalTt() > generalAnalyzerComb.getTotalTt());
		Assert.assertTrue("the stabilizing regime should satisfy the maximum cycle time", avgCycleTimePerSystemStab.get(SingleCrossingScenario.signalSystemId) < maxCycleTime);
		// stabilizing regime only shows green when number of vehicles beyond a critical number, i.e. some of the cycle time is given away (all signals show red)
		Assert.assertTrue("stab: sum of green times per cycle plus 10 seconds intergreen time should be more than 10 seconds less than the avg cycle time", 
				avgSignalGreenTimePerCycleStab.get(SingleCrossingScenario.signalGroupId1) + avgSignalGreenTimePerCycleStab.get(SingleCrossingScenario.signalGroupId2) + cycleIntergreenTime 
				< avgCycleTimePerSystemStab.get(SingleCrossingScenario.signalSystemId) - 10);
		
		// Test Optimizing Regime:
		for (Id<Link> linkId : avgDelayPerLinkOpt.keySet()) {
			Assert.assertTrue("opt: avg delay per link should be below a threshold (i.e. still stable)", avgDelayPerLinkOpt.get(linkId) < maxCycleTime);
		}
		Assert.assertEquals("sum of green times per cycle plus 10 seconds intergreen time should be more or less equal to the avg cycle time", 
				avgCycleTimePerSystemOpt.get(SingleCrossingScenario.signalSystemId), 
				avgSignalGreenTimePerCycleOpt.get(SingleCrossingScenario.signalGroupId1) + avgSignalGreenTimePerCycleOpt.get(SingleCrossingScenario.signalGroupId2) + cycleIntergreenTime, 
				2);
		Assert.assertTrue("for this demand, the cycle time of the optimizing regime should be still reasonable, i.e. below a threshold", 
				avgCycleTimePerSystemOpt.get(SingleCrossingScenario.signalSystemId) < maxCycleTime);
		
		// Test Combined Regime:
		for (Id<Link> linkId : avgDelayPerLinkComb.keySet()) {
			Assert.assertTrue("avg delay per link should be below a threshold (i.e. still stable)", avgDelayPerLinkComb.get(linkId) < maxCycleTime);
		}
		Assert.assertEquals("comb: sum of green times per cycle plus 10 seconds intergreen time should be more or less equal to the avg cycle time", 
				avgCycleTimePerSystemComb.get(SingleCrossingScenario.signalSystemId), 
				avgSignalGreenTimePerCycleComb.get(SingleCrossingScenario.signalGroupId1) + avgSignalGreenTimePerCycleComb.get(SingleCrossingScenario.signalGroupId2) + cycleIntergreenTime, 
				2);
		Assert.assertTrue("the combined regime should satisfy the maximum cycle time", avgCycleTimePerSystemComb.get(SingleCrossingScenario.signalSystemId) < maxCycleTime);
		Assert.assertTrue("total delay with the combined regime should be the lowest", generalAnalyzerOpt.getTotalDelay() > generalAnalyzerComb.getTotalDelay());
		Assert.assertTrue("total travel time with the combined regime should be the lowest", generalAnalyzerOpt.getTotalTt() > generalAnalyzerComb.getTotalTt());
	}
	
	/**
	 * compare different regimes for laemmer: stabilizing, optimizing and combination of both (combined)
	 * for high demand, i.e. an occupancy rate of 0.6 in the example of nico kuehnel's master thesis, the optimizing regime should no longer be stable (high delays, high travel time, 
	 * no standard cycle pattern). The stabilizing regime should still be stable, the combined regime should be the best.
	 */
	@Test
	public void testSingleCrossingScenarioStabilizingVsOptimizingRegimeHighDemand(){
		SignalAnalysisTool signalAnalyzerStab = new SignalAnalysisTool();
		TtGeneralAnalysis generalAnalyzerStab = runSingleCrossingScenario(360, 1800, signalAnalyzerStab, Regime.STABILIZING);
		
		SignalAnalysisTool signalAnalyzerOpt = new SignalAnalysisTool();
		TtGeneralAnalysis generalAnalyzerOpt = runSingleCrossingScenario(360, 1800, signalAnalyzerOpt, Regime.OPTIMIZING);
		
		SignalAnalysisTool signalAnalyzerComb = new SignalAnalysisTool();
		TtGeneralAnalysis generalAnalyzerComb = runSingleCrossingScenario(360, 1800, signalAnalyzerComb, Regime.COMBINED);
		
		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycleStab = signalAnalyzerStab.calculateAvgSignalGreenTimePerFlexibleCycle(); 
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystemStab = signalAnalyzerStab.calculateAvgFlexibleCycleTimePerSignalSystem(); 
		Map<Id<Link>, Double> avgDelayPerLinkStab = generalAnalyzerStab.getAvgDelayPerLink();
		
		log.info("demand 360,1800 (i.e. 0.6) -- results for the stabilizing regime:");
		log.info("avg signal green times per cycle: " + avgSignalGreenTimePerCycleStab.get(SingleCrossingScenario.signalGroupId1) + ", "
				+ avgSignalGreenTimePerCycleStab.get(SingleCrossingScenario.signalGroupId2));
		log.info("avg cycle time per system: " + avgCycleTimePerSystemStab.get(SingleCrossingScenario.signalSystemId));
		log.info("avg delay per link: " + avgDelayPerLinkStab.get(Id.createLinkId("2_3")) + ", " + avgDelayPerLinkStab.get(Id.createLinkId("7_3")));
		log.info("Total travel time: " + generalAnalyzerStab.getTotalTt() + ", total delay: " + generalAnalyzerStab.getTotalDelay());
		
		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycleOpt = signalAnalyzerOpt.calculateAvgSignalGreenTimePerFlexibleCycle(); 
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystemOpt = signalAnalyzerOpt.calculateAvgFlexibleCycleTimePerSignalSystem(); 
		Map<Id<Link>, Double> avgDelayPerLinkOpt = generalAnalyzerOpt.getAvgDelayPerLink();
		
		log.info("demand 360,1800 (i.e. 0.6) -- results for the optimizing regime:");
		log.info("avg signal green times per cycle: " + avgSignalGreenTimePerCycleOpt.get(SingleCrossingScenario.signalGroupId1) + ", "
				+ avgSignalGreenTimePerCycleOpt.get(SingleCrossingScenario.signalGroupId2));
		log.info("avg cycle time per system: " + avgCycleTimePerSystemOpt.get(SingleCrossingScenario.signalSystemId));
		log.info("avg delay per link: " + avgDelayPerLinkOpt.get(Id.createLinkId("2_3")) + ", " + avgDelayPerLinkOpt.get(Id.createLinkId("7_3")));
		log.info("Total travel time: " + generalAnalyzerOpt.getTotalTt() + ", total delay: " + generalAnalyzerOpt.getTotalDelay());
		
		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycleComb = signalAnalyzerComb.calculateAvgSignalGreenTimePerFlexibleCycle(); 
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystemComb = signalAnalyzerComb.calculateAvgFlexibleCycleTimePerSignalSystem(); 
		Map<Id<Link>, Double> avgDelayPerLinkComb = generalAnalyzerComb.getAvgDelayPerLink();
		
		log.info("demand 360,1800 (i.e. 0.6) -- results for the combined regime:");
		log.info("avg signal green times per cycle: " + avgSignalGreenTimePerCycleComb.get(SingleCrossingScenario.signalGroupId1) + ", "
				+ avgSignalGreenTimePerCycleComb.get(SingleCrossingScenario.signalGroupId2));
		log.info("avg cycle time per system: " + avgCycleTimePerSystemComb.get(SingleCrossingScenario.signalSystemId));
		log.info("avg delay per link: " + avgDelayPerLinkComb.get(Id.createLinkId("2_3")) + ", " + avgDelayPerLinkComb.get(Id.createLinkId("7_3")));
		log.info("Total travel time: " + generalAnalyzerComb.getTotalTt() + ", total delay: " + generalAnalyzerComb.getTotalDelay());
		
		// Test Stabilizing Regime:
		for (Id<Link> linkId : avgDelayPerLinkStab.keySet()) {
			Assert.assertTrue("stab: avg delay per link should be below a threshold (i.e. still stable)", avgDelayPerLinkStab.get(linkId) < maxCycleTime);
		}
		Assert.assertTrue("the stabilizing regime should satisfy the maximum cycle time", avgCycleTimePerSystemStab.get(SingleCrossingScenario.signalSystemId) < maxCycleTime);
		// stabilizing regime only shows green when number of vehicles beyond a critical number, i.e. some of the cycle time is given away (all signals show red)
		Assert.assertTrue("stab: sum of green times per cycle plus 10 seconds intergreen time should be more than 10 seconds less than the avg cycle time", 
				avgSignalGreenTimePerCycleStab.get(SingleCrossingScenario.signalGroupId1) + avgSignalGreenTimePerCycleStab.get(SingleCrossingScenario.signalGroupId2) + cycleIntergreenTime 
				< avgCycleTimePerSystemStab.get(SingleCrossingScenario.signalSystemId) - 10);
		
		// Test Optimizing Regime:
		Assert.assertTrue("avg delay for NS-direction should be very high for the optimizing regime with high demand", avgDelayPerLinkOpt.get(Id.createLinkId("7_3")) > maxCycleTime);
		Assert.assertTrue("total delay of optimizing regime should be the highest", generalAnalyzerStab.getTotalDelay() < generalAnalyzerOpt.getTotalDelay());
		Assert.assertTrue("total travel time of optimizing regime should be the highest", generalAnalyzerStab.getTotalTt() < generalAnalyzerOpt.getTotalTt());
		Assert.assertTrue("for this demand, the cycle time of the optimizing regime should be very high, i.e. not stable anymore", 
				avgCycleTimePerSystemOpt.get(SingleCrossingScenario.signalSystemId) > 10*maxCycleTime);
		
		// Test Combined Regime:
		for (Id<Link> linkId : avgDelayPerLinkComb.keySet()) {
			Assert.assertTrue("avg delay per link should be below a threshold (i.e. still stable)", avgDelayPerLinkComb.get(linkId) < maxCycleTime);
		}
		Assert.assertEquals("comb: sum of green times per cycle plus 10 seconds intergreen time should be more or less equal to the avg cycle time", 
				avgCycleTimePerSystemComb.get(SingleCrossingScenario.signalSystemId), 
				avgSignalGreenTimePerCycleComb.get(SingleCrossingScenario.signalGroupId1) + avgSignalGreenTimePerCycleComb.get(SingleCrossingScenario.signalGroupId2) + cycleIntergreenTime, 
				2);
		Assert.assertTrue("the combined regime should satisfy the maximum cycle time", avgCycleTimePerSystemComb.get(SingleCrossingScenario.signalSystemId) < maxCycleTime);
		Assert.assertTrue("total delay with the combined regime should be the lowest", generalAnalyzerOpt.getTotalDelay() > generalAnalyzerComb.getTotalDelay());
		Assert.assertTrue("total travel time with the combined regime should be the lowest", generalAnalyzerOpt.getTotalTt() > generalAnalyzerComb.getTotalTt());
		Assert.assertTrue("total delay with the combined regime should be the lowest", generalAnalyzerStab.getTotalDelay() > generalAnalyzerComb.getTotalDelay());
		Assert.assertTrue("total travel time with the combined regime should be the lowest", generalAnalyzerStab.getTotalTt() > generalAnalyzerComb.getTotalTt());
	}

	/**
	 * test laemmer for different flow capacity factors: a factor of 0.5 should result in the same signal settings as a factor of 1.0 when the demand is exactly doubled (same departure times).
	 */
	@Test
	public void testSingleCrossingScenarioWithDifferentFlowCapacityFactors(){
		SignalAnalysisTool signalAnalyzerFlowCap5 = new SignalAnalysisTool();
		TtGeneralAnalysis generalAnalyzerFlowCap5 = runSingleCrossingScenario(180, 900, 0.5, false, signalAnalyzerFlowCap5, Regime.COMBINED);
		
		SignalAnalysisTool signalAnalyzerFlowCap1 = new SignalAnalysisTool();
		TtGeneralAnalysis generalAnalyzerFlowCap1 = runSingleCrossingScenario(180, 900, 1.0, true, signalAnalyzerFlowCap1, Regime.COMBINED);
		
		Map<Id<SignalGroup>, Double> totalSignalGreenTimesFlowCap1 = signalAnalyzerFlowCap1.getTotalSignalGreenTime(); 
		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycleFlowCap1 = signalAnalyzerFlowCap1.calculateAvgSignalGreenTimePerFlexibleCycle(); 
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystemFlowCap1 = signalAnalyzerFlowCap1.calculateAvgFlexibleCycleTimePerSignalSystem(); 
		Map<Id<Link>, Double> avgDelayPerLinkFlowCap1 = generalAnalyzerFlowCap1.getAvgDelayPerLink();
		
		log.info("total signal green times: " + totalSignalGreenTimesFlowCap1.get(SingleCrossingScenario.signalGroupId1) + ", " + totalSignalGreenTimesFlowCap1.get(SingleCrossingScenario.signalGroupId2));
		log.info("avg signal green times per cycle: " + avgSignalGreenTimePerCycleFlowCap1.get(SingleCrossingScenario.signalGroupId1) + ", "
				+ avgSignalGreenTimePerCycleFlowCap1.get(SingleCrossingScenario.signalGroupId2));
		log.info("avg cycle time per system: " + avgCycleTimePerSystemFlowCap1.get(SingleCrossingScenario.signalSystemId));
		log.info("avg delay per link: " + avgDelayPerLinkFlowCap1.get(Id.createLinkId("2_3")) + ", " + avgDelayPerLinkFlowCap1.get(Id.createLinkId("7_3")));
		log.info("Total travel time: " + generalAnalyzerFlowCap1.getTotalTt() + ", total delay: " + generalAnalyzerFlowCap1.getTotalDelay());
		
		Map<Id<SignalGroup>, Double> totalSignalGreenTimesFlowCap5 = signalAnalyzerFlowCap5.getTotalSignalGreenTime(); 
		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycleFlowCap5 = signalAnalyzerFlowCap5.calculateAvgSignalGreenTimePerFlexibleCycle(); 
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystemFlowCap5 = signalAnalyzerFlowCap5.calculateAvgFlexibleCycleTimePerSignalSystem(); 
		Map<Id<Link>, Double> avgDelayPerLinkFlowCap5 = generalAnalyzerFlowCap5.getAvgDelayPerLink();
		
		log.info("total signal green times: " + totalSignalGreenTimesFlowCap5.get(SingleCrossingScenario.signalGroupId1) + ", " + totalSignalGreenTimesFlowCap5.get(SingleCrossingScenario.signalGroupId2));
		log.info("avg signal green times per cycle: " + avgSignalGreenTimePerCycleFlowCap5.get(SingleCrossingScenario.signalGroupId1) + ", "
				+ avgSignalGreenTimePerCycleFlowCap5.get(SingleCrossingScenario.signalGroupId2));
		log.info("avg cycle time per system: " + avgCycleTimePerSystemFlowCap5.get(SingleCrossingScenario.signalSystemId));
		log.info("avg delay per link: " + avgDelayPerLinkFlowCap5.get(Id.createLinkId("2_3")) + ", " + avgDelayPerLinkFlowCap5.get(Id.createLinkId("7_3")));
		log.info("Total travel time: " + generalAnalyzerFlowCap5.getTotalTt() + ", total delay: " + generalAnalyzerFlowCap5.getTotalDelay());
		
		Assert.assertEquals("total signal green times should not differ", 1, 
				totalSignalGreenTimesFlowCap5.get(SingleCrossingScenario.signalGroupId1)/totalSignalGreenTimesFlowCap1.get(SingleCrossingScenario.signalGroupId1), 0.01);
		Assert.assertEquals("total signal green times should not differ", 1, 
				totalSignalGreenTimesFlowCap5.get(SingleCrossingScenario.signalGroupId2)/totalSignalGreenTimesFlowCap1.get(SingleCrossingScenario.signalGroupId2), 0.01);
		Assert.assertEquals("avg signal green times per cycle should not differ", avgSignalGreenTimePerCycleFlowCap5.get(SingleCrossingScenario.signalGroupId1), 
				avgSignalGreenTimePerCycleFlowCap1.get(SingleCrossingScenario.signalGroupId1), 0.1);
		Assert.assertEquals("avg signal green times per cycle should not differ", avgSignalGreenTimePerCycleFlowCap5.get(SingleCrossingScenario.signalGroupId2),
				avgSignalGreenTimePerCycleFlowCap1.get(SingleCrossingScenario.signalGroupId2), 0.1);
		Assert.assertEquals("avg cycle time should not differ", avgCycleTimePerSystemFlowCap5.get(SingleCrossingScenario.signalSystemId),
				avgCycleTimePerSystemFlowCap1.get(SingleCrossingScenario.signalSystemId), 0.1);
		Assert.assertEquals("avg delay per vehicle per link should not differ", avgDelayPerLinkFlowCap5.get(Id.createLinkId("2_3")),
				avgDelayPerLinkFlowCap1.get(Id.createLinkId("2_3")), 0.1);
		Assert.assertEquals("avg delay per vehicle per link should not differ", avgDelayPerLinkFlowCap5.get(Id.createLinkId("7_3")),
				avgDelayPerLinkFlowCap1.get(Id.createLinkId("7_3")), 2);
		Assert.assertEquals("total delay for doubled demand should be doubled", 2, generalAnalyzerFlowCap1.getTotalDelay()/generalAnalyzerFlowCap5.getTotalDelay(), 0.1);
		Assert.assertEquals("total travel time for doubled demand should be doubled", 2, generalAnalyzerFlowCap1.getTotalTt()/generalAnalyzerFlowCap5.getTotalTt(), 0.1);
	}

	private TtGeneralAnalysis runSingleCrossingScenario(double flowNS, double flowWE, SignalAnalysisTool signalAnalyzer, Regime regime) {
		return runSingleCrossingScenario(flowNS, flowWE, false, 1.0, false, signalAnalyzer, regime);
	}
	
	private TtGeneralAnalysis runSingleCrossingScenario(double flowNS, double flowWE, boolean minG, SignalAnalysisTool signalAnalyzer, Regime regime) {
		return runSingleCrossingScenario(flowNS, flowWE, minG, 1.0, false, signalAnalyzer, regime);
	}
	
	private TtGeneralAnalysis runSingleCrossingScenario(double flowNS, double flowWE, double flowCapFactor, boolean doublePersons, SignalAnalysisTool signalAnalyzer, Regime regime) {
		return runSingleCrossingScenario(flowNS, flowWE, false, flowCapFactor, doublePersons, signalAnalyzer, regime);
	}
	
	private TtGeneralAnalysis runSingleCrossingScenario(double flowNS, double flowWE, boolean minG, double flowCapFactor, boolean doublePersons, SignalAnalysisTool signalAnalyzer, Regime regime) {
		SingleCrossingScenario singleCrossingScenario = new SingleCrossingScenario(flowNS, flowWE, SingleCrossingScenario.SignalControl.LAEMMER_NICO, regime, StabilizationStrategy.USE_MAX_LANECOUNT, false, false, false, true, true, 0, false);
		if (minG){
			singleCrossingScenario.setMinG(5);
		}
		
		Controler controler = singleCrossingScenario.defineControler();
		if (doublePersons){
			ModifyPopulation.doubleEachPerson(controler.getScenario().getPopulation());
		}
		controler.getConfig().qsim().setFlowCapFactor(flowCapFactor);
		controler.getConfig().controler().setOutputDirectory(testUtils.getOutputDirectory());
		// add signal analysis tool
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addEventHandlerBinding().toInstance(signalAnalyzer);
				this.addControlerListenerBinding().toInstance(signalAnalyzer);
			}
		});
		// add general analysis tools
		TtGeneralAnalysis generalAnalysis = new TtGeneralAnalysis(controler.getScenario().getNetwork());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addEventHandlerBinding().toInstance(generalAnalysis);
			}
		});
	
		controler.run();
		
		return generalAnalysis;
	}
	
	// TODO test stochasticity (laemmer better than fixed-time; different than for constant demand)
	// TODO test temporarily overcrowded situations (no exeption; signal is able to resolve congestion; like fixed-time schedule)
	// TODO test liveArrivalRate vs. exact data (the second results in more precise green times?!; liveArrivalRates are determined correctly)
	// TODO test grouping
	// TODO test lanes
	// ...

}
