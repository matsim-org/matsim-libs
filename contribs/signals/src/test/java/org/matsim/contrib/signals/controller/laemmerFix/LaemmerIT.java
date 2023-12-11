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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.signals.analysis.DelayAnalysisTool;
import org.matsim.contrib.signals.analysis.SignalAnalysisTool;
import org.matsim.contrib.signals.builder.Signals;
import org.matsim.contrib.signals.controller.laemmerFix.LaemmerConfigGroup.Regime;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.lanes.Lane;
import org.matsim.lanes.LanesToLinkAssignment;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author tthunig
 *
 */
public class LaemmerIT {

	private static final Logger log = LogManager.getLogger(LaemmerIT.class);

	@RegisterExtension
	private MatsimTestUtils testUtils = new MatsimTestUtils();

	private static final int maxCycleTime = 90;
	private static final int cycleIntergreenTime = 10;
	private final Id<SignalGroup> signalGroupId1 = Id.create("SignalGroup1", SignalGroup.class);
	private final Id<SignalGroup> signalGroupId1l = Id.create("SignalGroup1l", SignalGroup.class);
	private final Id<SignalGroup> signalGroupId2 = Id.create("SignalGroup2", SignalGroup.class);
	private final Id<SignalSystem> signalSystemId = Id.create("SignalSystem1", SignalSystem.class);

	/**
	 * single intersection with demand (equals flow capacity) only in NS-direction. signals should show green only for the NS-direction.
	 */
	@Test
	void testSingleCrossingScenarioDemandNS() {
		Fixture fixture = new Fixture(1800, 0, 5.0, Regime.COMBINED);
		SignalAnalysisTool signalAnalyzer = new SignalAnalysisTool();
		DelayAnalysisTool generalAnalyzer = fixture.run(signalAnalyzer);

		Map<Id<SignalGroup>, Double> totalSignalGreenTimes = signalAnalyzer.getTotalSignalGreenTime(); // NS should show a lot more green than WE
		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycle = signalAnalyzer.calculateAvgSignalGreenTimePerFlexibleCycle(); // WE should be almost 0
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystem = signalAnalyzer.calculateAvgFlexibleCycleTimePerSignalSystem(); // should be around 60, at most 90
		Map<Id<Link>, Double> avgDelayPerLink = generalAnalyzer.getAvgDelayPerLink();
		Double avgDelayWE = avgDelayPerLink.get(Id.createLinkId("2_3"));
		Double avgDelayNS = avgDelayPerLink.get(Id.createLinkId("7_3"));

		log.info("total signal green times: " + totalSignalGreenTimes.get(signalGroupId1) + ", "
				+ totalSignalGreenTimes.get(signalGroupId2));
		log.info("avg signal green times per cycle: " + avgSignalGreenTimePerCycle.get(signalGroupId1) + ", "
				+ avgSignalGreenTimePerCycle.get(signalGroupId2));
		log.info("avg cycle time per system: " + avgCycleTimePerSystem.get(signalSystemId));
		log.info("avg delay per link: " + avgDelayWE + ", " + avgDelayNS);

		Assertions.assertNull(avgSignalGreenTimePerCycle.get(signalGroupId1), "signal group 1 should show no green");
		Assertions.assertNotNull(avgSignalGreenTimePerCycle.get(signalGroupId2), "signal group 2 should show green");
		Assertions.assertEquals(0.0, avgDelayNS, MatsimTestUtils.EPSILON, "avg delay at NS-direction should be zero");
	}

	/**
	 * Test minimum green time:
	 * single intersection with high demand in WE-direction, very low demand in NS-direction but minimum green time. I.e. the NS-signal should show green for exactly this 5 seconds per cycle.
	 */
	@Test
	void testSingleCrossingScenarioLowVsHighDemandWithMinG(){
		Fixture fixture = new Fixture(90, 1800, 5.0, Regime.COMBINED);
		SignalAnalysisTool signalAnalyzer = new SignalAnalysisTool();
		DelayAnalysisTool generalAnalyzer = fixture.run(signalAnalyzer);

		Map<Id<SignalGroup>, Double> totalSignalGreenTimes = signalAnalyzer.getTotalSignalGreenTime();
		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycle = signalAnalyzer.calculateAvgSignalGreenTimePerFlexibleCycle();
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystem = signalAnalyzer.calculateAvgFlexibleCycleTimePerSignalSystem();
		Map<Id<Link>, Double> avgDelayPerLink = generalAnalyzer.getAvgDelayPerLink();
		Double avgDelayWE = avgDelayPerLink.get(Id.createLinkId("2_3"));
		Double avgDelayNS = avgDelayPerLink.get(Id.createLinkId("7_3"));

		log.info("total signal green times: " + totalSignalGreenTimes.get(signalGroupId1) + ", " + totalSignalGreenTimes.get(signalGroupId2));
		log.info("avg signal green times per cycle: " + avgSignalGreenTimePerCycle.get(signalGroupId1) + ", "
				+ avgSignalGreenTimePerCycle.get(signalGroupId2));
		log.info("avg cycle time per system: " + avgCycleTimePerSystem.get(signalSystemId));
		log.info("avg delay per link: " + avgDelayWE + ", " + avgDelayNS);

		Assertions.assertTrue(totalSignalGreenTimes.get(signalGroupId1)-totalSignalGreenTimes.get(signalGroupId2) > 0,
				"total signal green time of WE-direction should be higher than NS-direction");
		Assertions.assertTrue(avgSignalGreenTimePerCycle.get(signalGroupId1)-avgSignalGreenTimePerCycle.get(signalGroupId2) > 0,
				"avg signal green time per cycle of WE-direction should be higher than NS-direction");
		Assertions.assertEquals(5.0, avgSignalGreenTimePerCycle.get(signalGroupId2), MatsimTestUtils.EPSILON, "avg signal green time per cycle of NS-direction should be the minimum green time of 5 seconds");
		Assertions.assertTrue(avgCycleTimePerSystem.get(signalSystemId) <= 90, "cycle time should stay below 90 seconds");
		Assertions.assertTrue(avgDelayWE<avgDelayNS, "avg delay per vehicle on WS-direction should be less than on NS-direction");
	}

