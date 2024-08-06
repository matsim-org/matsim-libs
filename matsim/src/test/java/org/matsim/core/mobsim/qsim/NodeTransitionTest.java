/* *********************************************************************** *
 * project: org.matsim.*
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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.NodeTransition;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.PrepareForSimUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * Test the different node transition options that one can choose in the qsim config group.
 * EmptyBufferAfterBuffer and blockNodeWhenSingleOutlinkFull = false currently is the MATSim standard.
 *
 * This test checks the throughput of links in two different scenarios:
 * 1. A merge situation, where agents coming from three different links all want to enter the same downstream link with restricted capacity.
 * 2. An intersection, where agents coming from two different links do not use same links but might affect each other when the flag blockNodeWhenSingleOutlinkFull is set to true.
 *
 * While implementing and significantly testing the new node transitions some bugs have been found in QueueWithBuffer regarding half empty buffers and time step sizes < 1.
 * This tests therefore also validates the bug fixes (see e.g. testNodeTransitionWithTimeStepSizeSmallerOne).
 *
 * The results should be the same independently of slow/fast capacity update. That's why the test is run for both (parameterized).
 *
 * @author tthunig
 *
 */
public class NodeTransitionTest {

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testMergeSituationWithEmptyBufferAfterBufferRandomDistribution(boolean useFastCapUpdate) {
		Scenario scenario = Fixture.createMergeScenario();
		scenario.getConfig().qsim().setNodeTransitionLogic(NodeTransition.emptyBufferAfterBufferRandomDistribution_dontBlockNode);
		scenario.getConfig().qsim().setUsingFastCapacityUpdate(useFastCapUpdate);

		EventsManager events = EventsUtils.createEventsManager();
		List<Id<Link>> linksOfInterest = new LinkedList<>();
		linksOfInterest.add(Id.createLinkId("2_7"));
		linksOfInterest.add(Id.createLinkId("4_7"));
		linksOfInterest.add(Id.createLinkId("6_7"));
		ThroughputAnalyzer throughputAnalyzer = new ThroughputAnalyzer(linksOfInterest);
		events.addHandler(throughputAnalyzer);

		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();

		new QSimBuilder(scenario.getConfig())
			.useDefaults()
			.build(scenario, events)
			.run();

		// this is the time before the downstream link (7_8) gets full and with that no inflow capacity is valid
		int timeInterval1Start = 110; int timeInterval1End = 180;
		Map<Id<Link>, Double> avgThroughputFreeFlow = throughputAnalyzer.calculateAvgThroughputPerTimeStepOfTimeInterval(timeInterval1Start, timeInterval1End, 1);
		// this is the time when the downstream link (7_8) is already full and only 2 vehicles per second can enter but the third commodity has not started sending vehicles yet
		int timeInterval2Start = 210; int timeInterval2End = 340;
		Map<Id<Link>, Double> avgThroughputCongestedTwoLinks = throughputAnalyzer.calculateAvgThroughputPerTimeStepOfTimeInterval(timeInterval2Start, timeInterval2End, 1);
		// this is the time when all three commodities send vehicles and the downstream link (7_8) is still full and only 2 vehicles per second can enter
		int timeInterval3Start = 360; int timeInterval3End = 500;
		Map<Id<Link>, Double> avgThroughputCongestedThreeLinks = throughputAnalyzer.calculateAvgThroughputPerTimeStepOfTimeInterval(timeInterval3Start, timeInterval3End, 1);
		for (Id<Link> link : linksOfInterest) {
			System.out.println("avgThroughput link " + link + " time interval [" + timeInterval1Start + ", " + timeInterval1End + "]: " + avgThroughputFreeFlow.get(link)
					+ "\t; time interval [" + timeInterval2Start + ", " + timeInterval2End + "]: " + avgThroughputCongestedTwoLinks.get(link)
					+ "\t; time interval [" + timeInterval3Start + ", " + timeInterval3End + "]: " + avgThroughputCongestedThreeLinks.get(link));
		}

		/* since this node dynamic is a random distribution we allow a somewhat bigger difference here.
		 * otherwise we would need to run the test for a longer period or different random seeds which would increase the run time of this test.
		 */
		double delta = 0.1;

		// test throughput for the first time interval
		/* the downstream link is not full, i.e. the links can send vehicles with their full outflow capacity.
		 * the commodity on link 6_7 has not begun to send vehicles, i.e. the corresponding throughput should be 0 */
		Assertions.assertEquals(1, avgThroughputFreeFlow.get(Id.createLinkId("2_7")), MatsimTestUtils.EPSILON, "Troughput on link 2_7 is wrong");
		Assertions.assertEquals(2, avgThroughputFreeFlow.get(Id.createLinkId("4_7")), MatsimTestUtils.EPSILON, "Troughput on link 4_7 is wrong");
		Assertions.assertEquals(0, avgThroughputFreeFlow.get(Id.createLinkId("6_7")), MatsimTestUtils.EPSILON, "Troughput on link 6_7 is wrong");

		// test throughput for the second time interval
		/* with probability of 2/3 link 4_7 is selected and sends all vehicles from its buffer, i.e. 2;
		 * with probability of 1/3 link 2_7 is selected and sends all vehicles from its buffer, i.e. 1 and afterwards link 4_7 can send the remaining 1.
		 * this means, the expected average throughput of link 2_7 is 1/3 and the expected average throughput of link 4_7 is 2*2/3 + 1/3 = 5/3.
		 */
		Assertions.assertEquals(1./3, avgThroughputCongestedTwoLinks.get(Id.createLinkId("2_7")), delta, "Troughput on link 2_7 is wrong"); // 0.25384615384615383
		Assertions.assertEquals(5./3, avgThroughputCongestedTwoLinks.get(Id.createLinkId("4_7")), delta, "Troughput on link 4_7 is wrong"); // 1.7461538461538462
		Assertions.assertEquals(0, avgThroughputCongestedTwoLinks.get(Id.createLinkId("6_7")), MatsimTestUtils.EPSILON, "Troughput on link 6_7 is wrong");

		// test throughput for the third time interval
		/* with probability of 2/5 link 4_7 is selected and sends all vehicles from its buffer, i.e. 2;
		 * with probability of 2/5 link 6_7 is selected and sends all vehicles from its buffer, i.e. 2;
		 * with probability of 1/5 link 2_7 is selected and sends all vehicles from its buffer, i.e. 1 and afterwards link 4_7 or link 6_7 can send the remaining 1.
		 * this means, the expected average throughput of link 2_7 is 1/5 and the expected average throughput of link 4_7 and link 6_7 is 2*2/5 + 1/10 = 9/10.
		 */
		Assertions.assertEquals(1./5, avgThroughputCongestedThreeLinks.get(Id.createLinkId("2_7")), delta, "Troughput on link 2_7 is wrong"); // 0.17857142857142858
		Assertions.assertEquals(9./10, avgThroughputCongestedThreeLinks.get(Id.createLinkId("4_7")), delta, "Troughput on link 4_7 is wrong"); // 0.8928571428571429
		Assertions.assertEquals(9./10, avgThroughputCongestedThreeLinks.get(Id.createLinkId("6_7")), delta, "Troughput on link 6_7 is wrong"); // 0.9285714285714286
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testMergeSituationWithMoveVehByVehRandomDistribution(boolean useFastCapUpdate) {
		Scenario scenario = Fixture.createMergeScenario();
		scenario.getConfig().qsim().setNodeTransitionLogic(NodeTransition.moveVehByVehRandomDistribution_dontBlockNode);
		scenario.getConfig().qsim().setUsingFastCapacityUpdate(useFastCapUpdate);

		EventsManager events = EventsUtils.createEventsManager();
		List<Id<Link>> linksOfInterest = new LinkedList<>();
		linksOfInterest.add(Id.createLinkId("2_7"));
		linksOfInterest.add(Id.createLinkId("4_7"));
		linksOfInterest.add(Id.createLinkId("6_7"));
		ThroughputAnalyzer throughputAnalyzer = new ThroughputAnalyzer(linksOfInterest);
		events.addHandler(throughputAnalyzer);

		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();

		new QSimBuilder(scenario.getConfig())
			.useDefaults()
			.build(scenario, events)
			.run();

		// this is the time before the downstream link (7_8) gets full and with that no inflow capacity is valid
		int timeInterval1Start = 110; int timeInterval1End = 180;
		Map<Id<Link>, Double> avgThroughputFreeFlow = throughputAnalyzer.calculateAvgThroughputPerTimeStepOfTimeInterval(timeInterval1Start, timeInterval1End, 1);
		// this is the time when the downstream link (7_8) is already full and only 2 vehicles per second can enter but the third commodity has not started sending vehicles yet
		int timeInterval2Start = 210; int timeInterval2End = 340;
		Map<Id<Link>, Double> avgThroughputCongestedTwoLinks = throughputAnalyzer.calculateAvgThroughputPerTimeStepOfTimeInterval(timeInterval2Start, timeInterval2End, 1);
		// this is the time when all three commodities send vehicles and the downstream link (7_8) is still full and only 2 vehicles per second can enter
		int timeInterval3Start = 360; int timeInterval3End = 500;
		Map<Id<Link>, Double> avgThroughputCongestedThreeLinks = throughputAnalyzer.calculateAvgThroughputPerTimeStepOfTimeInterval(timeInterval3Start, timeInterval3End, 1);
		for (Id<Link> link : linksOfInterest) {
			System.out.println("avgThroughput link " + link + " time interval [" + timeInterval1Start + ", " + timeInterval1End + "]: " + avgThroughputFreeFlow.get(link)
					+ "\t; time interval [" + timeInterval2Start + ", " + timeInterval2End + "]: " + avgThroughputCongestedTwoLinks.get(link)
					+ "\t; time interval [" + timeInterval3Start + ", " + timeInterval3End + "]: " + avgThroughputCongestedThreeLinks.get(link));
		}

		/* since this node dynamic is a random distribution we allow a somewhat bigger difference here.
		 * otherwise we would need to run the test for a longer period or different random seeds which would increase the run time of this test.
		 * note, that these random values can even differ when all tests are run after each other or when they are run separately
		 */
		double delta = 0.04;

		// test throughput for the first time interval
		/* the downstream link is not full, i.e. the links can send vehicles with their full outflow capacity.
		 * the commodity on link 6_7 has not begun to send vehicles, i.e. the corresponding throughput should be 0 */
		Assertions.assertEquals(1, avgThroughputFreeFlow.get(Id.createLinkId("2_7")), MatsimTestUtils.EPSILON, "Troughput on link 2_7 is wrong");
		Assertions.assertEquals(2, avgThroughputFreeFlow.get(Id.createLinkId("4_7")), MatsimTestUtils.EPSILON, "Troughput on link 4_7 is wrong");
		Assertions.assertEquals(0, avgThroughputFreeFlow.get(Id.createLinkId("6_7")), MatsimTestUtils.EPSILON, "Troughput on link 6_7 is wrong");

		// test throughput for the second time interval
		/* with probability of 2/3 link 4_7 is selected and sends one vehicle; afterwards with probability of 2/3 link 4_7 is allowed to send the second vehicle, with probability 1/3 link 2_7 is allowed to send one.
		 * with probability of 1/3 link 2_7 is selected and sends one vehicle; afterwards link 4_7 will send the remaining one, because the buffer of link 2_7 is empty anyway.
		 * this means, the expected average throughput of link 2_7 will get underestimated because the random distribution is cut at one and will never overestimate above this number.
		 * consequently, the expected average throughput of link 4_7 will get overestimated.
		 * the numbers are as follows: the expected average throughput of link 2_7 is 2/3*1/3*1 + 1/3*1 = 5/9
		 * and the expected average throughput of link 4_7 is 2/3*2/3*2 + 2/3*1/3*1 + 1/3*1 = 13/9
		 */
		Assertions.assertEquals(5./9, avgThroughputCongestedTwoLinks.get(Id.createLinkId("2_7")), delta, "Troughput on link 2_7 is wrong"); // 0.5307692307692308
		Assertions.assertEquals(13./9, avgThroughputCongestedTwoLinks.get(Id.createLinkId("4_7")), delta, "Troughput on link 4_7 is wrong"); // 1.4692307692307693
		Assertions.assertEquals(0, avgThroughputCongestedTwoLinks.get(Id.createLinkId("6_7")), MatsimTestUtils.EPSILON, "Troughput on link 6_7 is wrong");

		// test throughput for the third time interval
		/* the first vehicle is send by link 2_7 with probability of 1/5 and by one of the other two links with probability 2/5.
		 * if link 4_7 or 6_7 have send the first vehicle, the same probability will hold for the second vehicle.
		 * if link 2_7 has send the first vehicle, its buffer is empty afterwards, i.e. the second vehicle is send by either link 4_7 or link 6_7 with equal probability.
		 * this means, the expected average throughput of link 2_7 will get underestimated because the random distribution is cut at one and will never overestimate above this number.
		 * consequently, the expected average throughput of link 4_7 and 6_7 will get overestimated.
		 * the numbers are as follows: the expected average throughput of link 2_7 is 1/5*1/2*1 + 1/5*1/2*1 + 2/5*1/5*1 + 2/5*1/5*1 = 9/25 = 0.36
		 * and the expected average throughput of link 4_7 and 6_7 is 1/5*1/2*1 + 2/5*2/5*2 + 2/5*1/5*1 + 2/5*2/5*1 + 2/5*2/5*1 = 41/50
		 */
		Assertions.assertEquals(9./25, avgThroughputCongestedThreeLinks.get(Id.createLinkId("2_7")), delta, "Troughput on link 2_7 is wrong"); // 0.34285714285714286
		Assertions.assertEquals(41./50, avgThroughputCongestedThreeLinks.get(Id.createLinkId("4_7")), delta, "Troughput on link 4_7 is wrong"); // 0.8
		Assertions.assertEquals(41./50, avgThroughputCongestedThreeLinks.get(Id.createLinkId("6_7")), delta, "Troughput on link 6_7 is wrong"); // 0.8571428571428571
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testMergeSituationWithMoveVehByVehDeterministicPriorities(boolean useFastCapUpdate) {
		Scenario scenario = Fixture.createMergeScenario();
		scenario.getConfig().qsim().setNodeTransitionLogic(NodeTransition.moveVehByVehDeterministicPriorities_nodeBlockedWhenSingleOutlinkFull);
		// note: the deterministic node transition is only implemented for the case when the node is blocked as soon as one outgoing link is full
		scenario.getConfig().qsim().setUsingFastCapacityUpdate(useFastCapUpdate);

		EventsManager events = EventsUtils.createEventsManager();
		List<Id<Link>> linksOfInterest = new LinkedList<>();
		linksOfInterest.add(Id.createLinkId("2_7"));
		linksOfInterest.add(Id.createLinkId("4_7"));
		linksOfInterest.add(Id.createLinkId("6_7"));
		ThroughputAnalyzer throughputAnalyzer = new ThroughputAnalyzer(linksOfInterest);
		events.addHandler(throughputAnalyzer);

		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();

		new QSimBuilder(scenario.getConfig())
			.useDefaults()
			.build(scenario, events)
			.run();

		// this is the time before the downstream link (7_8) gets full and with that no inflow capacity is valid
		int timeInterval1Start = 110; int timeInterval1End = 180;
		Map<Id<Link>, Double> avgThroughputFreeFlow = throughputAnalyzer.calculateAvgThroughputPerTimeStepOfTimeInterval(timeInterval1Start, timeInterval1End, 1);
		// this is the time when the downstream link (7_8) is already full and only 2 vehicles per second can enter but the third commodity has not started sending vehicles yet
		int timeInterval2Start = 210; int timeInterval2End = 340;
		Map<Id<Link>, Double> avgThroughputCongestedTwoLinks = throughputAnalyzer.calculateAvgThroughputPerTimeStepOfTimeInterval(timeInterval2Start, timeInterval2End, 1);
		// this is the time when all three commodities send vehicles and the downstream link (7_8) is still full and only 2 vehicles per second can enter
		int timeInterval3Start = 360; int timeInterval3End = 500;
		Map<Id<Link>, Double> avgThroughputCongestedThreeLinks = throughputAnalyzer.calculateAvgThroughputPerTimeStepOfTimeInterval(timeInterval3Start, timeInterval3End, 1);
		for (Id<Link> link : linksOfInterest) {
			System.out.println("avgThroughput link " + link + " time interval [" + timeInterval1Start + ", " + timeInterval1End + "]: " + avgThroughputFreeFlow.get(link)
					+ "\t; time interval [" + timeInterval2Start + ", " + timeInterval2End + "]: " + avgThroughputCongestedTwoLinks.get(link)
					+ "\t; time interval [" + timeInterval3Start + ", " + timeInterval3End + "]: " + avgThroughputCongestedThreeLinks.get(link));
		}

		/* since this node dynamic does not rely on a random distribution but updates link priorities each time step,
		 * i.e. remembers decisions made in previous time steps,
		 * the difference to the expected values should be much smaller compared to the other node transition logic.
		 * still, for periodic numbers a higher accuracy would only be reached by longer simulation times
		 */
		double delta = MatsimTestUtils.EPSILON;
		double deltaPeriodic = 0.01;

		// test throughput for the first time interval
		/* the downstream link is not full, i.e. the links can send vehicles with their full outflow capacity.
		 * the commodity on link 6_7 has not begun to send vehicles, i.e. the corresponding throughput should be 0 */
		Assertions.assertEquals(1, avgThroughputFreeFlow.get(Id.createLinkId("2_7")), delta, "Troughput on link 2_7 is wrong");
		Assertions.assertEquals(2, avgThroughputFreeFlow.get(Id.createLinkId("4_7")), delta, "Troughput on link 4_7 is wrong");
		Assertions.assertEquals(0, avgThroughputFreeFlow.get(Id.createLinkId("6_7")), delta, "Troughput on link 6_7 is wrong");

		// test throughput for the second time interval
		/* the deterministic node transition should distribute the free slots on the downstream link exactly proportional to the outflow capacity of the links,
		 * i.e. 1:2 in this case
		 */
		Assertions.assertEquals(1./3 * 2, avgThroughputCongestedTwoLinks.get(Id.createLinkId("2_7")), deltaPeriodic, "Troughput on link 2_7 is wrong"); // 0.6692307692307692
		Assertions.assertEquals(2./3 * 2, avgThroughputCongestedTwoLinks.get(Id.createLinkId("4_7")), deltaPeriodic, "Troughput on link 4_7 is wrong"); // 1.3307692307692307
		Assertions.assertEquals(0, avgThroughputCongestedTwoLinks.get(Id.createLinkId("6_7")), delta, "Troughput on link 6_7 is wrong");

		// test throughput for the third time interval
		/* the deterministic node transition should distribute the free slots on the downstream link exactly proportional to the outflow capacity of the links,
		 * i.e. 1:2:2 in this case.
		 */
		Assertions.assertEquals(1./5 * 2, avgThroughputCongestedThreeLinks.get(Id.createLinkId("2_7")), delta, "Troughput on link 2_7 is wrong"); // 0.4
		Assertions.assertEquals(2./5 * 2, avgThroughputCongestedThreeLinks.get(Id.createLinkId("4_7")), delta, "Troughput on link 4_7 is wrong"); // 0.8
		Assertions.assertEquals(2./5 * 2, avgThroughputCongestedThreeLinks.get(Id.createLinkId("6_7")), delta, "Troughput on link 6_7 is wrong"); // 0.8
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testBlockedNodeSituationWithEmptyBufferAfterBufferRandomDistribution(boolean useFastCapUpdate) {
		Scenario scenario = Fixture.createBlockedNodeScenario();
		scenario.getConfig().qsim().setNodeTransitionLogic(NodeTransition.emptyBufferAfterBufferRandomDistribution_nodeBlockedWhenSingleOutlinkFull);
		scenario.getConfig().qsim().setUsingFastCapacityUpdate(useFastCapUpdate);
		scenario.getConfig().routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);

		EventsManager events = EventsUtils.createEventsManager();
		List<Id<Link>> linksOfInterest = new LinkedList<>();
		linksOfInterest.add(Id.createLinkId("2_5"));
		linksOfInterest.add(Id.createLinkId("4_5"));
		linksOfInterest.add(Id.createLinkId("5_8"));
		ThroughputAnalyzer throughputAnalyzer = new ThroughputAnalyzer(linksOfInterest);
		events.addHandler(throughputAnalyzer);

		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();

		new QSimBuilder(scenario.getConfig())
			.useDefaults()
			.build(scenario, events)
			.run();

//		// output for debugging
//		for (int start = 0; start <= 800; start+=10) {
//			Map<Id<Link>, Double> avgThroughputThisInterval = throughputAnalyzer.calculateAvgThroughputPerTimeStepOfTimeInterval(start, start+10, 1);
//			System.out.println("[" + start + ", " + (start+10) + "]. link 2_5: " + avgThroughputThisInterval.get(Id.createLinkId("2_5"))
//					+ ", link 4_5: " + avgThroughputThisInterval.get(Id.createLinkId("4_5"))
//					+ ", link 5_8: " + avgThroughputThisInterval.get(Id.createLinkId("5_8")));
//		}
//		Map<Id<Link>, Double> avgThroughputUntil210 = throughputAnalyzer.calculateAvgThroughputPerTimeStepOfTimeInterval(0, 210, 1);
//		System.out.println("absolute throughput [" + 0 + ", " + 210 + "]. link 2_5: " + (avgThroughputUntil210.get(Id.createLinkId("2_5")) * 210 / 1)
//				+ ", link 4_5: " + (avgThroughputUntil210.get(Id.createLinkId("4_5")) * 210 / 1) );

		// this is the time before the downstream link (5_8) gets full and with that no inflow capacity is valid and no node is blocked
		int timeInterval1Start = 110; int timeInterval1End = 200;
		Map<Id<Link>, Double> avgThroughputFreeFlow = throughputAnalyzer.calculateAvgThroughputPerTimeStepOfTimeInterval(timeInterval1Start, timeInterval1End, 1);
		// this is the time when the downstream link (5_8) is full and also no vehicles are leaving this link such that no vehicles can enter and the node stays blocked
		int timeInterval2Start = 210; int timeInterval2End = 300;
		Map<Id<Link>, Double> avgThroughputCongestedNodeBlocked = throughputAnalyzer.calculateAvgThroughputPerTimeStepOfTimeInterval(timeInterval2Start, timeInterval2End, 1);
		// this is the time when one vehicle per second is allowed to enter 5_8, i.e. 2_5 is restricted to half of its outflow capacity
		int timeInterval3Start = 310; int timeInterval3End = 500;
		Map<Id<Link>, Double> avgThroughputCongestedRestrictFlow = throughputAnalyzer.calculateAvgThroughputPerTimeStepOfTimeInterval(timeInterval3Start, timeInterval3End, 1);
		for (Id<Link> link : linksOfInterest) {
			System.out.println("avgThroughput link " + link + " time interval [" + timeInterval1Start + ", " + timeInterval1End + "]: " + avgThroughputFreeFlow.get(link)
					+ "\t; time interval [" + timeInterval2Start + ", " + timeInterval2End + "]: " + avgThroughputCongestedNodeBlocked.get(link)
					+ "\t; time interval [" + timeInterval3Start + ", " + timeInterval3End + "]: " + avgThroughputCongestedRestrictFlow.get(link));
		}

		/* since this node dynamic is a random distribution we allow a somewhat bigger difference here.
		 * otherwise we would need to run the test for a longer period or different random seeds which would increase the run time of this test.
		 */
		double delta = 0.04;

		// test throughput for the first time interval
		/* the downstream link is not full, i.e. the links can send vehicles with their full outflow capacity.
		 * the first vehicles reach the end of link 5_8 around sec 300, i.e. throughput of link 5_8 should be zero here.
		 */
		Assertions.assertEquals(2, avgThroughputFreeFlow.get(Id.createLinkId("2_5")), MatsimTestUtils.EPSILON, "Troughput on link 2_5 is wrong");
		Assertions.assertEquals(1, avgThroughputFreeFlow.get(Id.createLinkId("4_5")), MatsimTestUtils.EPSILON, "Troughput on link 4_5 is wrong");
		Assertions.assertEquals(0, avgThroughputFreeFlow.get(Id.createLinkId("5_8")), MatsimTestUtils.EPSILON, "Troughput on link 5_8 is wrong");

		// test throughput for the second time interval
		/* with probability 1/3 link 4_5 is selected first and can send all its vehicles from the buffer (i.e. one) before link 2_5 blocks the intersection.
		 * if link 2_5 is selected first the intersection is blocked immediately.
		 * the first vehicles reach the end of link 5_8 around sec 300, i.e. throughput of link 5_8 should be zero here.
		 */
		Assertions.assertEquals(0, avgThroughputCongestedNodeBlocked.get(Id.createLinkId("2_5")), MatsimTestUtils.EPSILON, "Troughput on link 2_5 is wrong");
		Assertions.assertEquals(1./3, avgThroughputCongestedNodeBlocked.get(Id.createLinkId("4_5")), delta, "Troughput on link 4_5 is wrong"); // 0.36666666666666664
		Assertions.assertEquals(0, avgThroughputCongestedNodeBlocked.get(Id.createLinkId("5_8")), MatsimTestUtils.EPSILON, "Troughput on link 5_8 is wrong");

		// test throughput for the third time interval
		/* with probability 1/3 link 4_5 is selected first and can send all its vehicles from the buffer (i.e. one) before link 2_5 sends its first one and blocks the intersection.
		 * with probability 2/3 link 2_5 is selected first, sends one vehicle and thereby blocks the intersection.
		 * this means the correct expected throughput values are: 1/3*1 for link 4_5 and 1/3*1 + 2/3*1 = 1 for link 2_5.
		 * the first vehicles reach the end of link 5_8 around sec 300, i.e. throughput of link 5_8 should be equal to the flow
		 * capacity of link 5_8 here, i.e. 1 veh per time step (=sec).
		 */
		Assertions.assertEquals(1, avgThroughputCongestedRestrictFlow.get(Id.createLinkId("2_5")), MatsimTestUtils.EPSILON, "Troughput on link 2_5 is wrong");
		Assertions.assertEquals(1./3, avgThroughputCongestedRestrictFlow.get(Id.createLinkId("4_5")), delta, "Troughput on link 4_5 is wrong"); // 0.3263157894736842
		Assertions.assertEquals(1, avgThroughputCongestedRestrictFlow.get(Id.createLinkId("5_8")), MatsimTestUtils.EPSILON, "Troughput on link 5_8 is wrong");
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testBlockedNodeSituationWithMoveVehByVehRandomDistribution(boolean useFastCapUpdate) {
		Scenario scenario = Fixture.createBlockedNodeScenario();
		scenario.getConfig().qsim().setNodeTransitionLogic(NodeTransition.moveVehByVehRandomDistribution_nodeBlockedWhenSingleOutlinkFull);
		scenario.getConfig().qsim().setUsingFastCapacityUpdate(useFastCapUpdate);
		scenario.getConfig().routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);

		EventsManager events = EventsUtils.createEventsManager();
		List<Id<Link>> linksOfInterest = new LinkedList<>();
		linksOfInterest.add(Id.createLinkId("2_5"));
		linksOfInterest.add(Id.createLinkId("4_5"));
		linksOfInterest.add(Id.createLinkId("5_8"));
		ThroughputAnalyzer throughputAnalyzer = new ThroughputAnalyzer(linksOfInterest);
		events.addHandler(throughputAnalyzer);

		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();

		new QSimBuilder(scenario.getConfig())
			.useDefaults()
			.build(scenario, events)
			.run();

//		// output for debugging
//		for (int start = 0; start <= 800; start+=10) {
//			Map<Id<Link>, Double> avgThroughputThisInterval = throughputAnalyzer.calculateAvgThroughputPerTimeStepOfTimeInterval(start, start+10, 1);
//			System.out.println("[" + start + ", " + (start+10) + "]. link 2_5: " + avgThroughputThisInterval.get(Id.createLinkId("2_5"))
//					+ ", link 4_5: " + avgThroughputThisInterval.get(Id.createLinkId("4_5"))
//					+ ", link 5_8: " + avgThroughputThisInterval.get(Id.createLinkId("5_8")));
//		}
//		Map<Id<Link>, Double> avgThroughputUntil210 = throughputAnalyzer.calculateAvgThroughputPerTimeStepOfTimeInterval(0, 210, 1);
//		System.out.println("absolute throughput [" + 0 + ", " + 210 + "]. link 2_5: " + (avgThroughputUntil210.get(Id.createLinkId("2_5")) * 210 / 1)
//				+ ", link 4_5: " + (avgThroughputUntil210.get(Id.createLinkId("4_5")) * 210 / 1) );

		// this is the time before the downstream link (5_8) gets full and with that no inflow capacity is valid and no node is blocked
		int timeInterval1Start = 110; int timeInterval1End = 200;
		Map<Id<Link>, Double> avgThroughputFreeFlow = throughputAnalyzer.calculateAvgThroughputPerTimeStepOfTimeInterval(timeInterval1Start, timeInterval1End, 1);
		// this is the time when the downstream link (5_8) is full and also no vehicles are leaving this link such that no vehicles can enter and the node stays blocked
		int timeInterval2Start = 210; int timeInterval2End = 300;
		Map<Id<Link>, Double> avgThroughputCongestedNodeBlocked = throughputAnalyzer.calculateAvgThroughputPerTimeStepOfTimeInterval(timeInterval2Start, timeInterval2End, 1);
		// this is the time when one vehicle per second is allowed to enter 5_8, i.e. 2_5 is restricted to half of its outflow capacity
		int timeInterval3Start = 310; int timeInterval3End = 500;
		Map<Id<Link>, Double> avgThroughputCongestedRestrictFlow = throughputAnalyzer.calculateAvgThroughputPerTimeStepOfTimeInterval(timeInterval3Start, timeInterval3End, 1);
		for (Id<Link> link : linksOfInterest) {
			System.out.println("avgThroughput link " + link + " time interval [" + timeInterval1Start + ", " + timeInterval1End + "]: " + avgThroughputFreeFlow.get(link)
					+ "\t; time interval [" + timeInterval2Start + ", " + timeInterval2End + "]: " + avgThroughputCongestedNodeBlocked.get(link)
					+ "\t; time interval [" + timeInterval3Start + ", " + timeInterval3End + "]: " + avgThroughputCongestedRestrictFlow.get(link));
		}

		/* since this node dynamic is a random distribution we allow a somewhat bigger difference here.
		 * otherwise we would need to run the test for a longer period or different random seeds which would increase the run time of this test.
		 */
		double delta = 0.02;

		// test throughput for the first time interval
		/* the downstream link is not full, i.e. the links can send vehicles with their full outflow capacity.
		 * the first vehicles reach the end of link 5_8 around sec 300, i.e. throughput of link 5_8 should be zero here.
		 */
		Assertions.assertEquals(2, avgThroughputFreeFlow.get(Id.createLinkId("2_5")), MatsimTestUtils.EPSILON, "Troughput on link 2_5 is wrong");
		Assertions.assertEquals(1, avgThroughputFreeFlow.get(Id.createLinkId("4_5")), MatsimTestUtils.EPSILON, "Troughput on link 4_5 is wrong");
		Assertions.assertEquals(0, avgThroughputFreeFlow.get(Id.createLinkId("5_8")), MatsimTestUtils.EPSILON, "Troughput on link 5_8 is wrong");

		// test throughput for the second time interval
		/* with probability 1/3 link 4_5 is selected first and can send one vehicle before link 2_5 blocks the intersection.
		 * if link 2_5 is selected first the intersection is blocked immediately.
		 * the first vehicles reach the end of link 5_8 around sec 300, i.e. throughput of link 5_8 should be zero here.
		 */
		Assertions.assertEquals(0, avgThroughputCongestedNodeBlocked.get(Id.createLinkId("2_5")), MatsimTestUtils.EPSILON, "Troughput on link 2_5 is wrong");
		Assertions.assertEquals(1./3, avgThroughputCongestedNodeBlocked.get(Id.createLinkId("4_5")), delta, "Troughput on link 4_5 is wrong"); // 0.3333333333333333
		Assertions.assertEquals(0, avgThroughputCongestedNodeBlocked.get(Id.createLinkId("5_8")), MatsimTestUtils.EPSILON, "Troughput on link 5_8 is wrong");

		// test throughput for the third time interval
		/* with probability 1/3 link 4_5 is selected first and can send one vehicle before link 2_5 sends its first one and blocks the intersection.
		 * with probability 2/3 link 2_5 is selected first and sends one vehicle;
		 * afterwards with probability 1/3 link 4_5 sends one vehicle before link 2_5 blocks the intersection, with probability 2/3 it is blocked immediately.
		 * this means the correct expected throughput values are: 1/3*1 + 2/3*1/3*1 = 5/9 for link 4_5 and 1/3*1 + 2/3*1 = 1 for link 2_5.
		 * the first vehicles reach the end of link 5_8 around sec 300, i.e. throughput of link 5_8 should be equal to the flow
		 * capacity of link 5_8 here, i.e. 1 veh per time step (=sec).
		 */
		Assertions.assertEquals(1, avgThroughputCongestedRestrictFlow.get(Id.createLinkId("2_5")), MatsimTestUtils.EPSILON, "Troughput on link 2_5 is wrong");
		Assertions.assertEquals(5./9, avgThroughputCongestedRestrictFlow.get(Id.createLinkId("4_5")), delta, "Troughput on link 4_5 is wrong"); // 0.5736842105263158
		Assertions.assertEquals(1, avgThroughputCongestedRestrictFlow.get(Id.createLinkId("5_8")), MatsimTestUtils.EPSILON, "Troughput on link 5_8 is wrong");
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testBlockedNodeSituationWithMoveVehByVehDeterministicPriorities(boolean useFastCapUpdate) {
		Scenario scenario = Fixture.createBlockedNodeScenario();
		scenario.getConfig().qsim().setNodeTransitionLogic(NodeTransition.moveVehByVehDeterministicPriorities_nodeBlockedWhenSingleOutlinkFull);
		scenario.getConfig().qsim().setUsingFastCapacityUpdate(useFastCapUpdate);
		scenario.getConfig().routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);

		EventsManager events = EventsUtils.createEventsManager();
		List<Id<Link>> linksOfInterest = new LinkedList<>();
		linksOfInterest.add(Id.createLinkId("2_5"));
		linksOfInterest.add(Id.createLinkId("4_5"));
		linksOfInterest.add(Id.createLinkId("5_8"));
		ThroughputAnalyzer throughputAnalyzer = new ThroughputAnalyzer(linksOfInterest);
		events.addHandler(throughputAnalyzer);

		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();

		new QSimBuilder(scenario.getConfig())
			.useDefaults()
			.build(scenario, events)
			.run();

//		// output for debugging
//		for (int start = 0; start <= 800; start+=10) {
//			Map<Id<Link>, Double> avgThroughputThisInterval = throughputAnalyzer.calculateAvgThroughputPerTimeStepOfTimeInterval(start, start+10, 1);
//			System.out.println("[" + start + ", " + (start+10) + "]. link 2_5: " + avgThroughputThisInterval.get(Id.createLinkId("2_5"))
//					+ ", link 4_5: " + avgThroughputThisInterval.get(Id.createLinkId("4_5"))
//					+ ", link 5_8: " + avgThroughputThisInterval.get(Id.createLinkId("5_8")));
//		}
//		Map<Id<Link>, Double> avgThroughputUntil210 = throughputAnalyzer.calculateAvgThroughputPerTimeStepOfTimeInterval(0, 210, 1);
//		System.out.println("absolute throughput [" + 0 + ", " + 210 + "]. link 2_5: " + (avgThroughputUntil210.get(Id.createLinkId("2_5")) * 210 / 1)
//				+ ", link 4_5: " + (avgThroughputUntil210.get(Id.createLinkId("4_5")) * 210 / 1) );

		// this is the time before the downstream link (5_8) gets full and with that no inflow capacity is valid and no node is blocked
		int timeInterval1Start = 110; int timeInterval1End = 200;
		Map<Id<Link>, Double> avgThroughputFreeFlow = throughputAnalyzer.calculateAvgThroughputPerTimeStepOfTimeInterval(timeInterval1Start, timeInterval1End, 1);
		// this is the time when the downstream link (5_8) is full and also no vehicles are leaving this link such that no vehicles can enter and the node stays blocked
		int timeInterval2Start = 210; int timeInterval2End = 300;
		Map<Id<Link>, Double> avgThroughputCongestedNodeBlocked = throughputAnalyzer.calculateAvgThroughputPerTimeStepOfTimeInterval(timeInterval2Start, timeInterval2End, 1);
		// this is the time when one vehicle per second is allowed to enter 5_8, i.e. 2_5 is restricted to half of its outflow capacity, i.e. 4_5 gets also restricted to half of it's capacity
		int timeInterval3Start = 310; int timeInterval3End = 500;
		Map<Id<Link>, Double> avgThroughputCongestedRestrictFlow = throughputAnalyzer.calculateAvgThroughputPerTimeStepOfTimeInterval(timeInterval3Start, timeInterval3End, 1);
		for (Id<Link> link : linksOfInterest) {
			System.out.println("avgThroughput link " + link + " time interval [" + timeInterval1Start + ", " + timeInterval1End + "]: " + avgThroughputFreeFlow.get(link)
					+ "\t; time interval [" + timeInterval2Start + ", " + timeInterval2End + "]: " + avgThroughputCongestedNodeBlocked.get(link)
					+ "\t; time interval [" + timeInterval3Start + ", " + timeInterval3End + "]: " + avgThroughputCongestedRestrictFlow.get(link));
		}

		/* since this node dynamic does not rely on a random distribution but updates link priorities each time step,
		 * i.e. remembers decisions made in previous time steps,
		 * the difference to the expected values should be much smaller compared to the other node transition logic.
		 */
		double delta = MatsimTestUtils.EPSILON;

		// test throughput for the first time interval
		/* the downstream link is not full, i.e. the links can send vehicles with their full outflow capacity.
		 * the first vehicles reach the end of link 5_8 around sec 300, i.e. throughput of link 5_8 should be zero here.
		 */
		Assertions.assertEquals(2, avgThroughputFreeFlow.get(Id.createLinkId("2_5")), delta, "Troughput on link 2_5 is wrong");
		Assertions.assertEquals(1, avgThroughputFreeFlow.get(Id.createLinkId("4_5")), delta, "Troughput on link 4_5 is wrong");
		Assertions.assertEquals(0, avgThroughputFreeFlow.get(Id.createLinkId("5_8")), delta, "Troughput on link 5_8 is wrong");

		// test throughput for the second time interval
		/* the deterministic node transition should reduce the outflow of all links by the same percentage with which the outflow of links has to be reduced that lead to congested links.
		 * in this case link 2_5 needs to be reduced by 100%, i.e. link 4_5 is also reduced by 100%, i.e. no link is allowed to send vehicles.
		 * this works because the link with downstream congestion has and keeps the minimal priority (i.e. is always selected first) as soon as the move node step is stopped because of downstream congestion.
		 * the first vehicles reach the end of link 5_8 around sec 300, i.e. throughput of link 5_8 should be zero here.
		 */
		Assertions.assertEquals(0, avgThroughputCongestedNodeBlocked.get(Id.createLinkId("2_5")), delta, "Troughput on link 2_5 is wrong");
		Assertions.assertEquals(0, avgThroughputCongestedNodeBlocked.get(Id.createLinkId("4_5")), delta, "Troughput on link 4_5 is wrong");
		Assertions.assertEquals(0, avgThroughputCongestedNodeBlocked.get(Id.createLinkId("5_8")), delta, "Troughput on link 5_8 is wrong");

		// test throughput for the third time interval
		/* the deterministic node transition should reduce the outflow of all links by the same percentage with which the outflow of links has to be reduced that lead to congested links.
		 * in this case link 2_5 needs to be reduced by 1/2, i.e. link 4_5 is also reduced by 1/2.
		 * the first vehicles reach the end of link 5_8 around sec 300, i.e. throughput of link 5_8 should be equal to the flow
		 * capacity of link 5_8 here, i.e. 1 veh per time step (=sec).
		 */
		Assertions.assertEquals(1./2 * 2, avgThroughputCongestedRestrictFlow.get(Id.createLinkId("2_5")), delta, "Troughput on link 2_5 is wrong");
		Assertions.assertEquals(1./2 * 1, avgThroughputCongestedRestrictFlow.get(Id.createLinkId("4_5")), delta, "Troughput on link 4_5 is wrong");
		Assertions.assertEquals(1, avgThroughputCongestedRestrictFlow.get(Id.createLinkId("5_8")), delta, "Troughput on link 5_8 is wrong");
	}

	/**
	 * Test single intersection scenario with old emptyBufferAfterBuffer node transition and blockNode=false. Use time bin size 0.5 seconds.
	 * With that the tests validates the following things:
	 * 1. correct throughput for a time bin size smaller than 1 (see e.g. former bug in QueueWithBuffer removeFirstVehicle);
	 * 2. correct storage capacity bounds for a time bin size smaller than 1 (see e.g. former bug in QueueWithBuffer calculateStorageCapacity);
	 * 3. both streams are independently (because blockNode=false).
	 */
	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testNodeTransitionWithTimeStepSizeSmallerOne(boolean useFastCapUpdate) {
		Scenario scenario = Fixture.createBlockedNodeScenario();
		scenario.getConfig().qsim().setNodeTransitionLogic(NodeTransition.emptyBufferAfterBufferRandomDistribution_dontBlockNode);
		scenario.getConfig().qsim().setTimeStepSize(0.5);
		scenario.getConfig().qsim().setUsingFastCapacityUpdate(useFastCapUpdate);
		scenario.getConfig().routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);

		EventsManager events = EventsUtils.createEventsManager();
		List<Id<Link>> linksOfInterest = new LinkedList<>();
		linksOfInterest.add(Id.createLinkId("2_5"));
		linksOfInterest.add(Id.createLinkId("4_5"));
		linksOfInterest.add(Id.createLinkId("5_8"));
		ThroughputAnalyzer throughputAnalyzer = new ThroughputAnalyzer(linksOfInterest);
		events.addHandler(throughputAnalyzer);

		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();

		new QSimBuilder(scenario.getConfig())
			.useDefaults()
			.build(scenario, events)
			.run();

//		// output for debugging
//		for (int start = 0; start <= 800; start+=10) {
//			Map<Id<Link>, Double> avgThroughputThisInterval = throughputAnalyzer.calculateAvgThroughputPerTimeStepOfTimeInterval(start, start+10, 0.5);
//			System.out.println("[" + start + ", " + (start+10) + "]. link 2_5: " + avgThroughputThisInterval.get(Id.createLinkId("2_5"))
//					+ ", link 4_5: " + avgThroughputThisInterval.get(Id.createLinkId("4_5"))
//					+ ", link 5_8: " + avgThroughputThisInterval.get(Id.createLinkId("5_8")));
//		}
//		Map<Id<Link>, Double> avgThroughputUntil210 = throughputAnalyzer.calculateAvgThroughputPerTimeStepOfTimeInterval(0, 210, 0.5);
//		System.out.println("absolute throughput [" + 0 + ", " + 210 + "]. link 2_5: " + (avgThroughputUntil210.get(Id.createLinkId("2_5")) * 210 / 0.5)
//				+ ", link 4_5: " + (avgThroughputUntil210.get(Id.createLinkId("4_5")) * 210 / 0.5) );

		// this is the time before the downstream link (5_8) gets full and with that no inflow capacity is valid and no node is blocked
		int timeInterval1Start = 110; int timeInterval1End = 200;
		Map<Id<Link>, Double> avgThroughputFreeFlow = throughputAnalyzer.calculateAvgThroughputPerTimeStepOfTimeInterval(timeInterval1Start, timeInterval1End, 0.5);
		// this is the time when the downstream link (5_8) is full and also no vehicles are leaving this link such that no vehicles can enter and the node stays blocked
		int timeInterval2Start = 210; int timeInterval2End = 300;
		Map<Id<Link>, Double> avgThroughputCongestedNodeBlocked = throughputAnalyzer.calculateAvgThroughputPerTimeStepOfTimeInterval(timeInterval2Start, timeInterval2End, 0.5);
		// this is the time when one vehicle per second is allowed to enter 5_8, i.e. 2_5 is restricted to half of its outflow capacity
		int timeInterval3Start = 310; int timeInterval3End = 500;
		Map<Id<Link>, Double> avgThroughputCongestedRestrictFlow = throughputAnalyzer.calculateAvgThroughputPerTimeStepOfTimeInterval(timeInterval3Start, timeInterval3End, 0.5);
		for (Id<Link> link : linksOfInterest) {
			System.out.println("avgThroughput link " + link + " time interval [" + timeInterval1Start + ", " + timeInterval1End + "]: " + avgThroughputFreeFlow.get(link)
					+ "\t; time interval [" + timeInterval2Start + ", " + timeInterval2End + "]: " + avgThroughputCongestedNodeBlocked.get(link)
					+ "\t; time interval [" + timeInterval3Start + ", " + timeInterval3End + "]: " + avgThroughputCongestedRestrictFlow.get(link));
		}

		/* since this node dynamic does not rely on a random distribution but updates link priorities each time step,
		 * i.e. remembers decisions made in previous time steps,
		 * the difference to the expected values should be much smaller compared to the other node transition logic.
		 */
		double delta = MatsimTestUtils.EPSILON;

		// test throughput for the first time interval
		/* the downstream link is not full, i.e. the links can send vehicles with their full outflow capacity.
		 * the first vehicles reach the end of link 5_8 around sec 300, i.e. throughput of link 5_8 should be zero here.
		 */
		Assertions.assertEquals(1, avgThroughputFreeFlow.get(Id.createLinkId("2_5")), delta, "Troughput on link 2_5 is wrong");
		Assertions.assertEquals(0.5, avgThroughputFreeFlow.get(Id.createLinkId("4_5")), delta, "Troughput on link 4_5 is wrong");
		Assertions.assertEquals(0.0, avgThroughputFreeFlow.get(Id.createLinkId("5_8")), delta, "Troughput on link 5_8 is wrong");

		// test throughput for the second time interval
		/* the downstream link of 2_5 is full, i.e. no vehicles can leave 2_5 in this time interval.
		 * link 4_5 is not affected by this, because blockNodeWhenSingleOutlinkFull is set to false.
		 * the first vehicles reach the end of link 5_8 around sec 300, i.e. throughput of link 5_8 should be zero here.
		 */
		Assertions.assertEquals(0, avgThroughputCongestedNodeBlocked.get(Id.createLinkId("2_5")), delta, "Troughput on link 2_5 is wrong");
		Assertions.assertEquals(0.5, avgThroughputCongestedNodeBlocked.get(Id.createLinkId("4_5")), delta, "Troughput on link 4_5 is wrong");
		Assertions.assertEquals(0.0, avgThroughputCongestedNodeBlocked.get(Id.createLinkId("5_8")), delta, "Troughput on link 5_8 is wrong");

		// test throughput for the third time interval
		/* the downstream link of 2_5 lets 1 veh in every two time steps, i.e. throughput of link 2_5 should be 0.5 per time step.
		 * link 4_5 should still be not affected by this and send vehicles with its outflow capacity, i.e 0.5 per time step.
		 * the first vehicles reach the end of link 5_8 around sec 300, i.e. throughput of link 5_8 should be equal to the flow
		 * capacity of link 5_8 here, i.e. 0.5 veh per time step.
		 */
		Assertions.assertEquals(0.5, avgThroughputCongestedRestrictFlow.get(Id.createLinkId("2_5")), delta, "Troughput on link 2_5 is wrong");
		Assertions.assertEquals(0.5, avgThroughputCongestedRestrictFlow.get(Id.createLinkId("4_5")), delta, "Troughput on link 4_5 is wrong");
		Assertions.assertEquals(0.5, avgThroughputCongestedRestrictFlow.get(Id.createLinkId("5_8")), delta, "Troughput on link 5_8 is wrong");
	}

	private static final class Fixture {

		static Scenario createMergeScenario() {
			MatsimRandom.reset();
			Config config = ConfigUtils.createConfig();
			config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			config.controller().setLastIteration(0);
			config.qsim().setStuckTime(24*3600);
			config.qsim().setRemoveStuckVehicles(false);
			config.routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);
			ScoringConfigGroup.ActivityParams dummyAct = new ScoringConfigGroup.ActivityParams("dummy");
	        dummyAct.setTypicalDuration(12 * 3600);
	        config.scoring().addActivityParams(dummyAct);
			config.routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);

			Scenario scenario = ScenarioUtils.createScenario(config);

			/* build network */
			Network network = scenario.getNetwork();
			Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord(0, 1000));
			Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord(1000, 500));
			Node node3 = NetworkUtils.createAndAddNode(network, Id.create("3", Node.class), new Coord(0, 0));
			Node node4 = NetworkUtils.createAndAddNode(network, Id.create("4", Node.class), new Coord(1000, 0));
			Node node5 = NetworkUtils.createAndAddNode(network, Id.create("5", Node.class), new Coord(0, -1000));
			Node node6 = NetworkUtils.createAndAddNode(network, Id.create("6", Node.class), new Coord(1000, -500));
			Node node7 = NetworkUtils.createAndAddNode(network, Id.create("7", Node.class), new Coord(2000, 0));
			Node node8 = NetworkUtils.createAndAddNode(network, Id.create("8", Node.class), new Coord(3000, 0));
			Node node9 = NetworkUtils.createAndAddNode(network, Id.create("9", Node.class), new Coord(4000, 0));
			Link link12 = NetworkUtils.createAndAddLink(network,Id.create("1_2", Link.class), node1, node2, 1000, 10, 3600, 1 );
			Link link27 = NetworkUtils.createAndAddLink(network,Id.create("2_7", Link.class), node2, node7, 1000, 10, 3600, 1 );
			Link link34 = NetworkUtils.createAndAddLink(network,Id.create("3_4", Link.class), node3, node4, 1000, 10, 7200, 2 );
			Link link47 = NetworkUtils.createAndAddLink(network,Id.create("4_7", Link.class), node4, node7, 1000, 10, 7200, 2 );
			Link link56 = NetworkUtils.createAndAddLink(network,Id.create("5_6", Link.class), node5, node6, 1000, 10, 7200, 2 );
			Link link67 = NetworkUtils.createAndAddLink(network,Id.create("6_7", Link.class), node6, node7, 1000, 10, 7200, 2 );
			Link link78 = NetworkUtils.createAndAddLink(network,Id.create("7_8", Link.class), node7, node8, 1000, 10, 7200, 2 );
			Link link89 = NetworkUtils.createAndAddLink(network,Id.create("8_9", Link.class), node8, node9, 1000, 10, 7200, 2 );

			/* build plans */
			fillPopulationWithOneCommodity(scenario.getPopulation(), 1, 500, 0, link12.getId(), link89.getId());
			fillPopulationWithOneCommodity(scenario.getPopulation(), 2, 500, 0, link34.getId(), link89.getId());
			fillPopulationWithOneCommodity(scenario.getPopulation(), 2, 250, 250, link56.getId(), link89.getId());

			return scenario;
		}

		static Scenario createBlockedNodeScenario() {
			MatsimRandom.reset();
			Config config = ConfigUtils.createConfig();
			config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			config.controller().setLastIteration(0);
			config.qsim().setStuckTime(24*3600);
			config.qsim().setRemoveStuckVehicles(false);
			ScoringConfigGroup.ActivityParams dummyAct = new ScoringConfigGroup.ActivityParams("dummy");
	        dummyAct.setTypicalDuration(12 * 3600);
	        config.scoring().addActivityParams(dummyAct);

			Scenario scenario = ScenarioUtils.createScenario(config);

			/* build network */
			Network network = scenario.getNetwork();
			Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord(0, 2000));
			Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord(1000, 2000));
			Node node3 = NetworkUtils.createAndAddNode(network, Id.create("3", Node.class), new Coord(2000, 4000));
			Node node4 = NetworkUtils.createAndAddNode(network, Id.create("4", Node.class), new Coord(2000, 3000));
			Node node5 = NetworkUtils.createAndAddNode(network, Id.create("5", Node.class), new Coord(2000, 2000));
			Node node6 = NetworkUtils.createAndAddNode(network, Id.create("6", Node.class), new Coord(2000, 1000));
			Node node7 = NetworkUtils.createAndAddNode(network, Id.create("7", Node.class), new Coord(2000, 0));
			Node node8 = NetworkUtils.createAndAddNode(network, Id.create("8", Node.class), new Coord(3000, 2000));
			Node node9 = NetworkUtils.createAndAddNode(network, Id.create("9", Node.class), new Coord(4000, 2000));
			Link link12 = NetworkUtils.createAndAddLink(network,Id.create("1_2", Link.class), node1, node2, 1000, 10, 7200, 2 );
			Link link25 = NetworkUtils.createAndAddLink(network,Id.create("2_5", Link.class), node2, node5, 1000, 10, 7200, 2 );
			Link link34 = NetworkUtils.createAndAddLink(network,Id.create("3_4", Link.class), node3, node4, 1000, 10, 3600, 1 );
			Link link45 = NetworkUtils.createAndAddLink(network,Id.create("4_5", Link.class), node4, node5, 1000, 10, 3600, 1 );
			Link link56 = NetworkUtils.createAndAddLink(network,Id.create("5_6", Link.class), node5, node6, 1000, 10, 3600, 1 );
			Link link67 = NetworkUtils.createAndAddLink(network,Id.create("6_7", Link.class), node6, node7, 1000, 10, 3600, 1 );
			Link link58 = NetworkUtils.createAndAddLink(network,Id.create("5_8", Link.class), node5, node8, 1000, 5, 3600, 1 );
			Link link89 = NetworkUtils.createAndAddLink(network,Id.create("8_9", Link.class), node8, node9, 1000, 10, 7200, 2 );

			/* build plans */
			fillPopulationWithOneCommodity(scenario.getPopulation(), 2, 500, 0, link12.getId(), link89.getId());
			fillPopulationWithOneCommodity(scenario.getPopulation(), 1, 500, 0, link34.getId(), link67.getId());

			return scenario;
		}

		private static void fillPopulationWithOneCommodity(Population population, double agentsPerSec, double simulationPeriod, double startTime,
				Id<Link> sourceLink, Id<Link> sinkLink) {

			for (int i=0; i< agentsPerSec * simulationPeriod; i++) {
				// create a person
	            Person person = population.getFactory().createPerson(Id.createPersonId("agent-" + sourceLink + "-" + sinkLink + "-" + i));
	            population.addPerson(person);

	            // create a plan for the person that contains all this information
	            Plan plan = population.getFactory().createPlan();
	            person.addPlan(plan);

	            // create a start activity at the from link
	            Activity startAct = population.getFactory().createActivityFromLinkId("dummy", sourceLink);
	            startAct.setEndTime(startTime + (double)(i)/agentsPerSec);
	            plan.addActivity(startAct);

	            // create a dummy leg
	            plan.addLeg(population.getFactory().createLeg(TransportMode.car));

	            // create a drain activity at the to link
	            Activity drainAct = population.getFactory().createActivityFromLinkId("dummy", sinkLink);
	            plan.addActivity(drainAct);
			}
		}

	}

	private static final class ThroughputAnalyzer implements LinkLeaveEventHandler {

		private final List<Id<Link>> linksOfInterest;
		private Map<Id<Link>, Map<Double, Double>> absoluteThroughputPerTimeStep_veh = new HashMap<>();

		public ThroughputAnalyzer(List<Id<Link>> linksOfInterest) {
			this.linksOfInterest = linksOfInterest;
			for (Id<Link> link : linksOfInterest) {
				absoluteThroughputPerTimeStep_veh.put(link, new TreeMap<>());
			}
		}

		@Override
		public void reset(int iteration) {
			for (Id<Link> link : linksOfInterest) {
				absoluteThroughputPerTimeStep_veh.put(link, new TreeMap<>());
			}
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			for (Id<Link> link : linksOfInterest) {
				if (event.getLinkId().equals(link)) {
					Map<Double, Double> throughputMapThisLink = absoluteThroughputPerTimeStep_veh.get(link);
					if (!throughputMapThisLink.containsKey(event.getTime()))
						throughputMapThisLink.put(event.getTime(), 0.);

					throughputMapThisLink.put(event.getTime(), throughputMapThisLink.get(event.getTime()) + 1);
				}
			}
		}

		public Map<Id<Link>, Double> calculateAvgThroughputPerTimeStepOfTimeInterval(int startTime, int endTime, double timeStepSize) {
			Map<Id<Link>, Double> avgThroughputPerLink = new HashMap<>();
			for (Id<Link> link : linksOfInterest) {
				avgThroughputPerLink.put(link, 0.);
			}

			// sum up link leave events of this time interval
			for (double time = startTime; time < endTime; time += timeStepSize) {
				for (Id<Link> link : linksOfInterest) {
					Map<Double, Double> absThroughputThisLink = absoluteThroughputPerTimeStep_veh.get(link);
					if (absThroughputThisLink.containsKey(time))
						avgThroughputPerLink.put(link, avgThroughputPerLink.get(link) + absThroughputThisLink.get(time));
				}
			}
			// calculate average (divide by the number of time steps)
			for (Id<Link> link : linksOfInterest) {
				avgThroughputPerLink.put(link, avgThroughputPerLink.get(link) * timeStepSize / (endTime - startTime));
			}

			return avgThroughputPerLink;
		}

	}

}