	/**
	 * counterpart to minimum green time test above.
	 * single intersection with high demand in WE-direction, very low demand in NS-direction. No minimum green time! I.e. the NS-signal should show green for less than 5 seconds per cycle.
	 */
	@Test
	void testSingleCrossingScenarioLowVsHighDemandWoMinG(){
		Fixture fixture = new Fixture(90, 1800, 0.0, Regime.COMBINED);
		SignalAnalysisTool signalAnalyzer = new SignalAnalysisTool();
		DelayAnalysisTool generalAnalyzer = fixture.run(signalAnalyzer);

		Map<Id<SignalGroup>, Double> totalSignalGreenTimes = signalAnalyzer.getTotalSignalGreenTime();
		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycle = signalAnalyzer.calculateAvgSignalGreenTimePerFlexibleCycle();
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystem = signalAnalyzer.calculateAvgFlexibleCycleTimePerSignalSystem();
		Map<Id<Link>, Double> avgDelayPerLink = generalAnalyzer.getAvgDelayPerLink();
		Double avgDelayWE = avgDelayPerLink.get(Id.createLinkId("2_3"));
		Double avgDelayNS = avgDelayPerLink.get(Id.createLinkId("7_3"));

		log.info("total signal green times: " + totalSignalGreenTimes.get(signalGroupId1) + ", " + totalSignalGreenTimes.get(signalGroupId2));
		log.info("avg signal green times per cycle: " + avgSignalGreenTimePerCycle.get(signalGroupId1) + ", "
				+ avgSignalGreenTimePerCycle.get(signalGroupId2));
		log.info("avg cycle time per system: " + avgCycleTimePerSystem.get(signalSystemId));
		log.info("avg delay per link: " + avgDelayWE + ", " + avgDelayNS);

		Assertions.assertTrue(totalSignalGreenTimes.get(signalGroupId1)-totalSignalGreenTimes.get(signalGroupId2) > 0,
				"total signal green time of WE-direction should be higher than NS-direction");
		Assertions.assertTrue(avgSignalGreenTimePerCycle.get(signalGroupId1)-avgSignalGreenTimePerCycle.get(signalGroupId2) > 0,
				"avg signal green time per cycle of WE-direction should be higher than NS-direction");
		Assertions.assertTrue(avgSignalGreenTimePerCycle.get(signalGroupId2) < 5.0, "avg signal green time per cycle of NS-direction should be less than 5 seconds");
		Assertions.assertTrue(avgCycleTimePerSystem.get(signalSystemId) <= 90, "cycle time should stay below 90 seconds");
		Assertions.assertTrue(avgDelayWE<avgDelayNS, "avg delay per vehicle on WS-direction should be less than on NS-direction");
	}

	/**
	 * directions with the same demand-capacity-ration should get green for more or less the same time
	 */
	@Test
	void testSingleCrossingScenarioEqualDemandCapacityRatio(){
		Fixture fixture = new Fixture(900, 1800, 0.0, Regime.COMBINED);
		SignalAnalysisTool signalAnalyzer = new SignalAnalysisTool();
		DelayAnalysisTool generalAnalyzer = fixture.run(signalAnalyzer);

		Map<Id<SignalGroup>, Double> totalSignalGreenTimes = signalAnalyzer.getTotalSignalGreenTime();
		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycle = signalAnalyzer.calculateAvgSignalGreenTimePerFlexibleCycle();
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystem = signalAnalyzer.calculateAvgFlexibleCycleTimePerSignalSystem();
		Map<Id<Link>, Double> avgDelayPerLink = generalAnalyzer.getAvgDelayPerLink();
		Double avgDelayWE = avgDelayPerLink.get(Id.createLinkId("2_3"));
		Double avgDelayNS = avgDelayPerLink.get(Id.createLinkId("7_3"));

		log.info("total signal green times: " + totalSignalGreenTimes.get(signalGroupId1) + ", " + totalSignalGreenTimes.get(signalGroupId2));
		log.info("avg signal green times per cycle: " + avgSignalGreenTimePerCycle.get(signalGroupId1) + ", "
				+ avgSignalGreenTimePerCycle.get(signalGroupId2));
		log.info("avg cycle time per system: " + avgCycleTimePerSystem.get(signalSystemId));
		log.info("avg delay per link: " + avgDelayWE + ", " + avgDelayNS);

		Assertions.assertEquals(1,
				totalSignalGreenTimes.get(signalGroupId1)/totalSignalGreenTimes.get(signalGroupId2), 0.01, "total signal green times should not differ more than 1%");
		Assertions.assertEquals(1, avgSignalGreenTimePerCycle.get(signalGroupId1)/avgSignalGreenTimePerCycle.get(signalGroupId2), 0.01, "avg signal green times per cycle should not differ more than 1%");
//		Assert.assertEquals("avg delay per vehicle per link should not differ more than 5%", 1, avgDelayWE/avgDelayNS, 0.05);
		/* I commented the last line out because the delay strongly depends on the stabilizing regime, namely the arrival rates that are used.
		 * Even if I use fixed average arrival rates of 0.5 per lane and link, the delays of both directions do not correlate.
		 * Maybe that is okay since the signal green times are still the same, i.e. only the green phases are distributed differently over the simulation time...?
		 * theresa, jun'2020
		 */
	}

	/**
	 * compare different regimes for laemmer: stabilizing, optimizing and combination of both (combined)
	 * for low demand, i.e. an occupancy rate of 0.5 in the example of nico kuehnel's master thesis, the optimizing regime should be better than the stabilizing regime.
	 */
	@Test
	void testSingleCrossingScenarioStabilizingVsOptimizingRegimeLowDemand(){
		Fixture fixtureStab = new Fixture(360, 1440, 0.0, Regime.STABILIZING);
		SignalAnalysisTool signalAnalyzerStab = new SignalAnalysisTool();
		DelayAnalysisTool generalAnalyzerStab = fixtureStab.run(signalAnalyzerStab);

		Fixture fixtureOpt = new Fixture(360, 1440, 0.0, Regime.OPTIMIZING);
		SignalAnalysisTool signalAnalyzerOpt = new SignalAnalysisTool();
		DelayAnalysisTool generalAnalyzerOpt = fixtureOpt.run(signalAnalyzerOpt);

		Fixture fixtureComb = new Fixture(360, 1440, 0.0, Regime.COMBINED);
		SignalAnalysisTool signalAnalyzerComb = new SignalAnalysisTool();
		DelayAnalysisTool generalAnalyzerComb = fixtureComb.run(signalAnalyzerComb);

		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycleStab = signalAnalyzerStab.calculateAvgSignalGreenTimePerFlexibleCycle();
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystemStab = signalAnalyzerStab.calculateAvgFlexibleCycleTimePerSignalSystem();
		Map<Id<Link>, Double> avgDelayPerLinkStab = generalAnalyzerStab.getAvgDelayPerLink();

		log.info("demand 360,1440 (i.e. 0.5) -- results for the stabilizing regime:");
		log.info("avg signal green times per cycle: " + avgSignalGreenTimePerCycleStab.get(signalGroupId1) + ", "
				+ avgSignalGreenTimePerCycleStab.get(signalGroupId2));
		log.info("avg cycle time per system: " + avgCycleTimePerSystemStab.get(signalSystemId));
		log.info("avg delay per link: " + avgDelayPerLinkStab.get(Id.createLinkId("2_3")) + ", " + avgDelayPerLinkStab.get(Id.createLinkId("7_3")));
		log.info("Total delay: " + generalAnalyzerStab.getTotalDelay());

		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycleOpt = signalAnalyzerOpt.calculateAvgSignalGreenTimePerFlexibleCycle();
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystemOpt = signalAnalyzerOpt.calculateAvgFlexibleCycleTimePerSignalSystem();
		Map<Id<Link>, Double> avgDelayPerLinkOpt = generalAnalyzerOpt.getAvgDelayPerLink();

		log.info("demand 360,1440 (i.e. 0.5) -- results for the optimizing regime:");
		log.info("avg signal green times per cycle: " + avgSignalGreenTimePerCycleOpt.get(signalGroupId1) + ", "
				+ avgSignalGreenTimePerCycleOpt.get(signalGroupId2));
		log.info("avg cycle time per system: " + avgCycleTimePerSystemOpt.get(signalSystemId));
		log.info("avg delay per link: " + avgDelayPerLinkOpt.get(Id.createLinkId("2_3")) + ", " + avgDelayPerLinkOpt.get(Id.createLinkId("7_3")));
		log.info("Total delay: " + generalAnalyzerOpt.getTotalDelay());

		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycleComb = signalAnalyzerComb.calculateAvgSignalGreenTimePerFlexibleCycle();
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystemComb = signalAnalyzerComb.calculateAvgFlexibleCycleTimePerSignalSystem();
		Map<Id<Link>, Double> avgDelayPerLinkComb = generalAnalyzerComb.getAvgDelayPerLink();

		log.info("demand 360,1440 (i.e. 0.5) -- results for the combined regime:");
		log.info("avg signal green times per cycle: " + avgSignalGreenTimePerCycleComb.get(signalGroupId1) + ", "
				+ avgSignalGreenTimePerCycleComb.get(signalGroupId2));
		log.info("avg cycle time per system: " + avgCycleTimePerSystemComb.get(signalSystemId));
		log.info("avg delay per link: " + avgDelayPerLinkComb.get(Id.createLinkId("2_3")) + ", " + avgDelayPerLinkComb.get(Id.createLinkId("7_3")));
		log.info("Total delay: " + generalAnalyzerComb.getTotalDelay());

		// Test Stabilizing Regime:
		for (Id<Link> linkId : avgDelayPerLinkStab.keySet()) {
			Assertions.assertTrue(avgDelayPerLinkStab.get(linkId) < maxCycleTime, "stab: avg delay per link should be below a threshold (i.e. still stable)");
		}
		Assertions.assertTrue(generalAnalyzerStab.getTotalDelay() > generalAnalyzerOpt.getTotalDelay(), "stab: total delay should be higher than for the other regimes");
		Assertions.assertTrue(generalAnalyzerStab.getTotalDelay() > generalAnalyzerComb.getTotalDelay(), "stab: total delay should be higher than for the other regimes");
		Assertions.assertTrue(avgCycleTimePerSystemStab.get(signalSystemId) < maxCycleTime, "the stabilizing regime should satisfy the maximum cycle time");
		// stabilizing regime only shows green when number of vehicles beyond a critical number, i.e. some of the cycle time is given away (all signals show red)
		Assertions.assertTrue(avgSignalGreenTimePerCycleStab.get(signalGroupId1) + avgSignalGreenTimePerCycleStab.get(signalGroupId2) + cycleIntergreenTime
				< avgCycleTimePerSystemStab.get(signalSystemId) - 10,
				"stab: sum of green times per cycle plus 10 seconds intergreen time should be more than 10 seconds less than the avg cycle time");

		// Test Optimizing Regime:
		for (Id<Link> linkId : avgDelayPerLinkOpt.keySet()) {
			Assertions.assertTrue(avgDelayPerLinkOpt.get(linkId) < maxCycleTime, "opt: avg delay per link should be below a threshold (i.e. still stable)");
		}
		Assertions.assertEquals(avgCycleTimePerSystemOpt.get(signalSystemId),
				avgSignalGreenTimePerCycleOpt.get(signalGroupId1) + avgSignalGreenTimePerCycleOpt.get(signalGroupId2) + cycleIntergreenTime,
				2,
				"sum of green times per cycle plus 10 seconds intergreen time should be more or less equal to the avg cycle time");
		Assertions.assertTrue(avgCycleTimePerSystemOpt.get(signalSystemId) < maxCycleTime,
				"for this demand, the cycle time of the optimizing regime should be still reasonable, i.e. below a threshold");

		// Test Combined Regime:
		for (Id<Link> linkId : avgDelayPerLinkComb.keySet()) {
			Assertions.assertTrue(avgDelayPerLinkComb.get(linkId) < maxCycleTime, "avg delay per link should be below a threshold (i.e. still stable)");
		}
		Assertions.assertEquals(avgCycleTimePerSystemComb.get(signalSystemId),
				avgSignalGreenTimePerCycleComb.get(signalGroupId1) + avgSignalGreenTimePerCycleComb.get(signalGroupId2) + cycleIntergreenTime,
				2,
				"comb: sum of green times per cycle plus 10 seconds intergreen time should be more or less equal to the avg cycle time");
		Assertions.assertTrue(avgCycleTimePerSystemComb.get(signalSystemId) < maxCycleTime, "the combined regime should satisfy the maximum cycle time");
		Assertions.assertTrue(generalAnalyzerOpt.getTotalDelay() > generalAnalyzerComb.getTotalDelay(), "total delay with the combined regime should be the lowest");
	}

	/**
	 * compare different regimes for laemmer: stabilizing, optimizing and combination of both (combined)
	 * for high demand, i.e. an occupancy rate of 0.6 in the example of nico kuehnel's master thesis, the optimizing regime should no longer be stable (high delays, high travel time,
	 * no standard cycle pattern). The stabilizing regime should still be stable, the combined regime should be the best.
	 */
	@Test
	void testSingleCrossingScenarioStabilizingVsOptimizingRegimeHighDemand(){
		Fixture fixtureStab = new Fixture(360, 1800, 0.0, Regime.STABILIZING);
		SignalAnalysisTool signalAnalyzerStab = new SignalAnalysisTool();
		DelayAnalysisTool generalAnalyzerStab = fixtureStab.run(signalAnalyzerStab);

		Fixture fixtureOpt = new Fixture(360, 1800, 0.0, Regime.OPTIMIZING);
		SignalAnalysisTool signalAnalyzerOpt = new SignalAnalysisTool();
		DelayAnalysisTool generalAnalyzerOpt = fixtureOpt.run(signalAnalyzerOpt);

		Fixture fixtureComb = new Fixture(360, 1800, 0.0, Regime.COMBINED);
		SignalAnalysisTool signalAnalyzerComb = new SignalAnalysisTool();
		DelayAnalysisTool generalAnalyzerComb = fixtureComb.run(signalAnalyzerComb);

		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycleStab = signalAnalyzerStab.calculateAvgSignalGreenTimePerFlexibleCycle();
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystemStab = signalAnalyzerStab.calculateAvgFlexibleCycleTimePerSignalSystem();
		Map<Id<Link>, Double> avgDelayPerLinkStab = generalAnalyzerStab.getAvgDelayPerLink();

		log.info("demand 360,1800 (i.e. 0.6) -- results for the stabilizing regime:");
		log.info("avg signal green times per cycle: " + avgSignalGreenTimePerCycleStab.get(signalGroupId1) + ", "
				+ avgSignalGreenTimePerCycleStab.get(signalGroupId2));
		log.info("avg cycle time per system: " + avgCycleTimePerSystemStab.get(signalSystemId));
		log.info("avg delay per link: " + avgDelayPerLinkStab.get(Id.createLinkId("2_3")) + ", " + avgDelayPerLinkStab.get(Id.createLinkId("7_3")));
		log.info("Total delay: " + generalAnalyzerStab.getTotalDelay());

		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycleOpt = signalAnalyzerOpt.calculateAvgSignalGreenTimePerFlexibleCycle();
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystemOpt = signalAnalyzerOpt.calculateAvgFlexibleCycleTimePerSignalSystem();
		Map<Id<Link>, Double> avgDelayPerLinkOpt = generalAnalyzerOpt.getAvgDelayPerLink();

		log.info("demand 360,1800 (i.e. 0.6) -- results for the optimizing regime:");
		log.info("avg signal green times per cycle: " + avgSignalGreenTimePerCycleOpt.get(signalGroupId1) + ", "
				+ avgSignalGreenTimePerCycleOpt.get(signalGroupId2));
		log.info("avg cycle time per system: " + avgCycleTimePerSystemOpt.get(signalSystemId));
		log.info("avg delay per link: " + avgDelayPerLinkOpt.get(Id.createLinkId("2_3")) + ", " + avgDelayPerLinkOpt.get(Id.createLinkId("7_3")));
		log.info("Total delay: " + generalAnalyzerOpt.getTotalDelay());

		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycleComb = signalAnalyzerComb.calculateAvgSignalGreenTimePerFlexibleCycle();
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystemComb = signalAnalyzerComb.calculateAvgFlexibleCycleTimePerSignalSystem();
		Map<Id<Link>, Double> avgDelayPerLinkComb = generalAnalyzerComb.getAvgDelayPerLink();

		log.info("demand 360,1800 (i.e. 0.6) -- results for the combined regime:");
		log.info("avg signal green times per cycle: " + avgSignalGreenTimePerCycleComb.get(signalGroupId1) + ", "
				+ avgSignalGreenTimePerCycleComb.get(signalGroupId2));
		log.info("avg cycle time per system: " + avgCycleTimePerSystemComb.get(signalSystemId));
		log.info("avg delay per link: " + avgDelayPerLinkComb.get(Id.createLinkId("2_3")) + ", " + avgDelayPerLinkComb.get(Id.createLinkId("7_3")));
		log.info("Total delay: " + generalAnalyzerComb.getTotalDelay());

		// Test Stabilizing Regime:
		for (Id<Link> linkId : avgDelayPerLinkStab.keySet()) {
			Assertions.assertTrue(avgDelayPerLinkStab.get(linkId) < maxCycleTime, "stab: avg delay per link should be below a threshold (i.e. still stable)");
		}
		Assertions.assertTrue(avgCycleTimePerSystemStab.get(signalSystemId) < maxCycleTime, "the stabilizing regime should satisfy the maximum cycle time");
		// stabilizing regime only shows green when number of vehicles beyond a critical number, i.e. some of the cycle time is given away (all signals show red)
		Assertions.assertTrue(avgSignalGreenTimePerCycleStab.get(signalGroupId1) + avgSignalGreenTimePerCycleStab.get(signalGroupId2) + cycleIntergreenTime
				< avgCycleTimePerSystemStab.get(signalSystemId) - 9,
				"stab: sum of green times per cycle plus 10 seconds intergreen time should be more than 9 seconds less than the avg cycle time");

		// Test Optimizing Regime:
		Assertions.assertTrue(avgDelayPerLinkOpt.get(Id.createLinkId("7_3")) > maxCycleTime, "avg delay for NS-direction should be very high for the optimizing regime with high demand");
		Assertions.assertTrue(generalAnalyzerStab.getTotalDelay() < generalAnalyzerOpt.getTotalDelay(), "total delay of optimizing regime should be the highest");
		Assertions.assertTrue(avgCycleTimePerSystemOpt.get(signalSystemId) > 10*maxCycleTime,
				"for this demand, the cycle time of the optimizing regime should be very high, i.e. not stable anymore");

		// Test Combined Regime:
		for (Id<Link> linkId : avgDelayPerLinkComb.keySet()) {
			Assertions.assertTrue(avgDelayPerLinkComb.get(linkId) < maxCycleTime, "avg delay per link should be below a threshold (i.e. still stable)");
		}
		Assertions.assertEquals(avgCycleTimePerSystemComb.get(signalSystemId),
				avgSignalGreenTimePerCycleComb.get(signalGroupId1) + avgSignalGreenTimePerCycleComb.get(signalGroupId2) + cycleIntergreenTime,
				2,
				"comb: sum of green times per cycle plus 10 seconds intergreen time should be more or less equal to the avg cycle time");
		Assertions.assertTrue(avgCycleTimePerSystemComb.get(signalSystemId) < maxCycleTime, "the combined regime should satisfy the maximum cycle time");
		Assertions.assertTrue(generalAnalyzerOpt.getTotalDelay() > generalAnalyzerComb.getTotalDelay(), "total delay with the combined regime should be the lowest");
		Assertions.assertTrue(generalAnalyzerStab.getTotalDelay() > generalAnalyzerComb.getTotalDelay(), "total delay with the combined regime should be the lowest");
	}

	/**
	 * test laemmer for different flow capacity factors: a factor of 0.5 should
	 * result in the same signal settings as a factor of 1.0 when the demand is
	 * exactly doubled (same departure times).
	 */
	@Test
	void testSingleCrossingScenarioWithDifferentFlowCapacityFactors(){
		Fixture fixtureFlowCap1 = new Fixture(360, 1800, 0.0, Regime.COMBINED);
		SignalAnalysisTool signalAnalyzerFlowCap1 = new SignalAnalysisTool();
		DelayAnalysisTool generalAnalyzerFlowCap1 = fixtureFlowCap1.run(signalAnalyzerFlowCap1);

		Fixture fixtureFlowCap2 = new Fixture(360, 1800, 0.0, Regime.COMBINED);
		fixtureFlowCap2.doublePopulation();
		SignalAnalysisTool signalAnalyzerFlowCap2 = new SignalAnalysisTool();
		DelayAnalysisTool generalAnalyzerFlowCap2 = fixtureFlowCap2.run(signalAnalyzerFlowCap2);

		Map<Id<SignalGroup>, Double> totalSignalGreenTimesFlowCap2 = signalAnalyzerFlowCap2.getTotalSignalGreenTime();
		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycleFlowCap2 = signalAnalyzerFlowCap2.calculateAvgSignalGreenTimePerFlexibleCycle();
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystemFlowCap2 = signalAnalyzerFlowCap2.calculateAvgFlexibleCycleTimePerSignalSystem();
		Map<Id<Link>, Double> avgDelayPerLinkFlowCap2 = generalAnalyzerFlowCap2.getAvgDelayPerLink();

		log.info("*** flow capacity 2.0 ***");
		log.info("total signal green times: " + totalSignalGreenTimesFlowCap2.get(signalGroupId1) + ", " + totalSignalGreenTimesFlowCap2.get(signalGroupId2));
		log.info("avg signal green times per cycle: " + avgSignalGreenTimePerCycleFlowCap2.get(signalGroupId1) + ", "
				+ avgSignalGreenTimePerCycleFlowCap2.get(signalGroupId2));
		log.info("avg cycle time per system: " + avgCycleTimePerSystemFlowCap2.get(signalSystemId));
		log.info("avg delay per link: " + avgDelayPerLinkFlowCap2.get(Id.createLinkId("2_3")) + ", " + avgDelayPerLinkFlowCap2.get(Id.createLinkId("7_3")));
		log.info("Total delay: " + generalAnalyzerFlowCap2.getTotalDelay());

		Map<Id<SignalGroup>, Double> totalSignalGreenTimesFlowCap1 = signalAnalyzerFlowCap1.getTotalSignalGreenTime();
		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycleFlowCap1 = signalAnalyzerFlowCap1.calculateAvgSignalGreenTimePerFlexibleCycle();
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystemFlowCap1 = signalAnalyzerFlowCap1.calculateAvgFlexibleCycleTimePerSignalSystem();
		Map<Id<Link>, Double> avgDelayPerLinkFlowCap1 = generalAnalyzerFlowCap1.getAvgDelayPerLink();

		log.info("*** flow capacity 1.0 ***");
		log.info("total signal green times: " + totalSignalGreenTimesFlowCap1.get(signalGroupId1) + ", " + totalSignalGreenTimesFlowCap1.get(signalGroupId2));
		log.info("avg signal green times per cycle: " + avgSignalGreenTimePerCycleFlowCap1.get(signalGroupId1) + ", "
				+ avgSignalGreenTimePerCycleFlowCap1.get(signalGroupId2));
		log.info("avg cycle time per system: " + avgCycleTimePerSystemFlowCap1.get(signalSystemId));
		log.info("avg delay per link: " + avgDelayPerLinkFlowCap1.get(Id.createLinkId("2_3")) + ", " + avgDelayPerLinkFlowCap1.get(Id.createLinkId("7_3")));
		log.info("Total delay: " + generalAnalyzerFlowCap1.getTotalDelay());

		Assertions.assertEquals(1,
				totalSignalGreenTimesFlowCap1.get(signalGroupId1)/totalSignalGreenTimesFlowCap2.get(signalGroupId1), 0.01, "total signal green times should not differ");
		Assertions.assertEquals(1,
				totalSignalGreenTimesFlowCap1.get(signalGroupId2)/totalSignalGreenTimesFlowCap2.get(signalGroupId2), 0.01, "total signal green times should not differ");
		Assertions.assertEquals(avgSignalGreenTimePerCycleFlowCap1.get(signalGroupId1),
				avgSignalGreenTimePerCycleFlowCap2.get(signalGroupId1), 0.1, "avg signal green times per cycle should not differ");
		Assertions.assertEquals(avgSignalGreenTimePerCycleFlowCap1.get(signalGroupId2),
				avgSignalGreenTimePerCycleFlowCap2.get(signalGroupId2), 0.1, "avg signal green times per cycle should not differ");
		Assertions.assertEquals(avgCycleTimePerSystemFlowCap1.get(signalSystemId),
				avgCycleTimePerSystemFlowCap2.get(signalSystemId), 0.1, "avg cycle time should not differ");
		Assertions.assertEquals(avgDelayPerLinkFlowCap1.get(Id.createLinkId("2_3")), avgDelayPerLinkFlowCap2.get(Id.createLinkId("2_3")), 0.1, "avg delay per vehicle per link should not differ");
		Assertions.assertEquals(avgDelayPerLinkFlowCap1.get(Id.createLinkId("7_3")), avgDelayPerLinkFlowCap2.get(Id.createLinkId("7_3")), 2, "avg delay per vehicle per link should not differ");
		Assertions.assertEquals(2, generalAnalyzerFlowCap2.getTotalDelay()/generalAnalyzerFlowCap1.getTotalDelay(), 0.1, "total delay for doubled demand should be doubled");
	}

	/**
	 * Test Laemmer with multiple iterations (some variables have to be reset after iterations).
	 */
	@Test
	void testMultipleIterations() {
		Fixture fixture0It = new Fixture(500, 2000, 5.0, Regime.COMBINED);
		fixture0It.setLastIteration(0);
		fixture0It.addLeftTurnTraffic();
		SignalAnalysisTool signalAnalyzer0It = new SignalAnalysisTool();
		DelayAnalysisTool generalAnalyzer0It = fixture0It.run(signalAnalyzer0It);

		Map<Id<SignalGroup>, Double> totalSignalGreenTimes0It = signalAnalyzer0It.getTotalSignalGreenTime();
		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycle0It = signalAnalyzer0It.calculateAvgSignalGreenTimePerFlexibleCycle();
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystem0It = signalAnalyzer0It.calculateAvgFlexibleCycleTimePerSignalSystem();
		Map<Id<Link>, Double> avgDelayPerLink0It = generalAnalyzer0It.getAvgDelayPerLink();
		Double avgDelayWE0It = avgDelayPerLink0It.get(Id.createLinkId("2_3"));
		Double avgDelayNS0It = avgDelayPerLink0It.get(Id.createLinkId("7_3"));

		log.info("total signal green times in iteration 0: " + totalSignalGreenTimes0It.get(signalGroupId1) + ", " + totalSignalGreenTimes0It.get(signalGroupId1l) + ", "
				+ totalSignalGreenTimes0It.get(signalGroupId2));
		log.info("avg signal green times per cycle in itertion 0: " + avgSignalGreenTimePerCycle0It.get(signalGroupId1) + ", " + avgSignalGreenTimePerCycle0It.get(signalGroupId1l) + ", "
				+ avgSignalGreenTimePerCycle0It.get(signalGroupId2));
		log.info("avg cycle time per system in itertion 0: " + avgCycleTimePerSystem0It.get(signalSystemId));
		log.info("avg delay per link in itertion 0: " + avgDelayWE0It + ", " + avgDelayNS0It);


		Fixture fixture1It = new Fixture(500, 2000, 5.0, Regime.COMBINED);
		int lastIt = 1;
		fixture1It.setLastIteration(lastIt);
		fixture1It.addLeftTurnTraffic();
		SignalAnalysisTool signalAnalyzer1It = new SignalAnalysisTool();
		DelayAnalysisTool generalAnalyzer1It = fixture1It.run(signalAnalyzer1It);

		Map<Id<SignalGroup>, Double> totalSignalGreenTimes1It = signalAnalyzer1It.getTotalSignalGreenTime();
		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycle1It = signalAnalyzer1It.calculateAvgSignalGreenTimePerFlexibleCycle();
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystem1It = signalAnalyzer1It.calculateAvgFlexibleCycleTimePerSignalSystem();
		Map<Id<Link>, Double> avgDelayPerLink1It = generalAnalyzer1It.getAvgDelayPerLink();
		Double avgDelayWE1It = avgDelayPerLink1It.get(Id.createLinkId("2_3"));
		Double avgDelayNS1It = avgDelayPerLink1It.get(Id.createLinkId("7_3"));

		log.info("total signal green times in itertion 1: " + totalSignalGreenTimes1It.get(signalGroupId1) + ", " + totalSignalGreenTimes1It.get(signalGroupId1l) + ", "
				+ totalSignalGreenTimes1It.get(signalGroupId2));
		log.info("avg signal green times per cycle in itertion 1: " + avgSignalGreenTimePerCycle1It.get(signalGroupId1) + ", " + avgSignalGreenTimePerCycle1It.get(signalGroupId1l) + ", "
				+ avgSignalGreenTimePerCycle1It.get(signalGroupId2));
		log.info("avg cycle time per system in itertion 1: " + avgCycleTimePerSystem1It.get(signalSystemId));
		log.info("avg delay per link in itertion 1: " + avgDelayWE1It + ", " + avgDelayNS1It);


		Assertions.assertEquals(totalSignalGreenTimes0It.get(signalGroupId1), totalSignalGreenTimes1It.get(signalGroupId1), MatsimTestUtils.EPSILON, "total green time of signal group 1 should be the same as in the first iteration");
		Assertions.assertEquals(totalSignalGreenTimes0It.get(signalGroupId1l), totalSignalGreenTimes1It.get(signalGroupId1l), MatsimTestUtils.EPSILON, "total green time of signal group 1l should be the same as in the first iteration");
		Assertions.assertEquals(totalSignalGreenTimes0It.get(signalGroupId2), totalSignalGreenTimes1It.get(signalGroupId2), MatsimTestUtils.EPSILON, "total green time of signal group 2 should be the same as in the first iteration");
		Assertions.assertEquals(avgSignalGreenTimePerCycle0It.get(signalGroupId1), avgSignalGreenTimePerCycle1It.get(signalGroupId1), .01, "avg green time of signal group 1 should be the same as in the first iteration");
		Assertions.assertEquals(avgSignalGreenTimePerCycle0It.get(signalGroupId1l), avgSignalGreenTimePerCycle1It.get(signalGroupId1l), .01, "avg green time of signal group 1l should be the same as in the first iteration");
		Assertions.assertEquals(avgSignalGreenTimePerCycle0It.get(signalGroupId2), avgSignalGreenTimePerCycle1It.get(signalGroupId2), .01, "avg green time of signal group 2 should be the same as in the first iteration");
		Assertions.assertEquals(avgCycleTimePerSystem0It.get(signalSystemId), avgCycleTimePerSystem1It.get(signalSystemId), .01, "avg cycle time should be the same as in the first iteration");
		Assertions.assertEquals(avgDelayWE0It, avgDelayWE1It, .01, "avg delay in direction WE should be the same as in the first iteration");
		Assertions.assertEquals(avgDelayNS0It, avgDelayNS1It, .01, "avg delay in direction NS should be the same as in the first iteration");
		// compare signal event files
		long checksum_it0 = CRCChecksum.getCRCFromFile(testUtils.getOutputDirectory() + "ITERS/it.0/signalEvents2Via.csv");
		long checksum_itLast = CRCChecksum.getCRCFromFile(testUtils.getOutputDirectory() + "ITERS/it."+lastIt+"/signalEvents2Via.csv");
		Assertions.assertEquals(checksum_it0, checksum_itLast, "Signal events are different");
	}

	// TODO test stochasticity (laemmer better than fixed-time; different than for constant demand)
	// TODO test temporarily overcrowded situations (no exeption; signal is able to resolve congestion; like fixed-time schedule; cycle times get longer when overload continues)
	// TODO test liveArrivalRate (with/without time buckets) vs. exact data (the second results in more precise green times?!; liveArrivalRates are determined correctly)
	// TODO test grouping
	// TODO test lanes
	// ...

	private class Fixture {

		private Scenario scenario;
		private double flowNS;
		private double flowWE;

		Fixture(double flowNS, double flowWE, double minG, Regime regime) {
			this.flowWE = flowWE;
			this.flowNS = flowNS;

			// create Config
			Config config = ConfigUtils.loadConfig("./examples/tutorial/singleCrossingScenario/config.xml");
			config.plans().setInputFile(null);
			config.controller().setOutputDirectory(testUtils.getOutputDirectory());

			LaemmerConfigGroup laemmerConfigGroup = ConfigUtils.addOrGetModule(config,
					LaemmerConfigGroup.GROUP_NAME, LaemmerConfigGroup.class);
			laemmerConfigGroup.setMinGreenTime(minG);
			laemmerConfigGroup.setActiveRegime(regime);
			laemmerConfigGroup.setDesiredCycleTime(60);
			laemmerConfigGroup.setMaxCycleTime(90);

			// create scenario
			scenario = ScenarioUtils.loadScenario( config ) ;
			scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());
			// create a population
			createSimplePopulationWithoutLeftTurns(scenario.getPopulation(), flowNS, flowWE, "-1");
			// modify lanes (such that only one lane leads straight with a capacity of 3600 veh. this make especially the flow capacity test more clear.)
			LanesToLinkAssignment lanesOfLink23 = scenario.getLanes().getLanesToLinkAssignments().get(Id.createLinkId("2_3"));
			LanesToLinkAssignment lanesOfLink43 = scenario.getLanes().getLanesToLinkAssignments().get(Id.createLinkId("4_3"));
			lanesOfLink23.getLanes().get(Id.create("2_3.r", Lane.class)).getToLinkIds().remove(Id.createLinkId("3_4"));
			lanesOfLink23.getLanes().get(Id.create("2_3.r", Lane.class)).setCapacityVehiclesPerHour(0);
			lanesOfLink23.getLanes().get(Id.create("2_3.s", Lane.class)).setCapacityVehiclesPerHour(3600);
			lanesOfLink43.getLanes().get(Id.create("4_3.r", Lane.class)).getToLinkIds().remove(Id.createLinkId("3_2"));
			lanesOfLink43.getLanes().get(Id.create("4_3.r", Lane.class)).setCapacityVehiclesPerHour(0);
			lanesOfLink43.getLanes().get(Id.create("4_3.s", Lane.class)).setCapacityVehiclesPerHour(3600);
		}

		void doublePopulation() {
			scenario.getConfig().qsim().setFlowCapFactor(2.0);
			createSimplePopulationWithoutLeftTurns(scenario.getPopulation(), flowNS, flowWE, "-2");
		}

		void addLeftTurnTraffic() {
			String[] linksWEleft = {"1_2-6_7", "5_4-8_9"};
	        createPopulationForRelation(0.1 * flowWE, scenario.getPopulation(), linksWEleft, "-left");
		}

		void setLastIteration(int lastIteration) {
			scenario.getConfig().controller().setLastIteration(lastIteration);
			// add replanning strategy
			StrategySettings strat = new StrategySettings();
			strat.setStrategyName(DefaultSelector.KeepLastSelected.toString());
			strat.setWeight(1.);
			scenario.getConfig().replanning().addStrategySettings(strat);
		}

		DelayAnalysisTool run(SignalAnalysisTool signalAnalyzer) {
			Controler controler = new Controler( scenario );

			// add the signals module
//			controler.addOverridingModule(new SignalsModule());
			Signals.configure( controler );

			// add signal analysis tool
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					this.addEventHandlerBinding().toInstance(signalAnalyzer);
					this.addControlerListenerBinding().toInstance(signalAnalyzer);
				}
			});
			// add general analysis tools
			DelayAnalysisTool delayAnalysis = new DelayAnalysisTool(controler.getScenario().getNetwork());
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					this.addEventHandlerBinding().toInstance(delayAnalysis);
				}
			});

			// run the simulation
			controler.run();

			return delayAnalysis;
		}

		private void createSimplePopulationWithoutLeftTurns(Population pop, double flowNS, double flowWE, String idPostFix) {
	        String[] linksNS = {"6_7-8_9", "9_8-7_6"};
	        String[] linksWE = {"5_4-2_1", "1_2-4_5"};

	        createPopulationForRelation(flowNS, pop, linksNS, idPostFix);
	        createPopulationForRelation(flowWE, pop, linksWE, idPostFix);
	    }

		private void createPopulationForRelation(double flow, Population population, String[] links, String idPostFix) {
			if (flow == 0) {
				return;
			}

			for (String od : links) {
				String fromLinkId = od.split("-")[0];
				String toLinkId = od.split("-")[1];

				double nthSecond = (3600 / flow);
				for (double i = 0; i < 5400; i += nthSecond) {
					createPerson(population, fromLinkId, toLinkId, i, Id.createPersonId(od + "_" + i + idPostFix));
				}
			}
	    }

		private void createPerson(Population population, String fromLinkId, String toLinkId, double time, Id<Person> id) {
			Person person = population.getFactory().createPerson(id);
			population.addPerson(person);

			// create a plan for the person that contains all this information
			Plan plan = population.getFactory().createPlan();
			person.addPlan(plan);

			// create a start activity at the from link
			Activity startAct = population.getFactory().createActivityFromLinkId("dummy",
					Id.createLinkId(fromLinkId));
			// distribute agents uniformly during one hour.
			startAct.setEndTime(time);
			plan.addActivity(startAct);

			// create a dummy leg
			plan.addLeg(population.getFactory().createLeg(TransportMode.car));

			// create a drain activity at the to link
			Activity drainAct = population.getFactory().createActivityFromLinkId("dummy",
					Id.createLinkId(toLinkId));
			plan.addActivity(drainAct);
		}

	}

}
